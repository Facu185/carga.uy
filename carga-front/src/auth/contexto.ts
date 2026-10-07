import { createContext, useContext } from 'react'
import type { Sesion } from '../tipos'

export interface ValorAuth {
  sesion: Sesion | null
  /** Login interno del backoffice (usuario y contraseña). */
  login: (usuario: string, password: string) => Promise<void>
  /** Sesión que llega del ingreso con gub.uy. */
  establecerSesion: (sesion: Sesion) => void
  logout: () => void
}

export const AuthContext = createContext<ValorAuth | null>(null)

export function useAuth(): ValorAuth {
  const valor = useContext(AuthContext)
  if (!valor) {
    throw new Error('useAuth se usa dentro de <AuthProvider>')
  }
  return valor
}
