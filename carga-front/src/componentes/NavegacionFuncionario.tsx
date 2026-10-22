import { NavLink } from 'react-router-dom'

export function NavegacionFuncionario() {
  return (
    <nav className="empresa-nav" aria-label="Área del funcionario">
      <NavLink to="/mi-cuenta" end>Mi cuenta</NavLink>
      <NavLink to="/funcionario/empresas">Empresas</NavLink>
      <NavLink to="/funcionario/solicitudes-responsables">Solicitudes de responsables</NavLink>
    </nav>
  )
}
