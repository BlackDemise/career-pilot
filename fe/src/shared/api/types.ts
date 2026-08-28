export type ApiResponse<Result> = {
  timestamp: number
  statusCode: number
  message: string
  result: Result | null
}

export type ApiValidationErrors = Record<string, string>

export class ApiError extends Error {
  readonly statusCode: number
  readonly validationErrors: ApiValidationErrors | null

  constructor(message: string, statusCode: number, validationErrors: ApiValidationErrors | null = null) {
    super(message)
    this.name = 'ApiError'
    this.statusCode = statusCode
    this.validationErrors = validationErrors
  }
}
