import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/contexto'
import { ROLES } from '../tipos'

export function NavegacionEmpresa() {
  const { sesion } = useAuth()
  const roles = sesion?.roles ?? (sesion ? [sesion.rol] : [])

  return (
    <nav className="empresa-nav" aria-label="Secciones de la empresa">
      <NavLink to="/mi-cuenta" end>Mi cuenta</NavLink>
      {roles.includes(ROLES.CHOFER) && <NavLink to="/mis-guias" end>Mis guías</NavLink>}
      <NavLink to="/mi-empresa/perfil">Perfil de empresa</NavLink>
      <NavLink to="/mi-empresa" end>Choferes</NavLink>
      <NavLink to="/mi-empresa/vehiculos">Vehículos</NavLink>
      <NavLink to="/mi-empresa/guias">Guías de viajes</NavLink>
      <NavLink to="/mi-empresa/infracciones">Infracciones</NavLink>
      {roles.includes(ROLES.CIUDADANO) && !roles.includes(ROLES.FUNCIONARIO)
        && <NavLink to="/guias/nueva">Registrar guía</NavLink>}
    </nav>
  )
}
