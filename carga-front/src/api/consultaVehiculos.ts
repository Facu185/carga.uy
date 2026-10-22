import { api } from './cliente'

export type EstadoPermiso = 'VIGENTE' | 'FUTURO' | 'VENCIDO' | 'SIN_DATOS'
export type EstadoItv = Exclude<EstadoPermiso, 'FUTURO'>

export interface ConsultaVehiculo {
  id: number
  empresaId: number
  matricula: string
  habilitado: boolean
  numeroPermiso: string | null
  permisoDesde: string | null
  permisoHasta: string | null
  itvVencimiento: string | null
  estadoPermiso: EstadoPermiso
  estadoItv: EstadoItv
}

export interface PaginaConsultaVehiculos {
  registros: ConsultaVehiculo[]
  pagina: number
  tamano: number
  total: number
  fechaReferencia: string
  zonaHoraria: 'America/Montevideo'
}

function objeto(valor: unknown): Record<string, unknown> {
  if (!valor || typeof valor !== 'object' || Array.isArray(valor)) throw new Error('Respuesta de consulta vehicular no válida.')
  return valor as Record<string, unknown>
}

function entero(valor: unknown): valor is number { return typeof valor === 'number' && Number.isSafeInteger(valor) }
function fecha(valor: unknown): valor is string { return typeof valor === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(valor) }
function textoOpcional(valor: unknown): valor is string | null | undefined { return valor == null || typeof valor === 'string' }
function fechaOpcional(valor: unknown): valor is string | null | undefined { return valor == null || fecha(valor) }
function permiso(valor: unknown): valor is EstadoPermiso {
  return valor === 'VIGENTE' || valor === 'FUTURO' || valor === 'VENCIDO' || valor === 'SIN_DATOS'
}
function itv(valor: unknown): valor is EstadoItv { return permiso(valor) && valor !== 'FUTURO' }

/** Solo lectura C13; la vigencia y su fecha de referencia pertenecen al servidor. */
export async function consultarVehiculos(empresaId: number, matricula: string, pagina: number, tamano: number,
  signal: AbortSignal): Promise<PaginaConsultaVehiculos> {
  const parametros = new URLSearchParams({ empresaId: String(empresaId), pagina: String(pagina), tamano: String(tamano) })
  if (matricula) parametros.set('matricula', matricula)
  const doc = objeto(await api<unknown>(`/vehiculos/consulta?${parametros}`, { signal }))
  if (doc.pagina !== pagina || doc.tamano !== tamano || !entero(doc.total) || doc.total < 0
      || !fecha(doc.fechaReferencia) || doc.zonaHoraria !== 'America/Montevideo'
      || !Array.isArray(doc.registros) || doc.registros.length > tamano) {
    throw new Error('No se pudo interpretar la página de vehículos.')
  }
  const registros = doc.registros.map((valor: unknown): ConsultaVehiculo => {
    const v = objeto(valor)
    if (!entero(v.id) || v.id <= 0 || v.empresaId !== empresaId || typeof v.matricula !== 'string'
        || typeof v.habilitado !== 'boolean' || !textoOpcional(v.numeroPermiso)
        || !fechaOpcional(v.permisoDesde) || !fechaOpcional(v.permisoHasta) || !fechaOpcional(v.itvVencimiento)
        || !permiso(v.estadoPermiso) || !itv(v.estadoItv)) {
      throw new Error('No se pudieron interpretar los datos de un vehículo.')
    }
    return { id: v.id, empresaId, matricula: v.matricula, habilitado: v.habilitado,
      numeroPermiso: v.numeroPermiso ?? null, permisoDesde: v.permisoDesde ?? null,
      permisoHasta: v.permisoHasta ?? null, itvVencimiento: v.itvVencimiento ?? null,
      estadoPermiso: v.estadoPermiso, estadoItv: v.estadoItv }
  })
  if (new Set(registros.map((v) => v.id)).size !== registros.length)
    throw new Error('La consulta contiene vehículos repetidos.')
  return { registros, pagina, tamano, total: doc.total, fechaReferencia: doc.fechaReferencia, zonaHoraria: doc.zonaHoraria }
}
