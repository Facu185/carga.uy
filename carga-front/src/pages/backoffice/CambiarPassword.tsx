import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { PantallaAcceso } from '../../componentes/PantallaAcceso'
import type { Sesion } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const LARGO_MINIMO = 12

/** Cambio de contraseña propio. Si la contraseña es temporal, es obligatorio antes de usar el backoffice. */
export function CambiarPassword() {
  useTituloPagina('Cambiar contraseña', true)
  const { sesion, establecerSesion, logout } = useAuth()
  const navegar = useNavigate()
  const obligatorio = sesion?.debeCambiarPassword === true
  const esLdap = sesion?.origen === 'LDAP'

  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [repetida, setRepetida] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (nueva !== repetida) {
      setError('Las contraseñas nuevas no coinciden')
      return
    }
    setEnviando(true)
    try {
      // El central devuelve una sesión nueva: cambiar la contraseña cierra las anteriores
      const nuevaSesion = await api<Sesion>('/auth/cambiar-password', {
        method: 'POST',
        body: JSON.stringify({ actual, nueva }),
      })
      establecerSesion(nuevaSesion)
      navegar('/backoffice', { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar la contraseña')
    } finally {
      setEnviando(false)
    }
  }

  if (esLdap) {
    return (
      <PantallaAcceso
        lado="backoffice"
        titulo="Cambiar contraseña"
        descripcion="Tu contraseña la gestiona el directorio LDAP del MTOP. Cambiala por los canales del ministerio."
      >
        <p className="panel-nota">
          <Link to="/backoffice">Volver al backoffice</Link>
        </p>
      </PantallaAcceso>
    )
  }

  return (
    <PantallaAcceso
        lado="backoffice"
      titulo="Cambiar contraseña"
      descripcion={
        obligatorio
          ? 'Tu contraseña es temporal. Elegí una nueva para empezar a usar el backoffice.'
          : 'Al cambiarla se cierran tus otras sesiones abiertas.'
      }
    >
      <form onSubmit={(e) => void enviar(e)} className="formulario-panel">
        <label className="campo">
          <span>{obligatorio ? 'Contraseña temporal' : 'Contraseña actual'}</span>
          <input
            type="password"
            value={actual}
            onChange={(e) => setActual(e.target.value)}
            autoComplete="current-password"
            required
            autoFocus
          />
        </label>
        <label className="campo">
          <span>Nueva contraseña</span>
          <input
            type="password"
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
            autoComplete="new-password"
            minLength={LARGO_MINIMO}
            aria-describedby="ayuda-nueva"
            required
          />
          <small id="ayuda-nueva">Mínimo {LARGO_MINIMO} caracteres. Una frase corta es más fácil de recordar.</small>
        </label>
        <label className="campo">
          <span>Repetir la nueva contraseña</span>
          <input
            type="password"
            value={repetida}
            onChange={(e) => setRepetida(e.target.value)}
            autoComplete="new-password"
            required
          />
        </label>
        {error && (
          <p className="mensaje-error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" className="boton-bloque" disabled={enviando}>
          {enviando ? 'Guardando…' : 'Guardar contraseña'}
        </button>
      </form>
      <p className="panel-nota">
        {obligatorio ? (
          <button type="button" className="boton-texto" onClick={logout}>
            Salir sin cambiarla
          </button>
        ) : (
          <Link to="/backoffice">Volver al backoffice</Link>
        )}
      </p>
    </PantallaAcceso>
  )
}
