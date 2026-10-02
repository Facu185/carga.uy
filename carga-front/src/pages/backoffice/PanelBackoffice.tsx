import { IdCard, Users, type LucideIcon } from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { Etiqueta } from '../../componentes/Etiqueta'
import { formatearMomento, formatearUsuario, nombreAccion, nombreEntidad, nombreRol } from '../../formato'
import {
  ROLES,
  type Funcionario,
  type PaginaAuditoria,
  type RegistroAuditoria,
  type SolicitudFuncionario,
  type UsuarioInterno,
} from '../../tipos'
import { useTituloPagina } from '../../titulo'

/** Lo que muestra cada tarjeta. Se carga en paralelo; cada dato es opcional. */
interface Resumen {
  pendientes?: number
  funcionariosActivos?: number
  usuarios?: number
  bloqueados?: number
  /** Las últimas acciones de la auditoría (undefined si no se pudieron cargar). */
  actividad?: RegistroAuditoria[]
}

/** Si un pedido falla, esa tarjeta queda sin dato y las demás se ven igual. */
function intentar<T>(pedido: Promise<T>): Promise<T | undefined> {
  return pedido.catch(() => undefined)
}

function plural(n: number, singular: string, pluralTexto: string): string {
  return `${n} ${n === 1 ? singular : pluralTexto}`
}

/** cargando: mientras llegan los datos se ve "Cargando…", así la tarjeta no aparece vacía. */
function Tarjeta({
  icono: Icono,
  ruta,
  titulo,
  descripcion,
  cargando,
  children,
}: {
  icono: LucideIcon
  ruta: string
  titulo: string
  descripcion: string
  cargando: boolean
  children?: ReactNode
}) {
  return (
    <Link to={ruta} className="tarjeta-seccion">
      <span className="tarjeta-seccion-icono" aria-hidden="true">
        <Icono size={22} />
      </span>
      <h2>{titulo}</h2>
      <p>{descripcion}</p>
      <div className="tarjeta-seccion-dato" aria-busy={cargando}>
        {cargando ? <span className="dato-cargando">Cargando…</span> : children}
      </div>
    </Link>
  )
}

const ACCIONES_RECIENTES = 6

/** Lo último que pasó en el sistema: lo primero que quiere saber quien entra a administrar o fiscalizar. */
function ActividadReciente({ actividad, cargando }: { actividad?: RegistroAuditoria[]; cargando: boolean }) {
  return (
    <section className="actividad" aria-labelledby="titulo-actividad" aria-busy={cargando}>
      <div className="actividad-encabezado">
        <h2 id="titulo-actividad">Actividad reciente</h2>
        <Link to="/backoffice/auditoria">Ver la auditoría completa</Link>
      </div>
      {cargando && <p className="actividad-vacia">Cargando…</p>}
      {!cargando && actividad === undefined && <p className="actividad-vacia">No se pudo cargar la actividad.</p>}
      {!cargando && actividad?.length === 0 && <p className="actividad-vacia">Todavía no hay acciones registradas.</p>}
      {actividad && actividad.length > 0 && (
        <ol className="actividad-lista">
          {actividad.map((r) => (
            <li key={r.id}>
              <time dateTime={r.momento}>{formatearMomento(r.momento)}</time>
              <span className="actividad-accion">
                {nombreAccion(r.accion)}
                <span className="actividad-quien">
                  {formatearUsuario(r.usuario)}
                  {r.rol && `, ${nombreRol(r.rol)}`}
                </span>
              </span>
              <span className="actividad-sobre">
                {nombreEntidad(r.entidad)}
                <span className="actividad-quien">{formatearUsuario(r.entidadId)}</span>
              </span>
            </li>
          ))}
        </ol>
      )}
    </section>
  )
}

export function PanelBackoffice() {
  useTituloPagina('Inicio', true)
  const { sesion } = useAuth()
  const esAdmin = sesion?.rol === ROLES.ADMINISTRADOR
  const [resumen, setResumen] = useState<Resumen | null>(null)
  const cargando = resumen === null

  useEffect(() => {
    let vigente = true
    const soloAdmin = <T,>(ruta: string) => (esAdmin ? intentar(api<T>(ruta)) : Promise.resolve(undefined))
    Promise.all([
      intentar(api<PaginaAuditoria>(`/auditoria?tamano=${ACCIONES_RECIENTES}`)),
      soloAdmin<SolicitudFuncionario[]>('/solicitudes-funcionario?estado=PENDIENTE'),
      soloAdmin<Funcionario[]>('/funcionarios'),
      soloAdmin<UsuarioInterno[]>('/usuarios-internos'),
    ]).then(([auditoria, solicitudes, funcionarios, usuarios]) => {
      if (!vigente) return
      setResumen({
        actividad: auditoria?.registros,
        pendientes: solicitudes?.length,
        funcionariosActivos: funcionarios?.filter((f) => f.activo).length,
        usuarios: usuarios?.length,
        bloqueados: usuarios?.filter((u) => u.bloqueado).length,
      })
    })
    return () => {
      vigente = false
    }
  }, [esAdmin])

  const datos = resumen ?? {}
  const hayPendientes = (datos.pendientes ?? 0) > 0

  return (
    <>
      <div className="seccion-encabezado">
        <div>
          <h1>Hola, {sesion?.nombre}</h1>
          <p>{nombreRol(sesion?.rol)}</p>
        </div>
      </div>

      {esAdmin && hayPendientes && (
        <div className="llamado">
          <p>
            {datos.pendientes === 1
              ? 'Hay 1 solicitud de acceso esperando revisión.'
              : `Hay ${datos.pendientes} solicitudes de acceso esperando revisión.`}
          </p>
          <Link className="boton-enlace" to="/backoffice/funcionarios">
            Revisar solicitudes
          </Link>
        </div>
      )}

      {esAdmin && (
        <div className="grilla-secciones">
          <Tarjeta
            cargando={cargando}
            icono={IdCard}
            ruta="/backoffice/funcionarios"
            titulo="Funcionarios"
            descripcion="Solicitudes de acceso, altas, bajas y dependencias."
          >
            <span>
              {datos.funcionariosActivos === undefined
                ? 'Sin datos'
                : plural(datos.funcionariosActivos, 'habilitado', 'habilitados')}
            </span>
            {hayPendientes && (
              <Etiqueta tono="aviso">{plural(datos.pendientes ?? 0, 'pendiente', 'pendientes')}</Etiqueta>
            )}
          </Tarjeta>
          <Tarjeta
            cargando={cargando}
            icono={Users}
            ruta="/backoffice/usuarios"
            titulo="Usuarios internos"
            descripcion="Autoridades y administradores que usan este backoffice."
          >
            <span>{datos.usuarios === undefined ? 'Sin datos' : plural(datos.usuarios, 'usuario', 'usuarios')}</span>
            {(datos.bloqueados ?? 0) > 0 && (
              <Etiqueta tono="aviso">{plural(datos.bloqueados ?? 0, 'bloqueado', 'bloqueados')}</Etiqueta>
            )}
          </Tarjeta>
        </div>
      )}

      <ActividadReciente actividad={datos.actividad} cargando={cargando} />
    </>
  )
}
