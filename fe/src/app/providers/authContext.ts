import { createContext } from 'react'

export type AuthContextValue = {
  isAuthenticated: boolean
  displayName: string | null
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)
