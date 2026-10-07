import { UserPlus } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { useAviso } from '../../componentes/avisos'
import { useConfirmar } from '../../componentes/confirmaciones'
import { FilasCargando } from '../../componentes/FilasCargando'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { Etiqueta } from '../../componentes/Etiqueta'
import { nombreRol } from '../../formato'
import { ROLES, type NuevoUsuarioInterno, type PasswordTemporal, type UsuarioInterno } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const VACIO: NuevoUsuarioInterno = { usuario: '', nombre: '', password: '', rol: ROLES.AUTORIDAD }

function Estado({ u }: { u: UsuarioInterno }) {
  return (
    <div className="etiquetas">
      {u.activo ? <Etiqueta tono="ok">Activo</Etiqueta> : <Etiqueta tono="neutro">Dado de baja</Etiqueta>}
      {u.bloqueado && <Etiqueta tono="aviso">Bloqueado</Etiqueta>}
      {u.debeCambiarPassword && <Etiqueta tono="neutro">Contraseña temporal</Etiqueta>}
    </div>
  )
}

export function Usuarios() {
  useTituloPagina('Usuarios internos', true)
  const avisar = useAviso()
  const confirmar = useConfirmar()
  const [cargando, setCargando] = useState(true)
  const { sesion } = useAuth()
  const [usuarios, setUsuarios] = useState<UsuarioInterno[]>([])
  const [nuevo, setNuevo] = useState<NuevoUsuarioInterno>(VACIO)
  const [creando, setCreando] = useState(false)
  const [temporal, setTemporal] = useState<PasswordTemporal | null>(null)
  const [copiada, setCopiada] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let vigente = true
    api<UsuarioInterno[]>('/usuarios-internos')
      .then((lista) => vigente && setUsuarios(lista))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudieron cargar los usuarios'))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [version])

  async function ejecutar<T>(accion: () => Promise<T>, mensaje: string): Promise<T | null> {
    setError(null)
    try {
      const resultado = await accion()
      setVersion((v) => v + 1)
      avisar(mensaje)
      return resultado
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo completar la acción')
      return null
    }
  }

  async function crear(e: FormEvent) {
    e.preventDefault()
    const creado = await ejecutar(
      () => api<UsuarioInterno>('/usuarios-internos', { method: 'POST', body: JSON.stringify(nuevo) }),
      `Usuario ${nuevo.usuario.trim()} creado`,
    )
    if (creado) {
      setNuevo(VACIO)
      setCreando(false)
    }
  }

  async function cambiarEstado(u: UsuarioInterno) {
    if (
      u.activo &&
      !(await confirmar({
        titulo: `¿Dar de baja a ${u.nombre}?`,
        mensaje: 'No va a poder ingresar al backoffice y se cierran sus sesiones abiertas. Podés reactivarlo después.',
        accion: 'Dar de baja',
        peligro: true,
      }))
    ) {
      return
    }
    void ejecutar(
      () => api(`/usuarios-internos/${u.id}/activo`, { method: 'PUT', body: JSON.stringify(!u.activo) }),
      u.activo ? `Se dio de baja a ${u.nombre}` : `Se reactivó a ${u.nombre}`,
    )
  }

  async function cambiarRol(u: UsuarioInterno) {
    const rol = u.rol === ROLES.ADMINISTRADOR ? ROLES.AUTORIDAD : ROLES.ADMINISTRADOR
    const confirmado = await confirmar({
      titulo: `¿Pasar a ${u.nombre} a ${nombreRol(rol).toLowerCase()}?`,
      mensaje: 'Cambian las secciones que puede ver y se cierran sus sesiones abiertas.',
      accion: `Pasar a ${nombreRol(rol).toLowerCase()}`,
    })
    if (!confirmado) {
      return
    }
    void ejecutar(
      () => api(`/usuarios-internos/${u.id}/rol`, { method: 'PUT', body: JSON.stringify({ rol }) }),
      `${u.nombre} ahora es ${nombreRol(rol).toLowerCase()}`,
    )
  }

  async function resetearPassword(u: UsuarioInterno) {
    const confirmado = await confirmar({
      titulo: `¿Generar una contraseña temporal para ${u.nombre}?`,
      mensaje: 'Su contraseña actual deja de funcionar en el acto y va a tener que elegir una nueva al ingresar.',
      accion: 'Generar contraseña',
      peligro: true,
    })
    if (!confirmado) {
      return
    }
    const r = await ejecutar(
      () => api<PasswordTemporal>(`/usuarios-internos/${u.id}/password`, { method: 'POST' }),
      `Contraseña temporal generada para ${u.nombre}`,
    )
    if (r) {
      setTemporal(r)
      setCopiada(false)
    }
  }

  function desbloquear(u: UsuarioInterno) {
    void ejecutar(() => api(`/usuarios-internos/${u.id}/desbloqueo`, { method: 'POST' }), `Se desbloqueó a ${u.nombre}`)
  }

  async function copiar() {
    if (!temporal) return
    try {
      await navigator.clipboard.writeText(temporal.passwordTemporal)
      setCopiada(true)
    } catch {
      setError('No se pudo copiar: seleccioná la contraseña y copiala a mano')
    }
  }

  return (
    <>
      <EncabezadoSeccion titulo="Usuarios internos">
        {!creando && (
          <button type="button" className="con-icono" onClick={() => setCreando(true)}>
            <UserPlus size={18} aria-hidden="true" />
            Nuevo usuario
          </button>
        )}
      </EncabezadoSeccion>

      {temporal && (
        <section className="ficha ficha-ancha ficha-clave" aria-labelledby="titulo-temporal">
          <h2 id="titulo-temporal">Contraseña temporal de {temporal.usuario}</h2>
          <p className="ficha-texto">
            Pasásela por un medio seguro. Se muestra una sola vez y la persona la tiene que cambiar al ingresar.
          </p>
          <code className="clave-valor">{temporal.passwordTemporal}</code>
          <div className="acciones">
            <button type="button" onClick={() => void copiar()}>
              {copiada ? 'Copiada' : 'Copiar contraseña'}
            </button>
            <button type="button" className="boton-secundario" onClick={() => setTemporal(null)}>
              Listo
            </button>
          </div>
        </section>
      )}

      {error && (
        <p className="mensaje-error" role="alert">
          {error}
        </p>
      )}

      {creando && (
        <section className="ficha ficha-ancha" aria-labelledby="titulo-nuevo-usuario">
          <h2 id="titulo-nuevo-usuario">Nuevo usuario</h2>
          <form onSubmit={(e) => void crear(e)} className="formulario-grilla" autoComplete="off">
            <label className="campo">
              <span>Usuario</span>
              <input
                value={nuevo.usuario}
                onChange={(e) => setNuevo({ ...nuevo, usuario: e.target.value })}
                autoComplete="off"
                autoCapitalize="none"
                required
                autoFocus
              />
            </label>
            <label className="campo">
              <span>Nombre</span>
              <input value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} required />
            </label>
            <label className="campo">
              <span>Contraseña</span>
              <input
                type="password"
                value={nuevo.password}
                onChange={(e) => setNuevo({ ...nuevo, password: e.target.value })}
                minLength={12}
                autoComplete="new-password"
                aria-describedby="ayuda-password"
                required
              />
              <small id="ayuda-password">Mínimo 12 caracteres.</small>
            </label>
            <label className="campo">
              <span>Rol</span>
              <select
                value={nuevo.rol}
                onChange={(e) => setNuevo({ ...nuevo, rol: e.target.value as NuevoUsuarioInterno['rol'] })}
              >
                <option value={ROLES.AUTORIDAD}>Autoridad</option>
                <option value={ROLES.ADMINISTRADOR}>Administrador</option>
              </select>
            </label>
            <div className="formulario-botones">
              <button type="submit">Crear usuario</button>
              <button type="button" className="boton-secundario" onClick={() => setCreando(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </section>
      )}

      <div className="tabla-contenedor">
        <table className="tabla" aria-busy={cargando}>
          <thead>
            <tr>
              <th>Usuario</th>
              <th>Rol</th>
              <th>Estado</th>
              <th>
                <span className="solo-lectores">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {cargando && usuarios.length === 0 && <FilasCargando columnas={4} />}
            {usuarios.map((u) => {
              const soyYo = u.usuario === sesion?.usuario
              return (
                <tr key={u.id}>
                  <td data-etiqueta="Usuario">
                    <strong>{u.nombre}</strong>
                    <span className="celda-secundaria">
                      {u.usuario}
                      {soyYo && ' (vos)'}
                      {u.origen === 'LDAP' && ' · LDAP'}
                    </span>
                  </td>
                  <td data-etiqueta="Rol">{nombreRol(u.rol)}</td>
                  <td data-etiqueta="Estado">
                    <Estado u={u} />
                  </td>
                  <td data-etiqueta="" className="celda-acciones">
                    {soyYo ? (
                      <span className="celda-secundaria">
                        {u.origen === 'LDAP'
                          ? 'Tu contraseña la gestiona el directorio LDAP'
                          : 'Tu cuenta se gestiona desde “Cambiar contraseña”'}
                      </span>
                    ) : (
                      <div className="acciones">
                        {u.bloqueado && (
                          <button type="button" className="boton-chico" onClick={() => desbloquear(u)}>
                            Desbloquear
                          </button>
                        )}
                        {/* Con origen LDAP, el rol y la contraseña los define el directorio: no se gestionan acá */}
                        {u.activo && u.origen !== 'LDAP' && (
                          <button type="button" className="boton-chico boton-secundario" onClick={() => void cambiarRol(u)}>
                            {u.rol === ROLES.ADMINISTRADOR ? 'Pasar a autoridad' : 'Pasar a administrador'}
                          </button>
                        )}
                        {u.activo && u.origen !== 'LDAP' && (
                          <button
                            type="button"
                            className="boton-chico boton-secundario"
                            onClick={() => void resetearPassword(u)}
                          >
                            Nueva contraseña
                          </button>
                        )}
                        <button type="button" className="boton-chico boton-secundario" onClick={() => void cambiarEstado(u)}>
                          {u.activo ? 'Dar de baja' : 'Reactivar'}
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
    </>
  )
}
