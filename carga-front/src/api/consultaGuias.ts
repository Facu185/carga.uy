import { api } from './cliente'
import type { Guia } from '../tipos'

export const ESTADOS_GUIA = ['CREADA', 'ASIGNADA', 'EN_CURSO', 'FINALIZADA'] as const
export type EstadoGuia = typeof ESTADOS_GUIA[number]

// La lectura historica admite volumen ausente y no aplica las reglas de alta.
type GuiaConsulta = Pick<Guia, 'id' | 'empresaId' | 'vehiculoId' | 'choferId' | 'rubroId' | 'tipoCargaId'
  | 'pesoKg' | 'inicio' | 'fin' | 'origen' | 'destino' | 'creadaEn'> & {
  volumenM3: number | null
  estado: EstadoGuia
}

export interface ConsultaGuia {
  guia: GuiaConsulta
  nombreEmpresa: string
  matricula: string | null
  nombreChofer: string | null
}

export interface PaginaConsultaGuias {
  registros: ConsultaGuia[]
  pagina: number
  tamano: number
  total: number
}

function objeto(valor: unknown): Record<string, unknown> {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) throw new Error('Respuesta de consulta de viajes no válida.')
  return valor as Record<string, unknown>
}
function entero(valor: unknown): valor is number { return typeof valor === 'number' && Number.isSafeInteger(valor) }
function idOpcional(valor: unknown): valor is number | null | undefined { return valor == null || (entero(valor) && valor > 0) }
function textoOpcional(valor: unknown): valor is string | null | undefined { return valor == null || typeof valor === 'string' }
function cantidad(valor: unknown): valor is number { return typeof valor === 'number' && Number.isFinite(valor) && valor >= 0 }
function instante(valor: unknown): valor is string {
  return typeof valor === 'string' && /T.+(?:Z|[+-]\d{2}:\d{2})$/.test(valor) && Number.isFinite(Date.parse(valor))
}
export function esEstadoGuia(valor: unknown): valor is EstadoGuia {
  return typeof valor === 'string' && ESTADOS_GUIA.some((estado) => estado === valor)
}
export function identificadorValido(texto: string): boolean {
  return /^[1-9]\d*$/.test(texto) && Number.isSafeInteger(Number(texto))
}

function interpretarGuia(valor: unknown, empresaId: number): ConsultaGuia {
  const doc = objeto(valor), g = objeto(doc.guia)
  if (!entero(g.id) || g.id <= 0 || g.empresaId !== empresaId || typeof doc.nombreEmpresa !== 'string'
      || !textoOpcional(doc.matricula) || !textoOpcional(doc.nombreChofer)
      || !idOpcional(g.vehiculoId) || !idOpcional(g.choferId) || !idOpcional(g.rubroId) || !idOpcional(g.tipoCargaId)
      || !cantidad(g.pesoKg) || (g.volumenM3 != null && !cantidad(g.volumenM3))
      || !instante(g.inicio) || !instante(g.fin) || !instante(g.creadaEn) || !esEstadoGuia(g.estado)
      || typeof g.origen !== 'string' || typeof g.destino !== 'string') {
    throw new Error('No se pudieron interpretar los datos de un viaje.')
  }
  return { nombreEmpresa: doc.nombreEmpresa, matricula: doc.matricula ?? null, nombreChofer: doc.nombreChofer ?? null,
    guia: { id: g.id, empresaId, vehiculoId: g.vehiculoId ?? null, choferId: g.choferId ?? null,
      rubroId: g.rubroId ?? null, tipoCargaId: g.tipoCargaId ?? null, pesoKg: g.pesoKg, volumenM3: g.volumenM3 ?? null,
      inicio: g.inicio, fin: g.fin, creadaEn: g.creadaEn, estado: g.estado, origen: g.origen, destino: g.destino } }
}

/** C14 P2: nunca consulta fuentes supuestas de eventos o pesajes. */
export async function consultarGuias(empresaId: number, desde: string, hasta: string, estado: string,
  pagina: number, tamano: number, signal: AbortSignal): Promise<PaginaConsultaGuias> {
  const parametros = new URLSearchParams({ empresaId: String(empresaId), pagina: String(pagina), tamano: String(tamano) })
  if (desde) parametros.set('desde', desde)
  if (hasta) parametros.set('hasta', hasta)
  if (estado) parametros.set('estado', estado)
  const doc = objeto(await api<unknown>(`/guias/consulta?${parametros}`, { signal }))
  if (doc.pagina !== pagina || doc.tamano !== tamano || !entero(doc.total) || doc.total < 0
      || !Array.isArray(doc.registros) || doc.registros.length > tamano) {
    throw new Error('No se pudo interpretar la página de viajes.')
  }
  const registros = doc.registros.map((valor: unknown) => interpretarGuia(valor, empresaId))
  if (new Set(registros.map((registro) => registro.guia.id)).size !== registros.length)
    throw new Error('La consulta contiene viajes repetidos.')
  return { registros, pagina, tamano, total: doc.total }
}

export async function consultarDetalleGuia(empresaId: number, guiaId: number, signal: AbortSignal): Promise<ConsultaGuia> {
  const parametros = new URLSearchParams({ empresaId: String(empresaId) })
  const registro = interpretarGuia(await api<unknown>(`/guias/consulta/${guiaId}?${parametros}`, { signal }), empresaId)
  if (registro.guia.id !== guiaId) throw new Error('El detalle recibido no corresponde a la guía solicitada.')
  return registro
}
