import { api } from './cliente'
import { ESTADOS_GUIA } from './consultaGuias'

export interface ConteoActividad {
  clave: string
  etiqueta: string
  cantidad: number
}

export type CriterioActividad = 'CREACION_GUIA' | 'FIN_VIAJE'

export interface ReporteActividad {
  desde: string
  hasta: string
  zonaHoraria: 'America/Montevideo'
  criterioTemporal: CriterioActividad
  tipoCargaId: number | null
  tipoCargaNombre: string | null
  totalGuias: number
  empresasConGuias: number
  porDia: ConteoActividad[]
  porEstado: ConteoActividad[]
  porRubro: ConteoActividad[]
}

export interface TipoCargaActividad { id: number; nombre: string }

export function errorTipoCargaActividad(id: string | null): string | null {
  return id === null || (/^[1-9]\d*$/.test(id) && Number.isSafeInteger(Number(id)))
    ? null : 'Selecciona un tipo de carga válido o Todos los tipos.'
}

/** Etiqueta derivada de la respuesta aplicada, nunca del borrador o del catálogo pendiente. */
export function etiquetaCargaActividad(actividad: ReporteActividad): string {
  return actividad.tipoCargaId === null ? 'Todos los tipos (incluidas guías sin tipo)'
    : `${actividad.tipoCargaNombre} (ID ${actividad.tipoCargaId})`
}

function invalido(): never { throw new Error('El servidor devolvió un reporte de actividad no válido. Vuelve a consultar.') }
function objeto(valor: unknown): Record<string, unknown> {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) return invalido()
  return valor as Record<string, unknown>
}
function cantidad(valor: unknown): valor is number {
  return typeof valor === 'number' && Number.isSafeInteger(valor) && valor >= 0
}

function fecha(valor: unknown): valor is string {
  if (typeof valor !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(valor) || valor < '0001-01-01') return false
  const tiempo = Date.parse(`${valor}T00:00:00Z`)
  return Number.isFinite(tiempo) && new Date(tiempo).toISOString().slice(0, 10) === valor
}

/** Aritmética de etiquetas de calendario, NO conversión de medianoches de Montevideo. */
function diasDelPeriodo(desde: string, hasta: string): string[] {
  const inicio = Date.parse(`${desde}T00:00:00Z`), fin = Date.parse(`${hasta}T00:00:00Z`)
  const dias = (fin - inicio) / 86_400_000 + 1
  if (!Number.isInteger(dias) || dias < 1 || dias > 366) return invalido()
  return Array.from({ length: dias }, (_, i) => new Date(inicio + i * 86_400_000).toISOString().slice(0, 10))
}

export function errorPeriodoActividad(desde: string, hasta: string): string | null {
  if (!fecha(desde) || !fecha(hasta)) return 'Completa ambas fechas con días de calendario válidos.'
  const dias = (Date.parse(`${hasta}T00:00:00Z`) - Date.parse(`${desde}T00:00:00Z`)) / 86_400_000 + 1
  return dias < 1 || dias > 366 ? 'El período debe estar ordenado y abarcar entre 1 y 366 días inclusivos.' : null
}

function grupos(valor: unknown, total: number): ConteoActividad[] {
  if (!Array.isArray(valor)) return invalido()
  const vistos = new Set<string>()
  const resultado = valor.map((elemento: unknown) => {
    const grupo = objeto(elemento)
    if (typeof grupo.clave !== 'string' || !grupo.clave || vistos.has(grupo.clave)
        || typeof grupo.etiqueta !== 'string' || !grupo.etiqueta.trim() || !cantidad(grupo.cantidad)) return invalido()
    vistos.add(grupo.clave)
    return { clave: grupo.clave, etiqueta: grupo.etiqueta, cantidad: grupo.cantidad }
  })
  const suma = resultado.reduce((acumulado, grupo) => acumulado + grupo.cantidad, 0)
  if (!Number.isSafeInteger(suma) || suma !== total) return invalido()
  return resultado
}

/** El genérico de api no valida JSON: comprobar significado antes de graficar. */
function interpretarReporte(valor: unknown, desde: string | null, hasta: string | null, tipoSolicitado: string | null,
  criterio: CriterioActividad): ReporteActividad {
  const doc = objeto(valor)
  let tipoCargaId: number | null = null, tipoCargaNombre: string | null = null
  if (tipoSolicitado === null) {
    if (doc.tipoCargaId != null || doc.tipoCargaNombre != null) return invalido()
  } else {
    if (!cantidad(doc.tipoCargaId) || doc.tipoCargaId === 0 || String(doc.tipoCargaId) !== tipoSolicitado
        || typeof doc.tipoCargaNombre !== 'string' || !doc.tipoCargaNombre.trim()) return invalido()
    tipoCargaId = doc.tipoCargaId; tipoCargaNombre = doc.tipoCargaNombre
  }
  if (!fecha(doc.desde) || !fecha(doc.hasta) || (desde !== null && doc.desde !== desde)
      || (hasta !== null && doc.hasta !== hasta) || doc.zonaHoraria !== 'America/Montevideo'
      || doc.criterioTemporal !== criterio || !cantidad(doc.totalGuias) || !cantidad(doc.empresasConGuias)
      || doc.empresasConGuias > doc.totalGuias || (doc.totalGuias > 0 && doc.empresasConGuias === 0)) return invalido()
  const fechas = diasDelPeriodo(doc.desde, doc.hasta)
  if (desde === null && hasta === null && fechas.length !== 30) return invalido()
  const porDia = grupos(doc.porDia, doc.totalGuias), porEstado = grupos(doc.porEstado, doc.totalGuias)
  const porRubro = grupos(doc.porRubro, doc.totalGuias)
  if (porDia.length !== fechas.length || porDia.some((dia, i) => dia.clave !== fechas[i] || dia.etiqueta !== fechas[i])
      || porEstado.length !== ESTADOS_GUIA.length
      || porEstado.some((estado, i) => estado.clave !== ESTADOS_GUIA[i] || estado.etiqueta !== estado.clave)) return invalido()
  for (let i = 0; i < porRubro.length; i++) {
    const actual = porRubro[i], anterior = porRubro[i - 1]
    if (actual.cantidad === 0 || (actual.clave !== 'SIN_RUBRO' && !/^[1-9]\d*$/.test(actual.clave))
        || (actual.clave === 'SIN_RUBRO' && actual.etiqueta !== 'Sin rubro')
        || (anterior && (anterior.cantidad < actual.cantidad
          || (anterior.cantidad === actual.cantidad && anterior.clave > actual.clave)))) return invalido()
  }
  return { desde: doc.desde, hasta: doc.hasta, zonaHoraria: doc.zonaHoraria, criterioTemporal: criterio,
    tipoCargaId, tipoCargaNombre,
    totalGuias: doc.totalGuias, empresasConGuias: doc.empresasConGuias, porDia, porEstado, porRubro }
}

export async function consultarActividad(desde: string | null, hasta: string | null, signal: AbortSignal,
  tipoCargaId: string | null = null): Promise<ReporteActividad> {
  return consultarReporte(desde, hasta, signal, tipoCargaId, 'CREACION_GUIA')
}

export async function consultarViajesFinalizados(desde: string | null, hasta: string | null, signal: AbortSignal,
  tipoCargaId: string | null = null): Promise<ReporteActividad> {
  return consultarReporte(desde, hasta, signal, tipoCargaId, 'FIN_VIAJE')
}

async function consultarReporte(desde: string | null, hasta: string | null, signal: AbortSignal,
  tipoCargaId: string | null, criterio: CriterioActividad): Promise<ReporteActividad> {
  const errorTipo = errorTipoCargaActividad(tipoCargaId)
  if (errorTipo) throw new Error(errorTipo)
  const parametros = new URLSearchParams()
  if (desde !== null) parametros.set('desde', desde)
  if (hasta !== null) parametros.set('hasta', hasta)
  if (tipoCargaId !== null) parametros.set('tipoCargaId', tipoCargaId)
  const recurso = criterio === 'FIN_VIAJE' ? 'viajes-finalizados' : 'actividad'
  const doc = await api<unknown>(`/reportes/${recurso}?${parametros}`, { signal, cache: 'no-store' })
  return interpretarReporte(doc, desde, hasta, tipoCargaId, criterio)
}

export async function consultarTiposCargaActividad(signal: AbortSignal): Promise<TipoCargaActividad[]> {
  const doc = await api<unknown>('/reportes/actividad/tipos-carga', { signal, cache: 'no-store' })
  if (!Array.isArray(doc)) throw new Error('El servidor devolvió un catálogo de tipos de carga no válido.')
  const vistos = new Set<number>()
  return doc.map((valor: unknown) => {
    const tipo = objeto(valor)
    if (!cantidad(tipo.id) || tipo.id === 0 || vistos.has(tipo.id)
        || typeof tipo.nombre !== 'string' || !tipo.nombre.trim())
      throw new Error('El servidor devolvió un catálogo de tipos de carga no válido.')
    vistos.add(tipo.id)
    return { id: tipo.id, nombre: tipo.nombre }
  })
}
