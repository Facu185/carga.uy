import { useState, type FormEvent } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/contexto'
import { INGRESO_GUBUY } from '../../auth/gubuy'
import { PantallaAcceso } from '../../componentes/PantallaAcceso'
import { useTituloPagina } from '../../titulo'

export function Login() {
  useTituloPagina('Ingresar', true)
  const { sesion, login } = useAuth()
  const navegar = useNavigate()
  const ubicacion = useLocation()
  const estado = ubicacion.state as { desde?: string; vencida?: boolean } | null
  const destino = estado?.desde ?? '/backoffice'
  const vencida = estado?.vencida === true

  const [usuario, setUsuario] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (sesion) {
    return <Navigate to={sesion.debeCambiarPassword ? '/backoffice/cambiar-password' : destino} replace />
  }

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      await login(usuario, password)
      // Si la contraseña es temporal, el componente redirige a cambiarla (ver arriba)
      navegar(destino, { replace: true })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <PantallaAcceso lado="backoffice" titulo="Ingresar al backoffice" descripcion="Para autoridades y administradores del MTOP.">
      {vencida && (
        <p className="aviso-sesion" role="status">
          Tu sesión se cerró: venció o cambió tu acceso. Ingresá de nuevo para seguir donde estabas.
        </p>
      )}
      <form onSubmit={(e) => void enviar(e)} className="formulario-panel">
        <label className="campo">
          <span>Usuario</span>
          <input
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            autoComplete="username"
            autoCapitalize="none"
            spellCheck={false}
            required
            autoFocus
          />
        </label>
        <label className="campo">
          <span>Contraseña</span>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            autoComplete="current-password"
            required
          />
        </label>
        {error && (
          <p className="mensaje-error" role="alert">
            {error}
          </p>
        )}
        <button type="submit" className="boton-bloque" disabled={enviando}>
          {enviando ? 'Ingresando…' : 'Ingresar'}
        </button>
      </form>
      {/* No hay recuperación por correo: un administrador genera una contraseña temporal (Usuarios → Nueva contraseña) */}
      <details className="olvido">
        <summary>¿Olvidaste tu contraseña?</summary>
        <p>
          Pedile a un administrador del backoffice que te genere una contraseña temporal. Al ingresar con ella, el
          sistema te va a pedir que elijas una nueva.
        </p>
      </details>
      <p className="panel-nota">
        ¿Sos de una empresa o funcionario del MTOP? <a href={INGRESO_GUBUY}>Ingresá con gub.uy</a>
      </p>
    </PantallaAcceso>
  )
}
