import { useEffect, useId, useRef, useState, type KeyboardEvent } from 'react'
import { resolverAlternativas, sugerirCalles, type DireccionMapa, type SugerenciaCalle } from '../api/planificacionGuias'

interface Props {
  etiqueta: string
  valor: string
  seleccion: DireccionMapa | null
  revisionManual: number
  deshabilitado: boolean
  alEditar: (texto: string) => void
  alSeleccionar: (direccion: DireccionMapa) => void
}
interface Resultado { consulta: string; opciones: SugerenciaCalle[]; error?: string }
interface Ubicaciones {
  consulta: string
  revisionManual: number
  sugerencia: SugerenciaCalle
  alternativas: DireccionMapa[]
  error?: string
}

/** Combobox accesible; el texto escrito no equivale a una ubicación confirmada. */
export function CampoDireccionIde({ etiqueta, valor, seleccion, revisionManual, deshabilitado, alEditar, alSeleccionar }: Props) {
  const id = useId()
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [ubicaciones, setUbicaciones] = useState<Ubicaciones | null>(null)
  const [buscando, setBuscando] = useState<string | null>(null)
  const [resolviendo, setResolviendo] = useState(false)
  const [abierto, setAbierto] = useState(false)
  const [indice, setIndice] = useState(-1)
  const version = useRef(0)
  const resolucion = useRef<AbortController | null>(null)
  const ubicacionesActuales = ubicaciones?.consulta === valor && ubicaciones.revisionManual === revisionManual ? ubicaciones : null

  useEffect(() => () => { version.current++; resolucion.current?.abort() }, [])

  useEffect(() => {
    // Una respuesta pendiente de IDE no debe sobrescribir coordenadas editadas manualmente.
    version.current++; resolucion.current?.abort(); setResolviendo(false)
    setUbicaciones(null); setResultado(null); setBuscando(null); setAbierto(false); setIndice(-1)
  }, [revisionManual])

  useEffect(() => {
    if (valor.trim().length < 3 || seleccion || resolviendo || deshabilitado || ubicacionesActuales) return
    const control = new AbortController()
    const consulta = valor
    const solicitud = version.current
    const espera = window.setTimeout(() => {
      setBuscando(consulta)
      sugerirCalles(consulta.trim(), control.signal)
        .then((opciones) => {
          if (!control.signal.aborted && solicitud === version.current) { setResultado({ consulta, opciones }); setIndice(-1) }
        })
        .catch((causa: unknown) => {
          if (!control.signal.aborted && solicitud === version.current)
            setResultado({ consulta, opciones: [], error: causa instanceof Error ? causa.message : 'No se pudieron consultar las calles.' })
        })
        .finally(() => { if (!control.signal.aborted && solicitud === version.current) setBuscando(null) })
    }, 350)
    return () => { window.clearTimeout(espera); control.abort() }
  }, [valor, seleccion, resolviendo, deshabilitado, ubicacionesActuales, revisionManual])

  const actual = resultado?.consulta === valor ? resultado : null
  const opciones = abierto && !seleccion && !resolviendo && !ubicacionesActuales ? actual?.opciones ?? [] : []

  function editar(texto: string) {
    version.current++; resolucion.current?.abort()
    setResolviendo(false); setResultado(null); setBuscando(null); setIndice(-1); setAbierto(true)
    setUbicaciones(null)
    alEditar(texto)
  }

  async function elegir(sugerencia: SugerenciaCalle) {
    resolucion.current?.abort()
    const control = new AbortController(); resolucion.current = control
    const solicitud = ++version.current
    const estado: Ubicaciones = { consulta: sugerencia.direccion, revisionManual, sugerencia, alternativas: [] }
    setAbierto(false); setResolviendo(true); setResultado(null); setBuscando(null)
    setUbicaciones(estado)
    alEditar(sugerencia.direccion)
    try {
      const alternativas = await resolverAlternativas(sugerencia, control.signal)
      if (!control.signal.aborted && solicitud === version.current) {
        setUbicaciones({ ...estado, alternativas, error: alternativas.length === 0
          ? sugerencia.tipo === 'CALLEyPORTAL'
            ? 'IDE no encontró una ubicación para ese número de puerta. Revisa el número o usa «Ver o ingresar coordenadas manualmente».'
            : 'IDE no encontró una ubicación para esa dirección. Revisa el texto o usa «Ver o ingresar coordenadas manualmente».'
          : undefined })
        if (alternativas.length === 1) alSeleccionar(alternativas[0])
      }
    } catch (causa) {
      if (!control.signal.aborted && solicitud === version.current)
        setUbicaciones({ ...estado, error: causa instanceof Error ? causa.message : 'No se pudo localizar la dirección.' })
    } finally {
      if (!control.signal.aborted && solicitud === version.current) setResolviendo(false)
    }
  }

  function teclado(evento: KeyboardEvent<HTMLInputElement>) {
    if (evento.key === 'Escape') { setAbierto(false); setIndice(-1) }
    if (evento.key === 'ArrowDown' || evento.key === 'ArrowUp') {
      evento.preventDefault(); setAbierto(true)
      if (opciones.length) setIndice((anterior) => evento.key === 'ArrowDown'
        ? (anterior + 1) % opciones.length : (anterior <= 0 ? opciones.length - 1 : anterior - 1))
    }
    if (evento.key === 'Enter') {
      evento.preventDefault()
      if (indice >= 0 && opciones[indice]) void elegir(opciones[indice])
    }
  }

  return <div className="direccion-ide">
    <label className="campo" htmlFor={id}><span>{etiqueta}</span></label>
    <input id={id} role="combobox" aria-autocomplete="list" aria-expanded={opciones.length > 0}
      aria-controls={`${id}-opciones`} aria-activedescendant={indice >= 0 && opciones[indice] ? `${id}-opcion-${indice}` : undefined}
      aria-describedby={`${id}-ayuda`} autoComplete="off" placeholder="Calle y número, localidad, departamento"
      value={valor} onChange={(e) => editar(e.target.value)} onKeyDown={teclado}
      onFocus={() => setAbierto(true)} onBlur={() => { setAbierto(false); setIndice(-1) }} maxLength={200} required disabled={deshabilitado} />
    <ul id={`${id}-opciones`} role="listbox" aria-label={`Sugerencias de ${etiqueta.toLowerCase()}`}
      className="direccion-ide-opciones" hidden={opciones.length === 0}>
      {opciones.map((opcion, i) => <li key={`${opcion.tipo}-${opcion.idCalle}-${opcion.numeroPuerta}-${opcion.letra}-${opcion.idCalleEsquina}-${i}`}
        id={`${id}-opcion-${i}`} role="option" aria-selected={indice === i}
        onMouseDown={(e) => e.preventDefault()} onClick={() => void elegir(opcion)}>
        {opcion.direccion}{opcion.tipo === 'CALLE' && <small>Ubicación aproximada de la calle</small>}
      </li>)}
    </ul>
    <p id={`${id}-ayuda`} className="direccion-ide-ayuda" aria-live="polite">
      {resolviendo ? 'Localizando la dirección seleccionada…' : seleccion ? seleccion.detalle
        : ubicacionesActuales?.error ?? (ubicacionesActuales && ubicacionesActuales.alternativas.length > 1
          ? 'IDE encontró varias ubicaciones. Elige una alternativa para confirmar el punto.'
        : buscando === valor ? 'Buscando calles en IDE Uruguay…' : actual?.error ?? (actual && abierto && !actual.opciones.length
          ? 'Sin coincidencias de calles. Agrega localidad y departamento, o usa coordenadas manuales.'
          : 'Escribe al menos 3 caracteres y selecciona una sugerencia de IDE Uruguay.'))}
    </p>
    {!seleccion && !resolviendo && ubicacionesActuales && <div className="direccion-ide-ubicaciones">
      {ubicacionesActuales.alternativas.length > 1 && <fieldset disabled={deshabilitado}>
        <legend>Ubicaciones de {etiqueta.toLowerCase()}: selecciona una</legend>
        <ul>{ubicacionesActuales.alternativas.map((direccion, i) => <li key={i}>
          <button type="button" className="boton-secundario" onClick={() => alSeleccionar(direccion)}>
            {direccion.direccion}
            <small>{direccion.detalle}</small>
            <small>Coordenadas: {direccion.punto.latitud}, {direccion.punto.longitud}</small>
          </button>
        </li>)}</ul>
      </fieldset>}
      {ubicacionesActuales.error && <button type="button" className="boton-secundario boton-chico" disabled={deshabilitado}
        onClick={() => void elegir(ubicacionesActuales.sugerencia)}>Reintentar localización</button>}
    </div>}
  </div>
}
