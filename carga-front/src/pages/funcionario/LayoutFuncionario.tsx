import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { PieSitio } from '../../componentes/PieSitio'
import { ProveedorAvisos } from '../../componentes/ProveedorAvisos'
import { ProveedorConfirmaciones } from '../../componentes/ProveedorConfirmaciones'

/** Área operativa P2. No sustituye la administración de accesos de funcionarios P1. */
export function LayoutFuncionario() {
  const { sesion, logout } = useAuth()
  const { pathname } = useLocation()
  return (
    <ProveedorAvisos key={`${sesion?.token}:${pathname}`}>
      <ProveedorConfirmaciones>
        <div className="pagina">
          <EncabezadoSitio>
            <span className="sitio-usuario">{sesion?.nombre} · Funcionario</span>
            <button type="button" className="boton-secundario boton-chico" onClick={logout}>Salir</button>
          </EncabezadoSitio>
          <main className="pagina-contenido empresa-contenido">
            <nav className="empresa-nav" aria-label="Área del funcionario">
              <NavLink to="/mi-cuenta">Mi cuenta</NavLink>
              <NavLink to="/funcionario/empresas">Empresas</NavLink>
              <NavLink to="/funcionario/solicitudes-responsables">Solicitudes de responsables</NavLink>
            </nav>
            <Outlet />
          </main>
          <PieSitio />
        </div>
      </ProveedorConfirmaciones>
    </ProveedorAvisos>
  )
}
