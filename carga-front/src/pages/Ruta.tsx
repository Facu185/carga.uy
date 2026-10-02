import { useCallback, useEffect, useLayoutEffect, useRef, useState, type KeyboardEvent, type PointerEvent } from 'react'

/**
 * "Cómo funciona", recorrido etapa por etapa. La persona elige una etapa (tocando un mojón, con las flechas
 * del teclado o deslizando en el celular) y el camión maneja hasta ahí. Puede ver el viaje en regla o
 * uno con exceso de peso, que termina en un caso de fiscalización.
 * Mientras maneja, el camión deja los puntos de posición que reporta el sistema de tracking.
 * El movimiento responde siempre a una acción de la persona; con "reducir movimiento", el camión salta directo.
 * En pantallas angostas la vista sigue al camión, así cada etapa entra completa en pantalla.
 */

// Ruta extendida a los costados para que no se vea el final en pantallas anchas
const TRAZADO =
  'M -800 150 L -60 150 C 240 150, 380 78, 640 86 S 980 160, 1260 110 C 1540 60, 1700 110, 2000 110'
const ALTO = 220
const ANCHO_COMPLETO = 1200
const DURACION_MS = 1100
const SEPARACION_GPS = 55 // cada cuánto trazado queda un punto de posición
const CARRIL = 9 // el camión va por el carril derecho

type Escenario = 'regla' | 'exceso'
type Tono = 'guia' | 'ok' | 'problema'

interface Resultado {
  titulo: string
  texto: string
  cartel: string
  detalle: string
  tono: Tono
}

interface Etapa {
  km: string
  x: number // dónde está la etapa sobre la ruta (coordenada del dibujo)
  y: number // altura de la ruta en ese punto
  corto: string
  regla: Resultado
  exceso: Resultado
}

const ETAPAS: Etapa[] = [
  {
    km: '0',
    x: 250,
    y: 124,
    corto: 'Sale con la guía',
    regla: {
      titulo: 'La empresa emite la guía',
      texto:
        'Antes de salir declara el vehículo, la carga y el peso. El chofer inicia el viaje desde la app y el vehículo empieza a reportar su posición.',
      cartel: 'Guía emitida',
      detalle: '28 t de granos, SBC 1234',
      tono: 'guia',
    },
    exceso: {
      titulo: 'La empresa emite la guía',
      texto:
        'Antes de salir declara el vehículo, la carga y el peso. El chofer inicia el viaje desde la app y el vehículo empieza a reportar su posición.',
      cartel: 'Guía emitida',
      detalle: '28 t de granos, SBC 1234',
      tono: 'guia',
    },
  },
  {
    km: '32',
    x: 640,
    y: 86,
    corto: 'Pasa por el peaje',
    regla: {
      titulo: 'El peaje registra el peso',
      texto: 'Las balanzas de los peajes pesan el vehículo y el MTOP lo compara con lo declarado en la guía.',
      cartel: 'Pesaje verificado',
      detalle: '27,6 t de 28 t declaradas',
      tono: 'ok',
    },
    exceso: {
      titulo: 'La balanza detecta un exceso',
      texto: 'El vehículo pesa más de lo declarado en la guía. El pesaje queda registrado como evidencia.',
      cartel: 'Exceso de peso',
      detalle: '31,2 t de 28 t declaradas',
      tono: 'problema',
    },
  },
  {
    km: '120',
    x: 1030,
    y: 128,
    corto: 'Llega a destino',
    regla: {
      titulo: 'Al llegar, el viaje se cierra',
      texto: 'El peso y el recorrido coinciden con la guía: el viaje queda cerrado en regla.',
      cartel: 'Viaje cerrado',
      detalle: 'Llegó a Minas en regla',
      tono: 'ok',
    },
    exceso: {
      titulo: 'El MTOP abre un caso',
      texto:
        'Un funcionario abre un caso de fiscalización con el pesaje del peaje como fundamento. Todo queda registrado en la auditoría.',
      cartel: 'Caso de fiscalización',
      detalle: 'Abierto por exceso de peso',
      tono: 'problema',
    },
  },
]

const COLOR_TONO: Record<Tono, string> = { guia: '#0e4c92', ok: '#1e7b4a', problema: '#b42318' }

interface Pose {
  x: number
  y: number
  angulo: number
}

interface PuntoGps {
  largo: number
  x: number
  y: number
}

function prefiereMenosMovimiento(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

/** Largo del trazado en el que el camión queda a la altura x (el trazado avanza siempre hacia la derecha). */
function largoEnX(camino: SVGPathElement, x: number): number {
  let desde = 0
  let hasta = camino.getTotalLength()
  for (let i = 0; i < 40; i++) {
    const medio = (desde + hasta) / 2
    if (camino.getPointAtLength(medio).x < x) desde = medio
    else hasta = medio
  }
  return (desde + hasta) / 2
}

function poseEn(camino: SVGPathElement, largo: number): Pose {
  const p = camino.getPointAtLength(largo)
  const q = camino.getPointAtLength(largo + 1)
  return { x: p.x, y: p.y, angulo: (Math.atan2(q.y - p.y, q.x - p.x) * 180) / Math.PI }
}

/** Punto sobre el carril derecho (desplazado del centro de la ruta hacia la derecha del sentido de marcha). */
function puntoEnCarril(camino: SVGPathElement, largo: number): PuntoGps {
  const { x, y, angulo } = poseEn(camino, largo)
  const rad = (angulo * Math.PI) / 180
  return { largo, x: x - Math.sin(rad) * CARRIL, y: y + Math.cos(rad) * CARRIL }
}

const suavizar = (t: number) => (t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2)

/** Camión visto desde arriba, apuntando hacia +x, en el carril derecho. */
function Camion({ pose }: { pose: Pose }) {
  return (
    <g transform={`translate(${pose.x} ${pose.y}) rotate(${pose.angulo})`}>
      <g transform={`translate(0 ${CARRIL})`}>
        <rect x="-48" y="-10" width="56" height="20" rx="2" fill="#fff" stroke="#b9c2c0" strokeWidth="1.5" />
        <rect x="-44" y="-2" width="48" height="4" fill="#f2b705" />
        <rect x="10" y="-10" width="20" height="20" rx="3" fill="#0e4c92" />
        <rect x="24" y="-8" width="4" height="16" rx="1" fill="#9fc3e8" />
      </g>
    </g>
  )
}

/** Ícono dentro del círculo del cartel: documento (guía), tilde (en regla) o signo de exclamación (problema). */
function Icono({ tono, cx, cy }: { tono: Tono; cx: number; cy: number }) {
  if (tono === 'guia') {
    return <path d={`M ${cx - 5} ${cy - 7} h 7 l 3 3 v 11 h -10 z`} className="ruta-icono" />
  }
  if (tono === 'ok') {
    return (
      <path
        d={`M ${cx - 7} ${cy} l 5 5 l 9 -11`}
        fill="none"
        stroke="#fff"
        strokeWidth="3"
        strokeLinecap="round"
      />
    )
  }
  return (
    <g>
      <rect x={cx - 1.5} y={cy - 8} width="3" height="10" rx="1.5" className="ruta-icono" />
      <circle cx={cx} cy={cy + 6} r="1.8" className="ruta-icono" />
    </g>
  )
}

/** Cartel con el resultado de la etapa, debajo de la ruta. */
function Cartel({ etapa, indice, resultado, visible }: { etapa: Etapa; indice: number; resultado: Resultado; visible: boolean }) {
  const x = etapa.x - 125
  const y = etapa.y + (indice === 1 ? 54 : 34)
  return (
    <g className={visible ? 'ruta-resultado visible' : 'ruta-resultado'}>
      <rect
        x={x}
        y={y}
        width="250"
        height="56"
        rx="8"
        fill="#fff"
        stroke={resultado.tono === 'problema' ? '#b42318' : '#d5dbd9'}
        strokeWidth={resultado.tono === 'problema' ? 2 : 1}
      />
      <circle cx={x + 27} cy={y + 28} r="13" fill={COLOR_TONO[resultado.tono]} />
      <Icono tono={resultado.tono} cx={x + 27} cy={y + 28} />
      <text x={x + 50} y={y + 24} className={resultado.tono === 'problema' ? 'ruta-pesaje-titulo problema' : 'ruta-pesaje-titulo'}>
        {resultado.cartel}
      </text>
      <text x={x + 50} y={y + 44} className="ruta-pesaje-detalle">
        {resultado.detalle}
      </text>
    </g>
  )
}

export function Ruta() {
  const caminoRef = useRef<SVGPathElement>(null)
  const contenedorRef = useRef<HTMLDivElement>(null)
  const botonesRef = useRef<(HTMLButtonElement | null)[]>([])
  const largosRef = useRef<number[]>([])
  const largoActualRef = useRef(0)
  const animacionRef = useRef<number | null>(null)
  const deslizarRef = useRef<number | null>(null)

  const [etapa, setEtapa] = useState(0)
  const [escenario, setEscenario] = useState<Escenario>('regla')
  const [pose, setPose] = useState<Pose>({ x: ETAPAS[0].x, y: ETAPAS[0].y, angulo: 0 })
  const [largoActual, setLargoActual] = useState(0)
  const [puntosGps, setPuntosGps] = useState<PuntoGps[]>([])
  const [aspecto, setAspecto] = useState(ANCHO_COMPLETO / ALTO)

  // Largo del trazado en cada etapa, puntos de GPS posibles y posición inicial del camión (en la salida)
  useLayoutEffect(() => {
    const camino = caminoRef.current
    if (!camino) return
    largosRef.current = ETAPAS.map((e) => largoEnX(camino, e.x))
    const inicio = largosRef.current[0]
    const fin = largosRef.current[ETAPAS.length - 1]
    const puntos: PuntoGps[] = []
    for (let l = inicio + SEPARACION_GPS; l < fin; l += SEPARACION_GPS) puntos.push(puntoEnCarril(camino, l))
    setPuntosGps(puntos)
    largoActualRef.current = inicio
    setLargoActual(inicio)
    setPose(poseEn(camino, inicio))
  }, [])

  // Relación de aspecto del contenedor: define cuánto de la ruta entra en pantalla
  useEffect(() => {
    const contenedor = contenedorRef.current
    if (!contenedor) return
    const observador = new ResizeObserver(([entrada]) => {
      const { width, height } = entrada.contentRect
      if (width > 0 && height > 0) setAspecto(width / height)
    })
    observador.observe(contenedor)
    return () => observador.disconnect()
  }, [])

  useEffect(
    () => () => {
      if (animacionRef.current !== null) cancelAnimationFrame(animacionRef.current)
    },
    [],
  )

  const irA = useCallback((destino: number) => {
    const camino = caminoRef.current
    if (!camino || destino < 0 || destino >= ETAPAS.length) return
    setEtapa(destino)
    if (animacionRef.current !== null) cancelAnimationFrame(animacionRef.current)
    const desde = largoActualRef.current
    const hasta = largosRef.current[destino]
    const mover = (largo: number) => {
      largoActualRef.current = largo
      setLargoActual(largo)
      setPose(poseEn(camino, largo))
    }
    if (prefiereMenosMovimiento()) {
      mover(hasta)
      return
    }
    const inicio = performance.now()
    const paso = (ahora: number) => {
      const t = Math.min(1, (ahora - inicio) / DURACION_MS)
      mover(desde + (hasta - desde) * suavizar(t))
      animacionRef.current = t < 1 ? requestAnimationFrame(paso) : null
    }
    animacionRef.current = requestAnimationFrame(paso)
  }, [])

  // Flechas del teclado entre los mojones (patrón de barra de herramientas: el foco acompaña a la etapa)
  function alTeclear(e: KeyboardEvent<HTMLOListElement>) {
    const destinos: Record<string, number> = {
      ArrowRight: Math.min(etapa + 1, ETAPAS.length - 1),
      ArrowLeft: Math.max(etapa - 1, 0),
      Home: 0,
      End: ETAPAS.length - 1,
    }
    const destino = destinos[e.key]
    if (destino === undefined) return
    e.preventDefault()
    irA(destino)
    botonesRef.current[destino]?.focus()
  }

  // Deslizar con el dedo sobre la ruta: a la izquierda avanza, a la derecha retrocede
  function alTocar(e: PointerEvent<HTMLDivElement>) {
    if (e.pointerType === 'touch') deslizarRef.current = e.clientX
  }
  function alSoltar(e: PointerEvent<HTMLDivElement>) {
    if (deslizarRef.current === null) return
    const distancia = e.clientX - deslizarRef.current
    deslizarRef.current = null
    if (distancia < -50) irA(etapa + 1)
    else if (distancia > 50) irA(etapa - 1)
  }

  // Vista: si entran las tres etapas con sus carteles (de x=125 a x=1155), se ve la ruta entera;
  // si no, la cámara sigue al camión
  const anchoVisible = ALTO * aspecto
  const entraCompleta = anchoVisible >= 1060
  const centro = entraCompleta ? 640 : pose.x
  const viewBox = `${centro - anchoVisible / 2} 0 ${anchoVisible} ${ALTO}`
  const actual = ETAPAS[etapa][escenario]
  const esUltima = etapa === ETAPAS.length - 1
  const excesoEnBalanza = escenario === 'exceso' && etapa >= 1
  // El GPS reporta por donde ya pasó el camión (los puntos quedan detrás de él)
  const rastro = puntosGps.filter((p) => p.largo < largoActual - 30)

  return (
    <div className="recorrido-interactivo">
      <div className="escenarios" role="group" aria-label="Tipo de viaje">
        <button
          type="button"
          className={escenario === 'regla' ? 'escenario activo' : 'escenario'}
          aria-pressed={escenario === 'regla'}
          onClick={() => setEscenario('regla')}
        >
          Viaje en regla
        </button>
        <button
          type="button"
          className={escenario === 'exceso' ? 'escenario activo' : 'escenario'}
          aria-pressed={escenario === 'exceso'}
          onClick={() => setEscenario('exceso')}
        >
          Viaje con exceso de peso
        </button>
      </div>

      <div
        className="ruta-contenedor"
        ref={contenedorRef}
        onPointerDown={alTocar}
        onPointerUp={alSoltar}
        onPointerCancel={() => (deslizarRef.current = null)}
      >
        <svg className="ruta" viewBox={viewBox} preserveAspectRatio="xMidYMid meet" aria-hidden="true" focusable="false">
          <path ref={caminoRef} d={TRAZADO} className="ruta-banquina" />
          <path d={TRAZADO} className="ruta-asfalto" />
          <path d={TRAZADO} className="ruta-linea" />

          {/* Salida y llegada: carteles al costado de la ruta */}
          <rect x="200" y="50" width="100" height="28" rx="4" fill="#fff" stroke="#26323a" strokeWidth="2" />
          <text x="250" y="69" textAnchor="middle" className="ruta-rotulo">
            Montevideo
          </text>
          <rect x="995" y="54" width="70" height="28" rx="4" fill="#fff" stroke="#26323a" strokeWidth="2" />
          <text x="1030" y="73" textAnchor="middle" className="ruta-rotulo">
            Minas
          </text>

          {/* Peaje: cabina al costado y la balanza cruzando la ruta */}
          <text x="640" y="18" textAnchor="middle" className="ruta-rotulo">
            Peaje Pando, Ruta 8
          </text>
          <rect x="618" y="30" width="44" height="24" rx="3" fill="#fff" stroke="#26323a" strokeWidth="2" />
          <rect x="624" y="36" width="32" height="8" rx="1" fill="#9fc3e8" />
          <rect
            x="632"
            y="61"
            width="16"
            height="50"
            rx="2"
            className={['ruta-balanza', etapa === 1 ? 'pesando' : '', excesoEnBalanza ? 'exceso' : ''].join(' ')}
            transform="rotate(1.76 640 86)"
          />

          {/* Posiciones que reportó el GPS del vehículo */}
          {rastro.map((p) => (
            <circle key={p.largo} cx={p.x} cy={p.y} r="3.5" className="ruta-gps" />
          ))}

          {ETAPAS.map((e, i) => (
            <Cartel key={e.km} etapa={e} indice={i} resultado={e[escenario]} visible={i === etapa} />
          ))}

          <Camion pose={pose} />
        </svg>
      </div>
      <p className="ruta-leyenda">
        <span className="ruta-leyenda-punto" aria-hidden="true" />
        Posiciones que reporta el GPS del vehículo durante el viaje
      </p>

      {/* Las etapas: mojones kilométricos que mueven el camión */}
      <ol className="etapas" aria-label="Etapas del viaje" onKeyDown={alTeclear}>
        {ETAPAS.map((e, i) => (
          <li key={e.km}>
            <button
              type="button"
              ref={(boton) => {
                botonesRef.current[i] = boton
              }}
              className={[
                'etapa',
                i === etapa ? 'activa' : '',
                i === etapa && e[escenario].tono === 'problema' ? 'con-problema' : '',
              ].join(' ')}
              aria-current={i === etapa ? 'step' : undefined}
              onClick={() => irA(i)}
            >
              <span className="mojon" aria-hidden="true">
                <span className="mojon-km">km</span>
                <span className="mojon-numero">{e.km}</span>
              </span>
              <span className="etapa-nombre">{e.corto}</span>
            </button>
          </li>
        ))}
      </ol>

      <div className="etapa-detalle" aria-live="polite">
        <h3>{actual.titulo}</h3>
        <p>{actual.texto}</p>
        <button type="button" className="boton-secundario" onClick={() => irA(esUltima ? 0 : etapa + 1)}>
          {esUltima ? 'Volver a la salida' : 'Siguiente etapa'}
        </button>
      </div>
    </div>
  )
}
