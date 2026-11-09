import { useEffect, useRef, useState } from 'react'
import type { PuntoMapa } from '../api/planificacionGuias'

type Coordenada = [number, number]
interface Mapa {
  setView(punto: Coordenada, zoom: number): Mapa
  fitBounds(puntos: Coordenada[], opciones: { padding: Coordenada; maxZoom: number }): Mapa
  invalidateSize(): Mapa
  remove(): void
}
interface Capa { addTo(destino: Mapa | Grupo): Capa }
interface Fondo extends Capa { on(evento: 'tileerror', accion: () => void): Fondo }
interface Grupo extends Capa { clearLayers(): void }
interface Marcador extends Capa { bindTooltip(texto: HTMLElement, opciones: { permanent: boolean; direction: string }): Marcador }
interface Leaflet {
  map(elemento: HTMLElement): Mapa
  tileLayer(url: string, opciones: { maxZoom: number; attribution: string; referrerPolicy: ReferrerPolicy }): Fondo
  layerGroup(): Grupo
  polyline(puntos: Coordenada[], opciones: { color: string; weight: number }): Capa
  circleMarker(punto: Coordenada, opciones: { radius: number; color: string; fillColor: string; fillOpacity: number }): Marcador
}

let cargaLeaflet: Promise<Leaflet> | null = null

/** Misma versión y CDN que panel.xhtml, cargada solo al necesitar este mapa. */
function cargarLeaflet(): Promise<Leaflet> {
  if (cargaLeaflet) return cargaLeaflet
  cargaLeaflet = new Promise<Leaflet>((resolve, reject) => {
    const css = document.createElement('link')
    css.rel = 'stylesheet'; css.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'
    css.integrity = 'sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY='; css.crossOrigin = 'anonymous'
    const script = document.createElement('script')
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'; script.async = true
    script.integrity = 'sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo='; script.crossOrigin = 'anonymous'
    let cssLista = false, jsLista = false
    const fallo = () => {
      window.clearTimeout(espera); css.remove(); script.remove()
      reject(new Error('No se pudo cargar el mapa. Puedes continuar con las direcciones y coordenadas.'))
    }
    const completar = () => {
      if (!cssLista || !jsLista) return
      window.clearTimeout(espera)
      const leaflet = (window as Window & { L?: Leaflet }).L
      if (leaflet) resolve(leaflet); else fallo()
    }
    const espera = window.setTimeout(fallo, 15000)
    css.onload = () => { cssLista = true; completar() }; css.onerror = fallo
    script.onload = () => { jsLista = true; completar() }; script.onerror = fallo
    document.head.append(css, script)
  }).catch((error: unknown) => { cargaLeaflet = null; throw error })
  return cargaLeaflet
}

interface Props { origen: PuntoMapa | null; destino: PuntoMapa | null; puntos: PuntoMapa[] | null }

export function MapaGuia({ origen, destino, puntos }: Props) {
  const contenedor = useRef<HTMLDivElement>(null)
  const instancia = useRef<{ mapa: Mapa; grupo: Grupo; leaflet: Leaflet } | null>(null)
  const [listo, setListo] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [falloFondo, setFalloFondo] = useState(false)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let vigente = true
    let mapaLocal: Mapa | null = null
    let observador: ResizeObserver | null = null
    setListo(false); setError(null); setFalloFondo(false)
    cargarLeaflet().then((leaflet) => {
      if (!vigente || !contenedor.current) return
      mapaLocal = leaflet.map(contenedor.current).setView([-33.9, -56.3], 7)
      leaflet.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 18, attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap contributors</a>',
        // OSM exige Referer: enviar solo el origen sin cambiar la politica global de nginx.
        referrerPolicy: 'strict-origin-when-cross-origin',
      }).on('tileerror', () => { if (vigente) setFalloFondo(true) }).addTo(mapaLocal)
      const grupo = leaflet.layerGroup(); grupo.addTo(mapaLocal)
      instancia.current = { mapa: mapaLocal, grupo, leaflet }
      observador = new ResizeObserver(() => mapaLocal?.invalidateSize())
      observador.observe(contenedor.current); setListo(true)
    }).catch((causa: unknown) => { if (vigente) setError(causa instanceof Error ? causa.message : 'Mapa no disponible.') })
    return () => { vigente = false; observador?.disconnect(); mapaLocal?.remove(); instancia.current = null }
  }, [intento])

  useEffect(() => {
    if (!listo || !instancia.current) return
    const { mapa, grupo, leaflet } = instancia.current
    grupo.clearLayers()
    const limites: Coordenada[] = []
    if (puntos) {
      const linea: Coordenada[] = puntos.map((p) => [p.latitud, p.longitud])
      leaflet.polyline(linea, { color: '#145b85', weight: 5 }).addTo(grupo)
      limites.push(...linea)
    }
    for (const [punto, nombre, color] of [[origen, 'Origen', '#237347'], [destino, 'Destino', '#b03a32']] as const) {
      if (!punto) continue
      const coordenada: Coordenada = [punto.latitud, punto.longitud]
      const etiqueta = document.createElement('span'); etiqueta.textContent = nombre
      leaflet.circleMarker(coordenada, { radius: 8, color, fillColor: color, fillOpacity: 0.9 })
        .bindTooltip(etiqueta, { permanent: true, direction: 'top' }).addTo(grupo)
      limites.push(coordenada)
    }
    if (limites.length === 1) mapa.setView(limites[0], 15)
    else if (limites.length > 1) mapa.fitBounds(limites, { padding: [35, 35], maxZoom: 15 })
    else mapa.setView([-33.9, -56.3], 7)
  }, [listo, origen?.latitud, origen?.longitud, destino?.latitud, destino?.longitud, puntos])

  return <div className="mapa-guia-contenedor">
    {!listo && !error && <p role="status">Cargando mapa…</p>}
    {error && <div><p role="alert" className="mensaje-error">{error}</p>
      <button type="button" className="boton-secundario boton-chico" onClick={() => setIntento((v) => v + 1)}>Reintentar mapa</button></div>}
    <div ref={contenedor} className="mapa-guia" role="region" aria-label="Mapa de origen, destino y recorrido sugerido" />
    {falloFondo && <p role="status">No se pudo cargar parte del fondo de OpenStreetMap. Las coordenadas y el formulario siguen disponibles.</p>}
    <p className="mapa-guia-creditos">Direcciones: <a href="https://direcciones.ide.uy/">IDE Uruguay</a>. Recorrido: <a href="https://openrouteservice.org/">© openrouteservice.org by HeiGIT</a>.</p>
  </div>
}
