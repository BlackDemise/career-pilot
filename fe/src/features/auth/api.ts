import apiClient, { unwrap } from '../../shared/api/axiosClient'
import type { ApiResponse } from '../../shared/api/types'

type Credentials = { email: string; password: string }
type RegisterRequest = Credentials & { firstName: string; lastName: string }
type ResetPasswordRequest = { token: string; password: string; confirmPassword: string }

export function login(request: Credentials): Promise<string> {
  return unwrap<string>(apiClient.post<ApiResponse<string>>('/auth/login', request))
}

export function register(request: RegisterRequest): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/register', request))
}

export function verifyRegistration(token: string): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/register/verify', { token }))
}

export function resendRegistration(email: string): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/register/resend', { email }))
}

export function requestPasswordReset(email: string): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/forgot-password', { email }))
}

export function resendPasswordReset(email: string): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/forgot-password/resend', { email }))
}

export function resetPassword(request: ResetPasswordRequest): Promise<null> {
  return unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/reset-password', request))
}

export async function logout(): Promise<void> {
  await unwrap<null>(apiClient.post<ApiResponse<null>>('/auth/logout'))
}
