import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

/** Marca a la izquierda. A la derecha, el organismo o lo que se pase (p. ej. usuario y "Salir"). */
export function EncabezadoSitio({ children }: { children?: ReactNode }) {
  return (
    <header className="sitio-encabezado">
      <Link to="/" className="marca">
        carga.uy
      </Link>
      <div className="sitio-encabezado-extra">
        {children ?? <span className="sitio-organismo">Ministerio de Transporte y Obras Públicas</span>}
      </div>
    </header>
  )
}
