import { Building2, Truck, Users } from 'lucide-react'
import { NavLink } from 'react-router-dom'

export function NavegacionEmpresa() {
  return (
    <nav className="empresa-nav" aria-label="Secciones de la empresa">
      <NavLink to="/mi-cuenta"><Building2 size={18} aria-hidden="true" /> Mi cuenta</NavLink>
      <NavLink to="/mi-empresa/perfil"><Building2 size={18} aria-hidden="true" /> Perfil</NavLink>
      <NavLink to="/mi-empresa" end><Users size={18} aria-hidden="true" /> Choferes</NavLink>
      <NavLink to="/mi-empresa/vehiculos"><Truck size={18} aria-hidden="true" /> Vehículos</NavLink>
    </nav>
  )
}
