import type { ReactNode } from 'react'

/** Título de cada sección del backoffice, con una línea que explica para qué sirve y acciones a la derecha. */
export function EncabezadoSeccion({
  titulo,
  descripcion,
  children,
}: {
  titulo: string
  descripcion?: ReactNode
  children?: ReactNode
}) {
  return (
    <div className="seccion-encabezado">
      <div>
        <h1>{titulo}</h1>
        {descripcion && <p>{descripcion}</p>}
      </div>
      {children && <div className="seccion-acciones">{children}</div>}
    </div>
  )
}
