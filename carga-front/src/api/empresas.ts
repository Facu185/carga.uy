import { api } from './cliente'
import type { PerfilEmpresa } from '../tipos'

/** EmpresaDTO administrativo C12; no confundir sus escrituras con el perfil propio C10. */
export type EmpresaAdministrativa = PerfilEmpresa
export type DatosEmpresaAdministrativa = Pick<EmpresaAdministrativa,
  'nombre' | 'razonSocial' | 'numeroEmpresa' | 'direccion' | 'fechaAlta'>

export function interpretarEmpresa(valor: unknown): EmpresaAdministrativa {
  if (!valor || typeof valor !== 'object') throw new Error('Respuesta de empresa no válida.')
  const e = valor as Record<string, unknown>
  if (typeof e.id !== 'number' || !Number.isSafeInteger(e.id) || e.id <= 0
      || typeof e.nombre !== 'string' || typeof e.activa !== 'boolean'
      || typeof e.numeroEmpresa !== 'number' || !Number.isInteger(e.numeroEmpresa)
      || (e.razonSocial != null && typeof e.razonSocial !== 'string')
      || (e.direccion != null && typeof e.direccion !== 'string')
      || (e.fechaAlta != null && (typeof e.fechaAlta !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(e.fechaAlta)))) {
    throw new Error('No se pudieron interpretar los datos administrativos de la empresa.')
  }
  return {
    id: e.id, nombre: e.nombre, activa: e.activa, numeroEmpresa: e.numeroEmpresa,
    razonSocial: typeof e.razonSocial === 'string' ? e.razonSocial : null,
    direccion: typeof e.direccion === 'string' ? e.direccion : null,
    fechaAlta: typeof e.fechaAlta === 'string' ? e.fechaAlta : null,
  }
}

/** C12 no publica GET individual ni paginación; ambos consumidores usan el listado existente. */
export async function consultarEmpresas(nombre: string, signal: AbortSignal): Promise<EmpresaAdministrativa[]> {
  const query = nombre ? `?${new URLSearchParams({ nombre })}` : ''
  const respuesta = await api<unknown>(`/empresas${query}`, { signal })
  if (!Array.isArray(respuesta)) throw new Error('No se pudo interpretar el listado de empresas.')
  const empresas = respuesta.map(interpretarEmpresa)
  if (new Set(empresas.map((empresa) => empresa.id)).size !== empresas.length)
    throw new Error('El listado contiene identificadores de empresa repetidos.')
  return empresas
}
