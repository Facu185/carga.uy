import { api } from './cliente'

// Frontoffice público (ADR-0021): se usa sin sesión.

export type EstadoVigencia = 'VIGENTE' | 'FUTURO' | 'VENCIDO' | 'SIN_DATOS'

export interface ValidacionVehiculo {
  matricula: string
  encontrado: boolean
  vigente: boolean
  vehiculoHabilitado: boolean
  empresaHabilitada: boolean
  estadoPermiso: EstadoVigencia | null
  permisoHasta: string | null
  estadoItv: Exclude<EstadoVigencia, 'FUTURO'> | null
  itvVencimiento: string | null
  fechaReferencia: string
}

export interface ValidacionEmpresa {
  numeroEmpresa: number
  encontrada: boolean
  nombre: string | null
  habilitada: boolean
  fechaReferencia: string
}

/** guias y toneladas no vienen (o vienen en null) cuando la cifra está reservada: JSON-B omite los null. */
export interface CifraPublica {
  clave: string
  etiqueta: string
  reservada: boolean
  guias?: number | null
  toneladas?: number | null
}

export interface EstadisticasPublicas {
  desde: string
  hasta: string
  umbralGuias: number
  umbralEmpresas: number
  empresasHabilitadas?: number | null
  vehiculosHabilitados?: number | null
  vehiculosAlDia?: number | null
  porMes: CifraPublica[]
  porTipoCarga: CifraPublica[]
  porRubro: CifraPublica[]
  generadoEn: string
}

export function validarVehiculo(matricula: string, signal?: AbortSignal): Promise<ValidacionVehiculo> {
  return api<ValidacionVehiculo>(`/publico/permisos/vehiculo?${new URLSearchParams({ matricula })}`, { signal })
}

export function validarEmpresa(numero: string, signal?: AbortSignal): Promise<ValidacionEmpresa> {
  return api<ValidacionEmpresa>(`/publico/permisos/empresa?${new URLSearchParams({ numero })}`, { signal })
}

export function consultarEstadisticas(signal?: AbortSignal): Promise<EstadisticasPublicas> {
  return api<EstadisticasPublicas>('/publico/estadisticas', { signal })
}
