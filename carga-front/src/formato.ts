/** 33333333 → 3.333.333-3 (solo para mostrar; el backend guarda solo dígitos). */
export function formatearCedula(cedula: string): string {
  if (!/^\d{7,8}$/.test(cedula)) {
    return cedula
  }
  const numero = cedula.slice(0, -1)
  const verificador = cedula.slice(-1)
  return `${Number(numero).toLocaleString('es-UY')}-${verificador}`
}

/** 1983-10-18 → 18/10/1983. Se arma a mano para que la zona horaria no corra el día. */
export function formatearFecha(iso: string | null | undefined): string {
  if (!iso) return '—'
  const [anio, mes, dia] = iso.slice(0, 10).split('-')
  return anio && mes && dia ? `${dia}/${mes}/${anio}` : iso
}

const NOMBRES_ROL: Record<string, string> = {
  ADMINISTRADOR: 'Administrador',
  AUTORIDAD: 'Autoridad',
  FUNCIONARIO: 'Funcionario del MTOP',
  CIUDADANO: 'Ciudadano',
}

export function nombreRol(rol: string | null | undefined): string {
  return rol ? (NOMBRES_ROL[rol] ?? rol) : ''
}

/** FUNCIONARIO_HABILITADO → Funcionario habilitado */
export function humanizar(codigo: string): string {
  const texto = codigo.toLowerCase().replaceAll('_', ' ')
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}

/** Fecha y hora local cortas: 25/09/2026 14:30 */
export function formatearMomento(iso: string): string {
  return new Date(iso).toLocaleString('es-UY', { dateStyle: 'short', timeStyle: 'short' })
}

const ACCIONES: Record<string, string> = {
  CASO_CREADO: 'Creó un caso de fiscalización',
  CASO_CAMBIO_ESTADO: 'Cambió el estado de un caso',
  CASO_RESUELTO: 'Resolvió un caso',
  USUARIO_INTERNO_CREADO: 'Creó un usuario interno',
  FUNCIONARIO_HABILITADO: 'Habilitó a un funcionario',
  FUNCIONARIO_ESTADO_CAMBIADO: 'Cambió el estado de un funcionario',
  FUNCIONARIO_DEPENDENCIA_CAMBIADA: 'Cambió la dependencia de un funcionario',
  SOLICITUD_FUNCIONARIO_CREADA: 'Pidió acceso como funcionario',
  SOLICITUD_FUNCIONARIO_APROBADA: 'Aprobó una solicitud de acceso',
  SOLICITUD_FUNCIONARIO_RECHAZADA: 'Rechazó una solicitud de acceso',
  INGRESO_EXITOSO: 'Ingresó al sistema',
  INGRESO_FALLIDO: 'Intento de ingreso fallido',
  CUENTA_BLOQUEADA: 'Cuenta bloqueada por intentos fallidos',
  USUARIO_INTERNO_ESTADO_CAMBIADO: 'Cambió el estado de un usuario interno',
  USUARIO_INTERNO_ROL_CAMBIADO: 'Cambió el rol de un usuario interno',
  USUARIO_INTERNO_DESBLOQUEADO: 'Desbloqueó un usuario interno',
  PASSWORD_RESETEADA: 'Generó una contraseña temporal',
  PASSWORD_CAMBIADA: 'Cambió su contraseña',
}

/** Entidades que aparecen en la auditoría: código que guarda el central → nombre para mostrar. */
export const ENTIDADES: Record<string, string> = {
  CasoFiscalizacion: 'Caso de fiscalización',
  CuentaInterna: 'Usuario interno',
  Funcionario: 'Funcionario',
  SolicitudFuncionario: 'Solicitud de acceso',
  Usuario: 'Persona (gub.uy)',
}

/** Acciones nuevas que todavía no tengan texto se muestran igual, a partir del código. */
export function nombreAccion(codigo: string): string {
  return ACCIONES[codigo] ?? humanizar(codigo)
}

export function nombreEntidad(codigo: string): string {
  return ENTIDADES[codigo] ?? codigo
}
