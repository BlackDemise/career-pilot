import axios, { type AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import { ApiError, type ApiResponse, type ApiValidationErrors } from './types'
import { clearAccessToken, getAccessToken, setAccessToken } from './authSession'

type AuthAwareRequestConfig = AxiosRequestConfig & { _skipAuthRefresh?: boolean }
type AuthAwareInternalConfig = InternalAxiosRequestConfig & { _retry?: boolean; _skipAuthRefresh?: boolean }

const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

let refreshPromise: Promise<string> | null = null

function isApiResponse(value: unknown): value is ApiResponse<unknown> {
  if (!value || typeof value !== 'object') return false
  const response = value as Record<string, unknown>
  return typeof response.statusCode === 'number' && 'result' in response
}

function toApiError(error: AxiosError<ApiResponse<unknown>>): ApiError {
  const response = error.response
  const body = response?.data
  const validationErrors = body?.result && typeof body.result === 'object'
    ? body.result as ApiValidationErrors
    : null
  return new ApiError(body?.message ?? error.message, response?.status ?? 0, validationErrors)
}

async function refreshAccessToken(): Promise<string> {
  const skipAuthRefreshConfig: AuthAwareRequestConfig = { _skipAuthRefresh: true }
  const response = await apiClient.post<ApiResponse<string>>('/auth/refresh', undefined, skipAuthRefreshConfig)
  const token = response.data.result
  if (!token) throw new ApiError('Your session has expired.', 401)
  setAccessToken(token)
  return token
}

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

apiClient.interceptors.response.use(
  (response) => {
    if (!isApiResponse(response.data)) return response
    if (response.data.statusCode >= 400) {
      throw new ApiError(response.data.message, response.data.statusCode)
    }
    return response
  },
  async (error: AxiosError<ApiResponse<unknown>>) => {
    const originalRequest = error.config as AuthAwareInternalConfig | undefined
    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry || originalRequest._skipAuthRefresh) {
      if (error.response?.status === 401 && !originalRequest?._skipAuthRefresh) clearAccessToken()
      return Promise.reject(toApiError(error))
    }

    originalRequest._retry = true
    try {
      refreshPromise ??= refreshAccessToken().finally(() => { refreshPromise = null })
      const token = await refreshPromise
      originalRequest.headers.Authorization = `Bearer ${token}`
      return apiClient(originalRequest)
    } catch (refreshError) {
      clearAccessToken()
      window.location.assign('/login')
      return Promise.reject(refreshError)
    }
  },
)

export async function unwrap<Result>(request: Promise<{ data: ApiResponse<Result> }>): Promise<Result> {
  const response = await request
  return response.data.result as Result
}

export default apiClient
