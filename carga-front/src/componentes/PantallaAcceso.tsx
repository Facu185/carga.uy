import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

type Lado = 'ciudadanos' | 'backoffice'

/** Lo que dice la franja de la marca según a quién está dirigida la pantalla. */
const MARCA: Record<Lado, { titulo: string; texto: string }> = {
  ciudadanos: {
    titulo: 'Transporte de carga en Uruguay, de la guía al peaje',
    texto: 'Empresas, choferes y funcionarios del MTOP ingresan con su usuario gub.uy.',
  },
  backoffice: {
    titulo: 'Backoffice del MTOP',
    texto: 'Usuarios, funcionarios y la pista de auditoría de carga.uy.',
  },
}

/** Una ruta que baja por la franja y termina en el mojón del kilómetro cero. Decorativa. */
function RutaVertical() {
  const trazado = 'M 330 -40 C 330 200, 110 250, 150 420 S 340 640, 250 860'
  return (
    <svg className="acceso-ruta" viewBox="0 0 400 800" preserveAspectRatio="xMidYMid slice" aria-hidden="true" focusable="false">
      <path d={trazado} className="acceso-ruta-banquina" />
      <path d={trazado} className="acceso-ruta-asfalto" />
      <path d={trazado} className="acceso-ruta-linea" />
      <g transform="translate(92 640)">
        <rect width="44" height="54" rx="12" ry="12" fill="#fff" />
        <rect y="40" width="44" height="14" fill="#fff" />
        <text x="22" y="20" textAnchor="middle" className="acceso-mojon-km">
          km
        </text>
        <text x="22" y="40" textAnchor="middle" className="acceso-mojon-numero">
          0
        </text>
      </g>
    </svg>
  )
}

/**
 * Pantallas cortas (ingresos, errores y avisos) en dos mitades: a un lado la franja de la marca
 * con la ruta, al otro el contenido. En el celular la franja se reduce a una banda arriba.
 */
export function PantallaAcceso({
  titulo,
  descripcion,
  children,
  lado = 'ciudadanos',
}: {
  titulo: string
  descripcion?: ReactNode
  children?: ReactNode
  lado?: Lado
}) {
  const marca = MARCA[lado]
  return (
    <div className="acceso-dividido">
      <aside className="acceso-marca">
        <RutaVertical />
        <Link to="/" className="acceso-marca-logo">
          carga.uy
        </Link>
        <div className="acceso-marca-texto">
          <p className="acceso-marca-titulo">{marca.titulo}</p>
          <p className="acceso-marca-detalle">{marca.texto}</p>
        </div>
        <p className="acceso-marca-pie">
          Ministerio de Transporte y Obras Públicas. Prototipo académico del Taller de Sistemas Empresariales 2026.
        </p>
      </aside>
      <main className="acceso-contenido">
        <Link to="/" className="acceso-volver">
          Volver a la portada
        </Link>
        <section className="panel">
          <h1>{titulo}</h1>
          {descripcion && <p className="panel-descripcion">{descripcion}</p>}
          {children}
        </section>
      </main>
    </div>
  )
}
