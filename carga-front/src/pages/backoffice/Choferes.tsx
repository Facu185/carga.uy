import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { useAviso } from '../../componentes/avisos'
import { useConfirmar } from '../../componentes/confirmaciones'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FilasCargando } from '../../componentes/FilasCargando'
import { formatearCedula } from '../../formato'
import type { Chofer, Empresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const VACIO = { empresaId: '', cedula: '', nombre: '' }

/** Administración de choferes; api() agrega el Bearer de la sesión del backoffice. */
export function Choferes() {
  useTituloPagina('Choferes', true)
  const avisar = useAviso()
  const confirmar = useConfirmar()
  const [choferes, setChoferes] = useState<Chofer[]>([])
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [nuevo, setNuevo] = useState(VACIO)
  const [editando, setEditando] = useState<number | null>(null)
  const [edicion, setEdicion] = useState({ cedula: '', nombre: '' })

  useEffect(() => {
    let vigente = true
    setError(null)
    Promise.all([api<Chofer[]>('/choferes'), api<Empresa[]>('/empresas')])
      .then(([listaChoferes, listaEmpresas]) => {
        if (!vigente) return
        setChoferes(listaChoferes)
        // El servicio no permite asignar un chofer a una empresa inactiva.
        setEmpresas(listaEmpresas.filter((empresa) => empresa.activa))
      })
      .catch((err: unknown) => {
        if (vigente) setError(err instanceof Error ? err.message : 'No se pudieron cargar los choferes')
      })
      .finally(() => {
        if (vigente) setCargando(false)
      })
    return () => {
      vigente = false
    }
  }, [version])

  function actualizar(): void {
    setVersion((valor) => valor + 1)
  }

  async function crear(evento: FormEvent) {
    evento.preventDefault()
    setError(null)
    try {
      await api<Chofer>('/choferes', {
        method: 'POST',
        body: JSON.stringify({ ...nuevo, empresaId: Number(nuevo.empresaId) }),
      })
      setNuevo(VACIO)
      avisar('Chofer registrado')
      actualizar()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo registrar el chofer')
    }
  }

  function comenzarEdicion(chofer: Chofer) {
    setEditando(chofer.id)
    setEdicion({ cedula: chofer.cedula, nombre: chofer.nombre })
  }

  async function guardar(chofer: Chofer) {
    setError(null)
    try {
      await api<Chofer>(`/choferes/${chofer.id}`, {
        method: 'PUT',
        // El recurso recibe el DTO completo; empresa y estado no se modifican desde esta fila.
        body: JSON.stringify({ ...chofer, ...edicion }),
      })
      setEditando(null)
      avisar('Chofer actualizado')
      actualizar()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo actualizar el chofer')
    }
  }

  async function cambiarEstado(chofer: Chofer) {
    if (
      chofer.activo &&
      !(await confirmar({
        titulo: `¿Dar de baja a ${chofer.nombre}?`,
        mensaje: 'Dejará de estar disponible para nuevas guías. Podés reactivarlo después.',
        accion: 'Dar de baja',
        peligro: true,
      }))
    ) {
      return
    }

    setError(null)
    try {
      if (chofer.activo) {
        await api<void>(`/choferes/${chofer.id}`, { method: 'DELETE' })
      } else {
        await api<void>(`/choferes/${chofer.id}/activo`, { method: 'PUT', body: JSON.stringify(true) })
      }
      avisar(chofer.activo ? 'Chofer dado de baja' : 'Chofer reactivado')
      actualizar()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo cambiar el estado del chofer')
    }
  }

  function nombreEmpresa(empresaId: number): string {
    return empresas.find((empresa) => empresa.id === empresaId)?.nombre ?? `Empresa #${empresaId}`
  }

  return (
    <>
      <EncabezadoSeccion titulo="Choferes" />

      {error && <p className="mensaje-error" role="alert">{error}</p>}

      <section className="ficha ficha-ancha" aria-labelledby="titulo-nuevo-chofer">
        <h2 id="titulo-nuevo-chofer">Nuevo chofer</h2>
        <form onSubmit={(evento) => void crear(evento)} className="formulario-grilla" autoComplete="off">
          <label className="campo">
            <span>Empresa</span>
            <select
              value={nuevo.empresaId}
              onChange={(evento) => setNuevo({ ...nuevo, empresaId: evento.target.value })}
              required
            >
              <option value="">Seleccione una empresa</option>
              {empresas.map((empresa) => <option key={empresa.id} value={empresa.id}>{empresa.nombre}</option>)}
            </select>
          </label>
          <label className="campo">
            <span>Cédula</span>
            <input value={nuevo.cedula} onChange={(evento) => setNuevo({ ...nuevo, cedula: evento.target.value })} required />
          </label>
          <label className="campo">
            <span>Nombre</span>
            <input value={nuevo.nombre} onChange={(evento) => setNuevo({ ...nuevo, nombre: evento.target.value })} required />
          </label>
          <div className="formulario-botones">
            <button type="submit" disabled={empresas.length === 0}>Agregar chofer</button>
            {empresas.length === 0 && <p className="formulario-nota">No hay empresas activas disponibles para asignar.</p>}
          </div>
        </form>
      </section>

      <div className="tabla-contenedor">
        <table className="tabla" aria-busy={cargando}>
          <thead>
            <tr><th>Chofer</th><th>Empresa</th><th>Estado</th><th><span className="solo-lectores">Acciones</span></th></tr>
          </thead>
          <tbody>
            {cargando && choferes.length === 0 && <FilasCargando columnas={4} />}
            {choferes.map((chofer) => {
              const enEdicion = editando === chofer.id
              return (
                <tr key={chofer.id}>
                  <td data-etiqueta="Chofer">
                    {enEdicion ? (
                      <div className="edicion-en-linea">
                        <input aria-label={`Cédula de ${chofer.nombre}`} value={edicion.cedula} onChange={(evento) => setEdicion({ ...edicion, cedula: evento.target.value })} />
                        <input aria-label={`Nombre de ${chofer.nombre}`} value={edicion.nombre} onChange={(evento) => setEdicion({ ...edicion, nombre: evento.target.value })} />
                      </div>
                    ) : (
                      <><strong>{chofer.nombre}</strong><span className="celda-secundaria">{formatearCedula(chofer.cedula)}</span></>
                    )}
                  </td>
                  <td data-etiqueta="Empresa">{nombreEmpresa(chofer.empresaId)}</td>
                  <td data-etiqueta="Estado">{chofer.activo ? <Etiqueta tono="ok">Activo</Etiqueta> : <Etiqueta tono="neutro">Dado de baja</Etiqueta>}</td>
                  <td data-etiqueta="" className="celda-acciones">
                    {enEdicion ? (
                      <div className="acciones">
                        <button type="button" className="boton-chico" onClick={() => void guardar(chofer)} disabled={!edicion.cedula.trim() || !edicion.nombre.trim()}>Guardar</button>
                        <button type="button" className="boton-chico boton-secundario" onClick={() => setEditando(null)}>Cancelar</button>
                      </div>
                    ) : (
                      <div className="acciones">
                        <button type="button" className="boton-chico boton-secundario" onClick={() => comenzarEdicion(chofer)}>Editar</button>
                        <button type="button" className="boton-chico boton-secundario" onClick={() => void cambiarEstado(chofer)}>
                          {chofer.activo ? 'Dar de baja' : 'Reactivar'}
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              )
            })}
            {!cargando && choferes.length === 0 && <tr><td colSpan={4} className="tabla-vacia">Todavía no hay choferes registrados.</td></tr>}
          </tbody>
        </table>
      </div>
    </>
  )
}
