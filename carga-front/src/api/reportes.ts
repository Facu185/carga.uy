import { api } from './cliente'
import { ESTADOS_GUIA } from './consultaGuias'

export interface ConteoActividad {
  clave: string
  etiqueta: string
  cantidad: number
}

export interface ReporteActividad {
  desde: string
  hasta: string
  zonaHoraria: 'America/Montevideo'
  criterioTemporal: 'CREACION_GUIA'
  totalGuias: number
  empresasConGuias: number
  porDia: ConteoActividad[]
  porEstado: ConteoActividad[]
  porRubro: ConteoActividad[]
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
function interpretarReporte(valor: unknown, desde: string | null, hasta: string | null): ReporteActividad {
  const doc = objeto(valor)
  if (!fecha(doc.desde) || !fecha(doc.hasta) || (desde !== null && doc.desde !== desde)
      || (hasta !== null && doc.hasta !== hasta) || doc.zonaHoraria !== 'America/Montevideo'
      || doc.criterioTemporal !== 'CREACION_GUIA' || !cantidad(doc.totalGuias) || !cantidad(doc.empresasConGuias)
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
  return { desde: doc.desde, hasta: doc.hasta, zonaHoraria: doc.zonaHoraria, criterioTemporal: doc.criterioTemporal,
    totalGuias: doc.totalGuias, empresasConGuias: doc.empresasConGuias, porDia, porEstado, porRubro }
}

export async function consultarActividad(desde: string | null, hasta: string | null, signal: AbortSignal): Promise<ReporteActividad> {
  const parametros = new URLSearchParams()
  if (desde !== null) parametros.set('desde', desde)
  if (hasta !== null) parametros.set('hasta', hasta)
  const doc = await api<unknown>(`/reportes/actividad?${parametros}`, { signal, cache: 'no-store' })
  return interpretarReporte(doc, desde, hasta)
}
