export const ROLES = {
  ADMINISTRADOR: 'ADMINISTRADOR',
  AUTORIDAD: 'AUTORIDAD',
  FUNCIONARIO: 'FUNCIONARIO',
  CIUDADANO: 'CIUDADANO',
  CHOFER: 'CHOFER',
} as const

export type Rol = (typeof ROLES)[keyof typeof ROLES]

export interface Sesion {
  token: string
  expiraEn: string
  usuario: string
  nombre: string
  rol: Rol
  /** Todos los roles autorizados en el JWT; rol permanece como rol principal para compatibilidad. */
  roles?: Rol[]
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

/** Nodo periférico registrado, con su estado verificado en el momento (P4). */
export interface NodoPeriferico {
  id: number
  nombre: string
  tipo: 'BALANZAS' | 'TRACKING'
  /** Solo los nodos Balanzas: el nodo Tracking llama al central. */
  url: string | null
  activo: boolean
  actualizadoEn: string | null
  /** null si está desactivado: no se verifica. */
  enLinea: boolean | null
  detalle: string | null
}

export interface Funcionario {
  id: number
  cedula: string
  nombre: string
  email: string | null
  dependencia: string | null
  activo: boolean
}

/** Chofer habilitado para conducir vehículos de una empresa transportista. */
export interface Chofer {
  id: number
  empresaId: number
  cedula: string
  nombre: string
  activo: boolean
}

/** Empresa disponible para asignar al registrar o modificar un chofer. */
export interface Empresa {
  id: number
  nombre: string
  activa: boolean
}

/** Vínculo auditable de la persona con una empresa. RESPONSABLE activo habilita su gestión. */
export interface VinculoEmpresa {
  id: number
  empresaId: number
  nombreEmpresa: string
  tipo: 'RESPONSABLE' | 'OPERADOR'
  estado: 'PENDIENTE' | 'ACTIVO' | 'RECHAZADO' | 'REVOCADO'
  solicitadoEn: string
  cedulaSolicitante?: string
  nombreSolicitante?: string
}

export interface Perfil {
  cedula: string
  nombre: string | null
  email: string | null
  fechaNacimiento: string | null
  mayorDeEdad: boolean | null
  verificacionDnic: 'VERIFICADO' | 'PENDIENTE'
}

/** Datos requeridos por P2 para registrar una guía de carga. */
export interface CrearGuia {
  vehiculoId: number
  choferId: number
  rubroId: number
  tipoCargaId?: number
  pesoKg: number
  volumenM3: number
  inicio: string
  fin: string
  origen: string
  origenLat?: number
  origenLon?: number
  destino: string
  destinoLat?: number
  destinoLon?: number
}

/** Guía devuelta por P2 al registrarla o al consultar las asignadas a un chofer. */
export interface Guia {
  id: number
  empresaId: number
  vehiculoId: number
  choferId: number
  rubroId: number | null
  tipoCargaId: number | null
  pesoKg: number
  volumenM3: number
  inicio: string
  fin: string
  origen: string
  origenLat: number | null
  origenLon: number | null
  destino: string
  destinoLat: number | null
  destinoLon: number | null
  estado: string
  creadaEn: string
}

export interface VehiculoCatalogo { id: number; matricula: string; marca: string | null; modelo: string | null; capacidadCargaKg: number; capacidadVolumenM3: number | null }
export interface ChoferCatalogo { id: number; cedula: string; nombre: string }
export interface OpcionCatalogo { id: number; nombre: string }

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
