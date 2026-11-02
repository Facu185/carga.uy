import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { PieSitio } from '../../componentes/PieSitio'

/** Marco de las páginas que se usan sin ingresar: encabezado del sitio, navegación pública y pie. */
export function PaginaPublica({ children }: { children: ReactNode }) {
  return (
    <div className="pagina">
      <EncabezadoSitio>
        <nav className="publico-navegacion" aria-label="Información pública">
          <Link to="/validar-permiso">Validar un permiso</Link>
          <Link to="/estadisticas">Cifras del transporte</Link>
        </nav>
      </EncabezadoSitio>
      <main className="pagina-contenido publico-contenido">{children}</main>
      <PieSitio />
    </div>
  )
}
