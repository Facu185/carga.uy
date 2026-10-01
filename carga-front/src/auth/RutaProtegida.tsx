import type { ReactNode } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { PantallaAcceso } from '../componentes/PantallaAcceso'
import { nombreRol } from '../formato'
import { ROLES, type Rol } from '../tipos'
import { motivoSalida } from './almacenamiento'
import { useAuth } from './contexto'

/**
 * Solo decide qué pantallas se muestran. NO es el control de acceso:
 * la autorización real la hace el central con @RolesAllowed en los servicios.
 */
export function RutaProtegida({
  roles,
  children,
  login = '/backoffice/login',
}: {
  roles: Rol[]
  children: ReactNode
  /** A dónde mandar si no hay sesión: backoffice o ingreso con gub.uy. */
  login?: string
}) {
  const { sesion, logout } = useAuth()
  const ubicacion = useLocation()
  const navegar = useNavigate()

  if (!sesion) {
    const motivo = motivoSalida()
    // Si tocó Salir, el próximo ingreso puede ser de otra persona: no se recuerda la página anterior
    const estado = motivo === 'manual' ? null : { desde: ubicacion.pathname, vencida: motivo === 'vencida' }
    return <Navigate to={login} replace state={estado} />
  }
  if (!roles.includes(sesion.rol)) {
    // Pasa si hay una sesión abierta de otro tipo (p. ej. ciudadano de gub.uy entrando al backoffice)
    function cambiarDeCuenta() {
      logout()
      navegar(login, { replace: true, state: { desde: ubicacion.pathname } })
    }
    // El inicio que sí puede ver según su rol
    const inicio = sesion.rol === ROLES.ADMINISTRADOR || sesion.rol === ROLES.AUTORIDAD ? '/backoffice' : '/mi-cuenta'
    return (
      <PantallaAcceso
        lado={login === '/backoffice/login' ? 'backoffice' : 'ciudadanos'}
        titulo="Sin acceso a esta sección"
        descripcion={
          <>
            Ingresaste como <strong>{sesion.nombre}</strong> ({nombreRol(sesion.rol).toLowerCase()}), que no tiene
            permiso para esta sección.
          </>
        }
      >
        <div className="acciones-bloque">
          <button type="button" className="boton-bloque" onClick={() => navegar(inicio, { replace: true })}>
            Ir al inicio
          </button>
          <button type="button" className="boton-bloque boton-secundario" onClick={cambiarDeCuenta}>
            Salir e ingresar con otra cuenta
          </button>
        </div>
      </PantallaAcceso>
    )
  }
  return children
}
