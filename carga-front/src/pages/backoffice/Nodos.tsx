import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { useAviso } from '../../componentes/avisos'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FilasCargando } from '../../componentes/FilasCargando'
import { formatearMomento } from '../../formato'
import type { NodoPeriferico } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const VACIO = { nombre: '', url: '' }

/**
 * Gestionar nodos periféricos (Administrador, frente P4, ADR-0009).
 * La URL del nodo Balanzas activo es la que consulta el central, y desactivarlo corta las consultas en
 * el acto. El nodo Tracking se muestra con su estado, pero se habilita con su token (ADR-0007).
 */
export function Nodos() {
  useTituloPagina('Nodos periféricos', true)
  const avisar = useAviso()
  const [cargando, setCargando] = useState(true)
  const [nodos, setNodos] = useState<NodoPeriferico[]>([])
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [editando, setEditando] = useState<number | null>(null)
  const [urlEditada, setUrlEditada] = useState('')
  const [desactivando, setDesactivando] = useState<number | null>(null)
  const [fundamento, setFundamento] = useState('')
  const [alta, setAlta] = useState(false)
  const [nuevo, setNuevo] = useState(VACIO)

  useEffect(() => {
    let vigente = true
    api<NodoPeriferico[]>('/nodos')
      .then((lista) => vigente && setNodos(lista))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudieron cargar los nodos'))
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
      () => api('/nodos', { method: 'POST', body: JSON.stringify({ ...nuevo, tipo: 'BALANZAS' }) }),
      `Se dio de alta el nodo ${nuevo.nombre.trim()}. Queda desactivado hasta que lo actives.`,
    )
    if (ok) {
      setNuevo(VACIO)
      setAlta(false)
    }
  }

  async function guardarUrl(n: NodoPeriferico) {
    const ok = await ejecutar(
      () => api(`/nodos/${n.id}`, { method: 'PUT', body: JSON.stringify({ url: urlEditada }) }),
      n.activo ? 'URL actualizada: el central ya consulta la nueva dirección' : 'URL actualizada',
    )
    if (ok) setEditando(null)
  }

  function activar(n: NodoPeriferico) {
    void ejecutar(
      () => api(`/nodos/${n.id}/activo`, { method: 'PUT', body: JSON.stringify({ activo: true }) }),
      `Se activó ${n.nombre}: el central vuelve a obtener pesajes de este nodo`,
    )
  }

  async function desactivar(n: NodoPeriferico) {
    const ok = await ejecutar(
      () => api(`/nodos/${n.id}/activo`, { method: 'PUT', body: JSON.stringify({ activo: false, fundamento }) }),
      `Se desactivó ${n.nombre}`,
    )
    if (ok) {
      setDesactivando(null)
      setFundamento('')
    }
  }

  function estado(n: NodoPeriferico) {
    if (!n.activo) return <Etiqueta tono="neutro">Desactivado</Etiqueta>
    if (n.enLinea === null) return <Etiqueta tono="neutro">Sin verificar</Etiqueta>
    return n.enLinea ? <Etiqueta tono="ok">En línea</Etiqueta> : <Etiqueta tono="error">Sin respuesta</Etiqueta>
  }

  return (
    <>
      <EncabezadoSeccion
        titulo="Nodos periféricos"
        descripcion="Los sistemas de campo que se integran con el central. El estado se verifica contra el nodo real en el momento."
      >
        <button type="button" className="boton-secundario" onClick={() => setVersion((v) => v + 1)}>
          Verificar de nuevo
        </button>
      </EncabezadoSeccion>

      {error && (
        <p className="mensaje-error" role="alert">
          {error}
        </p>
      )}

      <div className="tabla-contenedor">
        <table className="tabla" aria-busy={cargando}>
          <thead>
            <tr>
              <th>Nodo</th>
              <th>Dirección</th>
              <th>Estado</th>
              <th>
                <span className="solo-lectores">Acciones</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {cargando && nodos.length === 0 && <FilasCargando columnas={4} />}
            {nodos.map((n) => (
              <tr key={n.id}>
                <td data-etiqueta="Nodo">
                  <strong>{n.nombre}</strong>
                  <span className="celda-secundaria">{n.tipo === 'BALANZAS' ? 'Balanzas (el central lo consulta)' : 'Tracking (envía posiciones)'}</span>
                  {n.actualizadoEn && <span className="celda-secundaria">Modificado {formatearMomento(n.actualizadoEn)}</span>}
                </td>
                <td data-etiqueta="Dirección">
                  {n.tipo !== 'BALANZAS' ? (
                    <span className="celda-vacia">No aplica: el nodo llama al central</span>
                  ) : editando === n.id ? (
                    <div className="edicion-en-linea">
                      <input
                        value={urlEditada}
                        onChange={(e) => setUrlEditada(e.target.value)}
                        aria-label={`URL de ${n.nombre}`}
                        type="url"
                        autoFocus
                      />
                      <button type="button" className="boton-chico" onClick={() => void guardarUrl(n)} disabled={!urlEditada.trim()}>
                        Guardar
                      </button>
                      <button type="button" className="boton-chico boton-secundario" onClick={() => setEditando(null)}>
                        Cancelar
                      </button>
                    </div>
                  ) : (
                    <div className="edicion-en-linea">
                      <span>{n.url}</span>
                      <button
                        type="button"
                        className="boton-texto"
                        onClick={() => {
                          setEditando(n.id)
                          setUrlEditada(n.url ?? '')
                        }}
                        aria-label={`Editar la URL de ${n.nombre}`}
                      >
                        Editar
                      </button>
                    </div>
                  )}
                </td>
                <td data-etiqueta="Estado">
                  {estado(n)}
                  {n.activo && n.detalle && <span className="celda-secundaria">{n.detalle}</span>}
                </td>
                <td data-etiqueta="" className="celda-acciones">
                  {n.tipo !== 'BALANZAS' ? (
                    <span className="celda-secundaria">Se habilita con su token</span>
                  ) : desactivando === n.id ? (
                    <div className="edicion-en-linea">
                      <input
                        value={fundamento}
                        onChange={(e) => setFundamento(e.target.value)}
                        placeholder="Motivo"
                        aria-label={`Motivo para desactivar ${n.nombre}`}
                        autoFocus
                      />
                      <button
                        type="button"
                        className="boton-chico"
                        onClick={() => void desactivar(n)}
                        disabled={!fundamento.trim()}
                      >
                        Desactivar
                      </button>
                      <button
                        type="button"
                        className="boton-chico boton-secundario"
                        onClick={() => {
                          setDesactivando(null)
                          setFundamento('')
                        }}
                      >
                        Cancelar
                      </button>
                    </div>
                  ) : n.activo ? (
                    <button type="button" className="boton-chico boton-secundario" onClick={() => setDesactivando(n.id)}>
                      Desactivar
                    </button>
                  ) : (
                    <button type="button" className="boton-chico boton-secundario" onClick={() => activar(n)}>
                      Activar
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {!cargando && nodos.length === 0 && (
              <tr>
                <td colSpan={4} className="tabla-vacia">
                  No hay nodos registrados.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {alta ? (
        <section className="ficha ficha-ancha" aria-labelledby="titulo-alta-nodo">
          <h2 id="titulo-alta-nodo">Nuevo nodo Balanzas</h2>
          <p className="ficha-texto">
            Queda desactivado: el central consulta a un solo nodo Balanzas, así que para pasar a este hay que desactivar el actual y
            activar el nuevo.
          </p>
          <form onSubmit={(e) => void crear(e)} className="formulario-grilla" autoComplete="off">
            <label className="campo">
              <span>Nombre</span>
              <input value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} required autoFocus />
            </label>
            <label className="campo">
              <span>URL de consulta de pesadas</span>
              <input
                value={nuevo.url}
                onChange={(e) => setNuevo({ ...nuevo, url: e.target.value })}
                placeholder="https://balanzas.ejemplo.gub.uy/api/pesadas"
                type="url"
                required
              />
            </label>
            <div className="formulario-botones">
              <button type="submit">Dar de alta</button>
              <button type="button" className="boton-secundario" onClick={() => setAlta(false)}>
                Cancelar
              </button>
            </div>
          </form>
        </section>
      ) : (
        <p className="nota-seccion">
          ¿Hay un nodo Balanzas nuevo o de respaldo?{' '}
          <button type="button" className="boton-texto" onClick={() => setAlta(true)}>
            Darlo de alta
          </button>
        </p>
      )}
    </>
  )
}
