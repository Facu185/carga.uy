import { Link } from 'react-router-dom'
import { EncabezadoSitio } from '../componentes/EncabezadoSitio'
import { PieSitio } from '../componentes/PieSitio'
import { useTituloPagina } from '../titulo'

/** Un tramo de ruta que termina en una barrera de obra: la dirección no lleva a ningún lado. */
function FinDelCamino() {
  return (
    <svg className="fin-camino" viewBox="0 0 640 170" aria-hidden="true" focusable="false">
      <path d="M -20 120 C 140 120, 260 96, 430 100" className="ruta-banquina" />
      <path d="M -20 120 C 140 120, 260 96, 430 100" className="ruta-asfalto" />
      <path d="M -20 120 C 140 120, 260 96, 430 100" className="ruta-linea" />
      {/* Barrera: dos tablas a rayas sobre patas */}
      <rect x="436" y="118" width="6" height="42" fill="#26323a" />
      <rect x="560" y="118" width="6" height="42" fill="#26323a" />
      <defs>
        <pattern id="rayas" width="28" height="28" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
          <rect width="14" height="28" fill="#b42318" />
          <rect x="14" width="14" height="28" fill="#fff" />
        </pattern>
      </defs>
      <rect x="424" y="84" width="154" height="18" rx="2" fill="url(#rayas)" stroke="#26323a" strokeWidth="2" />
      <rect x="424" y="110" width="154" height="18" rx="2" fill="url(#rayas)" stroke="#26323a" strokeWidth="2" />
      {/* Cartel */}
      <rect x="490" y="30" width="6" height="54" fill="#26323a" />
      <rect x="440" y="10" width="106" height="30" rx="4" fill="#f2b705" stroke="#26323a" strokeWidth="2" />
      <text x="493" y="31" textAnchor="middle" className="fin-camino-cartel">
        Fin del camino
      </text>
    </svg>
  )
}

export function NoEncontrado() {
  useTituloPagina('Página no encontrada')
  return (
    <div className="pagina">
      <EncabezadoSitio />
      <main className="no-encontrado">
        <FinDelCamino />
        <h1>Esta dirección no lleva a ningún lado</h1>
        <p>
          La página que buscás no existe o cambió de lugar. Revisá la dirección, o seguí desde la portada.
        </p>
        <div className="no-encontrado-acciones">
          <Link className="boton-acceso" to="/">
            Ir a la portada
          </Link>
          <Link to="/backoffice">Ir al backoffice</Link>
        </div>
      </main>
      <PieSitio />
    </div>
  )
}
