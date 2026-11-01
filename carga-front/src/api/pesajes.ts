import { api } from './cliente'

export const LIMITE_PESAJES_GUIA = 500

export interface PesadaRegistrada {
  id: number
  idPesada: string
  guiaId: number
  matricula: string
  peaje: string | null
  pesoKg: number
  instante: string
  obtenidaEn: string
}

export interface PesajesDeGuia {
  guiaId: number
  matricula: string
  desde: string
  hasta: string
  pesadas: PesadaRegistrada[]
  cantidad: number
  truncado: boolean
  nodoDisponible: boolean
}

function objeto(valor: unknown): Record<string, unknown> {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) throw respuestaInvalida()
  return valor as Record<string, unknown>
}
function respuestaInvalida(): Error { return new Error('No se pudo interpretar la respuesta de pesajes de la guía.') }
function id(valor: unknown): valor is number { return typeof valor === 'number' && Number.isSafeInteger(valor) && valor > 0 }
function texto(valor: unknown): valor is string { return typeof valor === 'string' && valor.trim().length > 0 }
function instante(valor: unknown): valor is string {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}T(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d(?:\.\d{1,9})?)?(?:Z|[+-]\d{2}:\d{2})$/.test(valor)
      || !Number.isFinite(Date.parse(valor))) return false
  // Date.parse puede normalizar dias imposibles; no presentarlos como fechas validas.
  const fecha = valor.slice(0, 10)
  return new Date(`${fecha}T00:00:00Z`).toISOString().slice(0, 10) === fecha
}

function interpretarPesada(valor: unknown, guiaId: number): PesadaRegistrada {
  const p = objeto(valor)
  if (!id(p.id) || !texto(p.idPesada) || p.guiaId !== guiaId || !texto(p.matricula)
      || (p.peaje != null && typeof p.peaje !== 'string')
      || typeof p.pesoKg !== 'number' || !Number.isSafeInteger(p.pesoKg) || p.pesoKg < 0
      || !instante(p.instante) || !instante(p.obtenidaEn)) throw respuestaInvalida()
  return { id: p.id, idPesada: p.idPesada, guiaId, matricula: p.matricula,
    peaje: p.peaje ?? null, pesoKg: p.pesoKg, instante: p.instante, obtenidaEn: p.obtenidaEn }
}

/** Lee asociaciones ya guardadas por P4: no sincroniza ni las reconstruye por fechas o matricula. */
export async function consultarPesajesDeGuia(guiaId: number, signal: AbortSignal): Promise<PesajesDeGuia> {
  if (!id(guiaId)) throw new Error('La guía debe tener un identificador positivo válido.')
  const doc = objeto(await api<unknown>(`/pesajes/guia/${guiaId}`, { signal }))
  if (doc.guiaId !== guiaId || !texto(doc.matricula) || !instante(doc.desde) || !instante(doc.hasta)
      || !Array.isArray(doc.pesadas) || doc.pesadas.length > LIMITE_PESAJES_GUIA
      || doc.cantidad !== doc.pesadas.length || typeof doc.truncado !== 'boolean'
      || typeof doc.nodoDisponible !== 'boolean') throw respuestaInvalida()
  const pesadas = doc.pesadas.map((valor: unknown) => interpretarPesada(valor, guiaId))
  if (new Set(pesadas.map((p) => p.id)).size !== pesadas.length
      || new Set(pesadas.map((p) => p.idPesada)).size !== pesadas.length) throw respuestaInvalida()
  return { guiaId, matricula: doc.matricula, desde: doc.desde, hasta: doc.hasta, pesadas,
    cantidad: pesadas.length, truncado: doc.truncado, nodoDisponible: doc.nodoDisponible }
}
