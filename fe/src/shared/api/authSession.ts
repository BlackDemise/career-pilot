const accessTokenKey = 'careerPilot.accessToken'

type JwtPayload = {
  exp?: number
  sub?: string
  email?: string
  firstName?: string
  lastName?: string
  role?: 'USER' | 'ADMIN'
}

function decodePayload(token: string): JwtPayload | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized)) as JwtPayload
  } catch {
    return null
  }
}

export function getAccessToken(): string | null {
  return localStorage.getItem(accessTokenKey)
}

export function setAccessToken(token: string): void {
  localStorage.setItem(accessTokenKey, token)
}

export function clearAccessToken(): void {
  localStorage.removeItem(accessTokenKey)
}

export function getAccessTokenPayload(token = getAccessToken()): JwtPayload | null {
  return token ? decodePayload(token) : null
}

export function isAccessTokenUsable(token = getAccessToken()): boolean {
  const payload = token ? decodePayload(token) : null
  return Boolean(token && payload?.exp && payload.exp * 1000 > Date.now())
}
