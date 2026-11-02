import { Building2, ChartColumn, Gavel, House, IdCard, KeyRound, LogOut, RadioTower, ScrollText, Users } from 'lucide-react'
import { Link, Navigate, NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/contexto'
import { PieSitio } from '../../componentes/PieSitio'
import { ProveedorAvisos } from '../../componentes/ProveedorAvisos'
import { ProveedorConfirmaciones } from '../../componentes/ProveedorConfirmaciones'
import { nombreRol } from '../../formato'
import { ROLES } from '../../tipos'

export function LayoutBackoffice() {
  const { sesion, logout } = useAuth()
  const esAdmin = sesion?.rol === ROLES.ADMINISTRADOR
  const puedeRevisarEmpresas = sesion?.rol === ROLES.ADMINISTRADOR || sesion?.rol === ROLES.FUNCIONARIO

  // Con contraseña temporal el central rechaza todo lo demás: primero hay que cambiarla
  if (sesion?.debeCambiarPassword) {
    return <Navigate to="/backoffice/cambiar-password" replace />
  }

  return (
    <ProveedorAvisos>
      <ProveedorConfirmaciones>
      <div className="pagina">
        <header className="bo-encabezado">
          <div className="bo-encabezado-interior">
            <Link to="/backoffice" className="bo-marca">
              carga.uy <span>Backoffice</span>
            </Link>
            <nav className="bo-nav" aria-label="Secciones del backoffice">
              <NavLink to="/backoffice" end>
                <House size={18} aria-hidden="true" />
                Inicio
              </NavLink>
              {sesion?.rol === ROLES.AUTORIDAD && (
                <NavLink to="/backoffice/reportes">
                  <ChartColumn size={18} aria-hidden="true" />
                  Reportes
                </NavLink>
              )}
              {esAdmin && (
                <NavLink to="/backoffice/funcionarios">
                  <IdCard size={18} aria-hidden="true" />
                  Funcionarios
                </NavLink>
              )}
              {puedeRevisarEmpresas && (
                <NavLink to="/backoffice/solicitudes-empresa">
                  <Building2 size={18} aria-hidden="true" />
                  Solicitudes de empresas
                </NavLink>
              )}
              {esAdmin && (
                <NavLink to="/backoffice/usuarios">
                  <Users size={18} aria-hidden="true" />
                  Usuarios
                </NavLink>
              )}
              {esAdmin && (
                <NavLink to="/backoffice/reglas">
                  <Gavel size={18} aria-hidden="true" />
                  Reglas de fiscalización
                </NavLink>
              )}
              {esAdmin && (
                <NavLink to="/backoffice/nodos">
                  <RadioTower size={18} aria-hidden="true" />
                  Nodos
                </NavLink>
              )}
              <NavLink to="/backoffice/auditoria">
                <ScrollText size={18} aria-hidden="true" />
                Auditoría
              </NavLink>
            </nav>
            <div className="bo-usuario">
              <span>
                {sesion?.nombre}
                <small>{nombreRol(sesion?.rol)}</small>
                {/* Con LDAP la contraseña la gestiona el directorio del MTOP, no carga.uy */}
                {sesion?.origen !== 'LDAP' && (
                  <Link to="/backoffice/cambiar-password" className="bo-enlace-cuenta">
                    <KeyRound size={13} aria-hidden="true" />
                    Cambiar contraseña
                  </Link>
                )}
              </span>
              <button type="button" className="boton-chico boton-claro con-icono" onClick={logout}>
                <LogOut size={16} aria-hidden="true" />
                Salir
              </button>
            </div>
          </div>
        </header>
        <main className="bo-contenido">
          <Outlet />
        </main>
        <PieSitio />
      </div>
      </ProveedorConfirmaciones>
    </ProveedorAvisos>
  )
}
