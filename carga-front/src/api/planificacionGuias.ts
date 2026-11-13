import { api } from './cliente'

export interface PuntoMapa { latitud: number; longitud: number }
export interface SugerenciaCalle {
  direccion: string
  tipo: 'CALLE' | 'CALLEyPORTAL' | 'ESQUINA'
  idCalle: number
  idCalleEsquina?: number | null
  numeroPuerta?: number | null
  letra?: string | null
}
export interface DireccionMapa { direccion: string; punto: PuntoMapa; aproximada: boolean; detalle: string }
export interface SolicitudRuta { vehiculoId: number; cargaKg: number; origen: PuntoMapa; destino: PuntoMapa }
export interface RutaPrevia {
  puntos: PuntoMapa[]
  distanciaMetros: number
  duracionSegundos: number
  pesoTotalKg: number
  advertenciasProveedor: boolean
}

function objeto(valor: unknown): Record<string, unknown> {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) throw new Error('Respuesta geográfica inválida.')
  return valor as Record<string, unknown>
}
function numero(valor: unknown): valor is number { return typeof valor === 'number' && Number.isFinite(valor) }
function identificador(valor: unknown): valor is number { return numero(valor) && Number.isSafeInteger(valor) && valor > 0 }
function texto(valor: unknown, maximo: number): valor is string {
  return typeof valor === 'string' && valor.trim().length > 0 && valor.length <= maximo
}
function punto(valor: unknown): PuntoMapa {
  const p = objeto(valor)
  if (!numero(p.latitud) || !numero(p.longitud) || Math.abs(p.latitud) > 90 || Math.abs(p.longitud) > 180
      || (p.latitud === 0 && p.longitud === 0)) throw new Error('La ubicación recibida no es válida.')
  return { latitud: p.latitud, longitud: p.longitud }
}

/** No interpreta campos vacíos como coordenadas cero. */
export function puntoDeCampos(latitud: string, longitud: string): PuntoMapa | null {
  if (!latitud.trim() || !longitud.trim()) return null
  try { return punto({ latitud: Number(latitud), longitud: Number(longitud) }) } catch { return null }
}

export async function sugerirCalles(q: string, signal: AbortSignal): Promise<SugerenciaCalle[]> {
  const datos = await api<unknown>(`/planificacion-guias/calles?${new URLSearchParams({ q })}`, { signal })
  if (!Array.isArray(datos) || datos.length > 5) throw new Error('No se pudieron interpretar las sugerencias de IDE.')
  return datos.map((valor: unknown) => {
    const s = objeto(valor)
    if (!texto(s.direccion, 200) || !identificador(s.idCalle)
        || (s.tipo !== 'CALLE' && s.tipo !== 'CALLEyPORTAL' && s.tipo !== 'ESQUINA')
        || (s.tipo === 'CALLEyPORTAL' && !identificador(s.numeroPuerta))
        || (s.tipo === 'ESQUINA' && !identificador(s.idCalleEsquina))
        || (s.letra != null && (typeof s.letra !== 'string' || s.letra.length > 10))) {
      throw new Error('IDE devolvió una sugerencia que no se pudo interpretar.')
    }
    return { direccion: s.direccion, tipo: s.tipo, idCalle: s.idCalle,
      idCalleEsquina: identificador(s.idCalleEsquina) ? s.idCalleEsquina : null,
      numeroPuerta: identificador(s.numeroPuerta) ? s.numeroPuerta : null,
      letra: typeof s.letra === 'string' ? s.letra : null }
  })
}

export async function resolverDireccion(seleccion: SugerenciaCalle, signal: AbortSignal): Promise<DireccionMapa> {
  return direccionMapa(await api<unknown>('/planificacion-guias/direccion', {
    method: 'POST', body: JSON.stringify(seleccion), signal,
  }))
}

export async function resolverAlternativas(seleccion: SugerenciaCalle, signal: AbortSignal): Promise<DireccionMapa[]> {
  const datos = await api<unknown>('/planificacion-guias/direccion/alternativas', {
    method: 'POST', body: JSON.stringify(seleccion), signal,
  })
  if (!Array.isArray(datos) || datos.length > 50) throw new Error('No se pudieron interpretar las ubicaciones de IDE.')
  return datos.map(direccionMapa)
}

function direccionMapa(valor: unknown): DireccionMapa {
  const d = objeto(valor)
  if (!texto(d.direccion, 200) || typeof d.aproximada !== 'boolean' || !texto(d.detalle, 300))
    throw new Error('No se pudo interpretar la dirección localizada.')
  return { direccion: d.direccion, punto: punto(d.punto), aproximada: d.aproximada, detalle: d.detalle }
}

export async function calcularRuta(solicitud: SolicitudRuta, signal: AbortSignal): Promise<RutaPrevia> {
  const r = objeto(await api<unknown>('/planificacion-guias/ruta', {
    method: 'POST', body: JSON.stringify(solicitud), signal,
  }))
  if (!Array.isArray(r.puntos) || r.puntos.length < 2 || r.puntos.length > 25000
      || !numero(r.distanciaMetros) || r.distanciaMetros < 0 || !numero(r.duracionSegundos) || r.duracionSegundos < 0
      || !numero(r.pesoTotalKg) || r.pesoTotalKg <= solicitud.cargaKg || typeof r.advertenciasProveedor !== 'boolean')
    throw new Error('No se pudo interpretar el recorrido. No se mostrará una ruta incompleta.')
  return { puntos: r.puntos.map(punto), distanciaMetros: r.distanciaMetros, duracionSegundos: r.duracionSegundos,
    pesoTotalKg: r.pesoTotalKg, advertenciasProveedor: r.advertenciasProveedor }
}
