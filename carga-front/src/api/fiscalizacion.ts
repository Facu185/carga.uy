import type { TonoEtiqueta } from '../componentes/Etiqueta'
import { api } from './cliente'

// Fiscalización (P5, ADR-0022): bandeja del funcionario, infracciones de la empresa, parámetros e indicadores.

export type TipoInfraccion = 'EXCESO_CARGA' | 'SIN_HABILITACION' | 'VIAJE_SIN_GUIA' | 'DESVIO_RUTA'
export type EstadoCaso = 'DETECTADA' | 'NOTIFICADA' | 'EN_DESCARGO' | 'CONFIRMADA' | 'DESESTIMADA'

export interface Descargo {
  fundamento: string
  presentadoPor?: string | null
  presentadoEn: string
}

/** JSON-B omite los campos en null: los opcionales pueden no venir. */
export interface Caso {
  id: number
  tipo: TipoInfraccion
  estado: EstadoCaso
  empresaId?: number
  empresaNombre?: string
  matricula?: string
  guiaId?: number
  evidencia?: string
  detectadaEn: string
  notificadaEn?: string
  venceDescargo?: string
  plazoVencido: boolean
  admiteDescargo: boolean
  resueltaEn?: string
  resueltaPor?: string
  fundamento?: string
  descargo?: Descargo
}

export interface PaginaCasos {
  registros: Caso[]
  total: number
  pagina: number
  tamano: number
}

export interface ParametroRegla {
  clave: string
  valor: string
  descripcion?: string
}

export interface ConteoInfracciones {
  tipo: TipoInfraccion
  detectados: number
  confirmados: number
  desestimados: number
}

export interface ReporteFiscalizacion {
  desde: string
  hasta: string
  detectados: number
  confirmados: number
  desestimados: number
  pendientes: number
  tasaConfirmacion?: number | null
  porTipo: ConteoInfracciones[]
}

export const TIPOS: Record<TipoInfraccion, string> = {
  EXCESO_CARGA: 'Exceso de carga',
  SIN_HABILITACION: 'Sin habilitación vigente',
  VIAJE_SIN_GUIA: 'Viaje sin guía',
  DESVIO_RUTA: 'Desvío de ruta',
}

export const ESTADOS: Record<EstadoCaso, [string, TonoEtiqueta]> = {
  DETECTADA: ['Detectada', 'aviso'],
  NOTIFICADA: ['Notificada', 'aviso'],
  EN_DESCARGO: ['En descargo', 'neutro'],
  CONFIRMADA: ['Confirmada', 'error'],
  DESESTIMADA: ['Desestimada', 'ok'],
}

export function bandeja(filtros: { estado?: string; tipo?: string; pagina: number }, signal?: AbortSignal): Promise<PaginaCasos> {
  const p = new URLSearchParams({ pagina: String(filtros.pagina), tamano: '20' })
  if (filtros.estado) p.set('estado', filtros.estado)
  if (filtros.tipo) p.set('tipo', filtros.tipo)
  return api<PaginaCasos>(`/fiscalizacion/casos?${p}`, { signal })
}

export function caso(id: string, signal?: AbortSignal): Promise<Caso> {
  return api<Caso>(`/fiscalizacion/casos/${encodeURIComponent(id)}`, { signal })
}

export function resolver(id: number, confirmar: boolean, fundamento: string): Promise<Caso> {
  return api<Caso>(`/fiscalizacion/casos/${id}/resolucion`, { method: 'POST', body: JSON.stringify({ confirmar, fundamento }) })
}

export function infracciones(empresaId: string, pagina: number, signal?: AbortSignal): Promise<PaginaCasos> {
  return api<PaginaCasos>(`/infracciones?${new URLSearchParams({ empresaId, pagina: String(pagina), tamano: '20' })}`, { signal })
}

export function presentarDescargo(empresaId: string, casoId: number, fundamento: string): Promise<Caso> {
  return api<Caso>(`/infracciones/${casoId}/descargo?${new URLSearchParams({ empresaId })}`,
    { method: 'POST', body: JSON.stringify({ fundamento }) })
}

export function parametros(): Promise<ParametroRegla[]> {
  return api<ParametroRegla[]>('/parametros-reglas')
}

export function modificarParametro(clave: string, valor: string, motivo: string): Promise<ParametroRegla> {
  return api<ParametroRegla>(`/parametros-reglas/${encodeURIComponent(clave)}`, { method: 'PUT', body: JSON.stringify({ valor, motivo }) })
}

export function reporteFiscalizacion(desde: string, hasta: string, signal?: AbortSignal): Promise<ReporteFiscalizacion> {
  return api<ReporteFiscalizacion>(`/fiscalizacion/reporte?${new URLSearchParams({ desde, hasta })}`, { signal })
}

/** Archivo CSV (separado por ";" y con BOM, para que Excel en español lo abra bien). */
export function descargarCsv(nombre: string, filas: (string | number)[][]) {
  const texto = filas.map((fila) => fila.map((celda) => {
    const valor = String(celda)
    return /[";\n]/.test(valor) ? `"${valor.replace(/"/g, '""')}"` : valor
  }).join(';')).join('\r\n')
  const enlace = document.createElement('a')
  enlace.href = URL.createObjectURL(new Blob(['﻿' + texto], { type: 'text/csv;charset=utf-8' }))
  enlace.download = nombre
  enlace.click()
  URL.revokeObjectURL(enlace.href)
}
