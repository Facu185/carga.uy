import { useEffect, useState } from 'react'
import { api } from '../api/cliente'
import { useAuth } from '../auth/contexto'
import type { Sesion } from '../tipos'

/** Cuánto antes del vencimiento aparece el aviso. */
const ANTICIPACION_MS = 2 * 60 * 1000

function restante(expiraEn: string, ahora: number): number {
  return new Date(expiraEn).getTime() - ahora
}

function formatear(ms: number): string {
  const segundos = Math.max(0, Math.ceil(ms / 1000))
  const minutos = Math.floor(segundos / 60)
  return `${minutos}:${String(segundos % 60).padStart(2, '0')}`
}

/**
 * Aviso de que la sesión está por vencer, con la opción de seguir conectado. Evita que alguien pierda
 * lo que estaba cargando cuando el token de 30 minutos vence en medio de un formulario.
 * La renovación la pide siempre la persona: nunca se renueva sola.
 */
export function AvisoSesion() {
  const { sesion, establecerSesion, logout } = useAuth()
  // Reloj que avanza cada segundo; lo que falta para vencer se calcula a partir de él
  const [ahora, setAhora] = useState(() => Date.now())
  const [renovando, setRenovando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!sesion) return
    const intervalo = window.setInterval(() => setAhora(Date.now()), 1000)
    return () => window.clearInterval(intervalo)
  }, [sesion])

  const ms = sesion ? restante(sesion.expiraEn, ahora) : null
  if (!sesion || ms === null || ms > ANTICIPACION_MS || ms <= 0) {
    return null
  }

  async function seguirConectado() {
    setRenovando(true)
    setError(null)
    try {
      establecerSesion(await api<Sesion>('/auth/renovar', { method: 'POST' }))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo renovar la sesión')
    } finally {
      setRenovando(false)
    }
  }

  return (
    <div className="aviso-sesion-vence" role="alert">
      <p>
        <strong>Tu sesión vence en {formatear(ms)}.</strong> Si seguís trabajando, renovala para no perder lo que
        estás haciendo.
      </p>
      {error && <p className="aviso-sesion-error">{error}</p>}
      <div className="acciones">
        <button type="button" className="boton-chico" onClick={() => void seguirConectado()} disabled={renovando}>
          {renovando ? 'Renovando…' : 'Seguir conectado'}
        </button>
        <button type="button" className="boton-chico boton-claro" onClick={logout}>
          Salir
        </button>
      </div>
    </div>
  )
}
