import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { useAviso } from '../../componentes/avisos'
import { useConfirmar } from '../../componentes/confirmaciones'
import { FilasCargando } from '../../componentes/FilasCargando'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { Etiqueta } from '../../componentes/Etiqueta'
import { SelectorDependencia } from '../../componentes/SelectorDependencia'
import { formatearCedula } from '../../formato'
import type { Funcionario } from '../../tipos'
import { SolicitudesPendientes } from './SolicitudesPendientes'
import { useTituloPagina } from '../../titulo'

const VACIO = { cedula: '', nombre: '', dependencia: '' }

/**
 * Nombre y correo vienen de gub.uy (se actualizan en cada ingreso) y no se editan acá.
 * El administrador gestiona el acceso y la dependencia.
 */
export function Funcionarios() {
  useTituloPagina('Funcionarios', true)
  const avisar = useAviso()
  const confirmar = useConfirmar()
  const [cargando, setCargando] = useState(true)
  const [funcionarios, setFuncionarios] = useState<Funcionario[]>([])
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [editando, setEditando] = useState<number | null>(null)
  const [dependenciaEditada, setDependenciaEditada] = useState('')
  const [altaManual, setAltaManual] = useState(false)
  const [nuevo, setNuevo] = useState(VACIO)

  useEffect(() => {
    let vigente = true
    api<Funcionario[]>('/funcionarios')
      .then((lista) => vigente && setFuncionarios(lista))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudieron cargar los funcionarios'))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [version])

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string): Promise<boolean> {
    setError(null)
    try {
      await accion()
      setVersion((v) => v + 1)
      avisar(mensaje)
      return true
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo completar la acción')
      return false
    }
  }

  async function crear(e: FormEvent) {
    e.preventDefault()
    const ok = await ejecutar(
      () => api('/funcionarios', { method: 'POST', body: JSON.stringify(nuevo) }),
      `Se habilitó a ${nuevo.nombre.trim()} como funcionario`,
    )
    if (ok) {
      setNuevo(VACIO)
      setAltaManual(false)
    }
  }

  async function cambiarEstado(f: Funcionario) {
    if (
      f.activo &&
      !(await confirmar({
        titulo: `¿Dar de baja a ${f.nombre}?`,
        mensaje: 'Deja de tener acceso como funcionario en el acto, aunque tenga una sesión abierta. Podés reactivarlo después.',
        accion: 'Dar de baja',
        peligro: true,
      }))
    ) {
      return
    }
    void ejecutar(
      () => api(`/funcionarios/${f.id}/activo`, { method: 'PUT', body: JSON.stringify(!f.activo) }),
      f.activo ? `Se dio de baja a ${f.nombre}` : `Se reactivó a ${f.nombre}`,
    )
  }

  async function guardarDependencia(f: Funcionario) {
    const ok = await ejecutar(
      () =>
        api(`/funcionarios/${f.id}/dependencia`, {
          method: 'PUT',
          body: JSON.stringify({ dependencia: dependenciaEditada }),
        }),
      'Dependencia actualizada',
    )
    if (ok) setEditando(null)
  }

  return (
    <>
      <EncabezadoSeccion titulo="Funcionarios" />

      <SolicitudesPendientes alResolver={() => setVersion((v) => v + 1)} />

      {error && (
        <p className="mensaje-error" role="alert">
          {error}
        </p>
      )}

      <div className="tabla-contenedor">
        <table className="tabla" aria-busy={cargando}>
          <thead>
            <tr>
              <th>Funcionario</th>
              <th>Dependencia</th>
              <th>Estado</th>
              <th>
                <span className="solo-lectores">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {cargando && funcionarios.length === 0 && <FilasCargando columnas={4} />}
            {funcionarios.map((f) => (
              <tr key={f.id}>
                <td data-etiqueta="Funcionario">
                  <strong>{f.nombre}</strong>
                  <span className="celda-secundaria">{formatearCedula(f.cedula)}</span>
                  {f.email && <span className="celda-secundaria">{f.email}</span>}
                </td>
                <td data-etiqueta="Dependencia">
                  {editando === f.id ? (
                    <div className="edicion-en-linea">
                      <SelectorDependencia
                        valor={dependenciaEditada}
                        onChange={setDependenciaEditada}
                        etiqueta={`Dependencia de ${f.nombre}`}
                      />
                      <button
                        type="button"
                        className="boton-chico"
                        onClick={() => void guardarDependencia(f)}
                        disabled={!dependenciaEditada.trim()}
                      >
                        Guardar
                      </button>
                      <button type="button" className="boton-chico boton-secundario" onClick={() => setEditando(null)}>
                        Cancelar
                      </button>
                    </div>
                  ) : (
                    <div className="edicion-en-linea">
                      <span>{f.dependencia ?? <span className="celda-vacia">Sin indicar</span>}</span>
                      <button
                        type="button"
                        className="boton-texto"
                        onClick={() => {
                          setEditando(f.id)
                          setDependenciaEditada(f.dependencia ?? '')
                        }}
                        aria-label={`Editar dependencia de ${f.nombre}`}
                      >
                        Editar
                      </button>
                    </div>
                  )}
                </td>
                <td data-etiqueta="Estado">{f.activo ? <Etiqueta tono="ok">Activo</Etiqueta> : <Etiqueta tono="neutro">Dado de baja</Etiqueta>}</td>
                <td data-etiqueta="" className="celda-acciones">
                  <button type="button" className="boton-chico boton-secundario" onClick={() => void cambiarEstado(f)}>
                    {f.activo ? 'Dar de baja' : 'Reactivar'}
                  </button>
                </td>
              </tr>
            ))}
            {!cargando && funcionarios.length === 0 && (
              <tr>
                <td colSpan={4} className="tabla-vacia">
                  Todavía no hay funcionarios habilitados. Aparecen acá cuando aprobás una solicitud de acceso.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {altaManual ? (
        <section className="ficha ficha-ancha" aria-labelledby="titulo-alta-manual">
          <h2 id="titulo-alta-manual">Alta manual</h2>
          <p className="ficha-texto">Solo si la persona no puede pedir acceso por sí misma desde gub.uy.</p>
          <form onSubmit={(e) => void crear(e)} className="formulario-grilla" autoComplete="off">
            <label className="campo">
              <span>Cédula</span>
              <input
                value={nuevo.cedula}
                onChange={(e) => setNuevo({ ...nuevo, cedula: e.target.value })}
                placeholder="1.234.567-2"
                required
                autoFocus
              />
            </label>
            <label className="campo">
              <span>Nombre</span>
              <input value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} required />
            </label>
            <SelectorDependencia
              valor={nuevo.dependencia}
              onChange={(dependencia) => setNuevo({ ...nuevo, dependencia })}
            />
            <div className="formulario-botones">
              <button type="submit">Habilitar funcionario</button>
              <button type="button" className="boton-secundario" onClick={() => setAltaManual(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </section>
      ) : (
        <p className="nota-seccion">
          ¿La persona no puede pedir acceso desde gub.uy?{' '}
          <button type="button" className="boton-texto" onClick={() => setAltaManual(true)}>
            Hacer un alta manual
          </button>
        </p>
      )}
    </>
  )
}
