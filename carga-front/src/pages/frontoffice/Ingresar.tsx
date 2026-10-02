import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/contexto'
import { INGRESO_GUBUY } from '../../auth/gubuy'
import { PantallaAcceso } from '../../componentes/PantallaAcceso'
import { useTituloPagina } from '../../titulo'

/** Aparece cuando alguien intenta entrar a una sección del frontoffice sin sesión. */
export function Ingresar() {
  useTituloPagina('Ingresar')
  const { sesion } = useAuth()
  if (sesion) {
    return <Navigate to="/mi-cuenta" replace />
  }

  return (
    <PantallaAcceso
      titulo="Ingresar con gub.uy"
      descripcion="Para empresas transportistas, choferes y funcionarios del MTOP. Si es la primera vez, tu cuenta se crea al ingresar."
    >
      <a className="boton-enlace boton-bloque" href={INGRESO_GUBUY}>
        Ingresar con gub.uy
      </a>
      <p className="panel-nota">
        ¿Sos autoridad o administrador? <Link to="/backoffice/login">Ingresá al backoffice</Link>
      </p>
    </PantallaAcceso>
  )
}
