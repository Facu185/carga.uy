import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, alExpirarLaSesion } from '../api/cliente'
import type { Sesion } from '../tipos'
import { borrarSesion, guardarSesion, leerSesionGuardada, olvidarSalida, registrarSalida } from './almacenamiento'
import { AuthContext } from './contexto'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [sesion, setSesion] = useState<Sesion | null>(leerSesionGuardada)

  const logout = useCallback(() => {
    borrarSesion()
    registrarSalida('manual')
    setSesion(null)
  }, [])

  // Si el central rechaza el token (vencido o revocado), se cierra la sesión en pantalla y se recuerda el motivo
  useEffect(
    () =>
      alExpirarLaSesion(() => {
        registrarSalida('vencida')
        setSesion(null)
      }),
    [],
  )

  // Al llegar la hora de vencimiento, la sesión se cierra en pantalla sin esperar al próximo pedido al central
  useEffect(() => {
    if (!sesion) return
    const faltan = new Date(sesion.expiraEn).getTime() - Date.now()
    const vencer = () => {
      borrarSesion()
      registrarSalida('vencida')
      setSesion(null)
    }
    if (faltan <= 0) {
      vencer()
      return
    }
    // setTimeout admite hasta ~24 días: un token de 30 minutos entra de sobra
    const temporizador = window.setTimeout(vencer, faltan)
    return () => window.clearTimeout(temporizador)
  }, [sesion])

  const establecerSesion = useCallback((nueva: Sesion) => {
    guardarSesion(nueva)
    olvidarSalida()
    setSesion(nueva)
  }, [])

  const login = useCallback(
    async (usuario: string, password: string) => {
      const nueva = await api<Sesion>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ usuario, password }),
      })
      establecerSesion(nueva)
    },
    [establecerSesion],
  )

  const valor = useMemo(
    () => ({ sesion, login, establecerSesion, logout }),
    [sesion, login, establecerSesion, logout],
  )

  return <AuthContext.Provider value={valor}>{children}</AuthContext.Provider>
}
