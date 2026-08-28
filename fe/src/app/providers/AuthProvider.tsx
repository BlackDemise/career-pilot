import { useState, type PropsWithChildren } from 'react'
import { clearAccessToken, getAccessTokenPayload, isAccessTokenUsable, setAccessToken } from '../../shared/api/authSession'
import { login as loginRequest, logout as logoutRequest } from '../../features/auth/api'
import { AuthContext } from './authContext'

function getSession() {
  const payload = getAccessTokenPayload()
  return {
    isAuthenticated: isAccessTokenUsable(),
    displayName: payload?.firstName && payload.lastName ? `${payload.firstName} ${payload.lastName}` : payload?.email ?? null,
  }
}

export function AuthProvider({ children }: PropsWithChildren) {
  const [session, setSession] = useState(getSession)

  async function login(email: string, password: string) {
    const token = await loginRequest({ email, password })
    setAccessToken(token)
    setSession(getSession())
  }

  async function logout() {
    try {
      await logoutRequest()
    } finally {
      clearAccessToken()
      setSession(getSession())
    }
  }

  return <AuthContext.Provider value={{ ...session, login, logout }}>{children}</AuthContext.Provider>
}
