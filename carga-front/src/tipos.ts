export const ROLES = {
  ADMINISTRADOR: 'ADMINISTRADOR',
  AUTORIDAD: 'AUTORIDAD',
  FUNCIONARIO: 'FUNCIONARIO',
  CIUDADANO: 'CIUDADANO',
} as const

export type Rol = (typeof ROLES)[keyof typeof ROLES]

export interface Sesion {
  token: string
  expiraEn: string
  usuario: string
  nombre: string
  rol: Rol
  /** La contraseña es temporal: hay que cambiarla antes de usar el backoffice. */
  debeCambiarPassword?: boolean
  /** BASE_DE_DATOS o LDAP (usuarios internos), GUBUY (ciudadanos y funcionarios) */
  origen?: 'BASE_DE_DATOS' | 'LDAP' | 'GUBUY'
}

export interface UsuarioInterno {
  id: number
  usuario: string
  nombre: string
  rol: Rol
  activo: boolean
  bloqueado: boolean
  debeCambiarPassword: boolean
  /** De dónde viene la contraseña. Con LDAP no se puede resetear ni cambiar el rol desde acá. */
  origen: 'BASE_DE_DATOS' | 'LDAP'
}

export interface PasswordTemporal {
  usuario: string
  passwordTemporal: string
}

export interface NuevoUsuarioInterno {
  usuario: string
  nombre: string
  password: string
  rol: Rol
}

export interface Funcionario {
  id: number
  cedula: string
  nombre: string
  email: string | null
  dependencia: string | null
  activo: boolean
}

export interface Perfil {
  cedula: string
  nombre: string | null
  email: string | null
  fechaNacimiento: string | null
  mayorDeEdad: boolean | null
  verificacionDnic: 'VERIFICADO' | 'PENDIENTE'
}

export interface RegistroAuditoria {
  id: number
  momento: string
  usuario: string
  rol: string | null
  accion: string
  entidad: string
  entidadId: string
  fundamento: string | null
  detalle: string | null
}

/** Una página de la auditoría: pagina empieza en 0 y total es la cantidad de registros del filtro. */
export interface PaginaAuditoria {
  registros: RegistroAuditoria[]
  pagina: number
  tamano: number
  total: number
}

export type EstadoSolicitud = 'PENDIENTE' | 'APROBADA' | 'RECHAZADA'

export interface SolicitudFuncionario {
  id: number
  cedula: string
  nombre: string | null
  email: string | null
  dependencia: string
  estado: EstadoSolicitud
  creadaEn: string
  resueltaEn: string | null
  resueltaPor: string | null
  motivoRechazo: string | null
}
