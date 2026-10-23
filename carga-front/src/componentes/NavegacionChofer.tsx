import { NavLink } from 'react-router-dom'

export function NavegacionChofer() {
  return (
    <nav className="empresa-nav" aria-label="Área del chofer">
      <NavLink to="/mi-cuenta" end>Mi cuenta</NavLink>
      <NavLink to="/mis-guias" end>Mis guías</NavLink>
    </nav>
  )
}
