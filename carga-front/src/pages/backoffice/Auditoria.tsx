import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { FilasCargando } from '../../componentes/FilasCargando'
import { ENTIDADES, formatearMomento, nombreAccion, nombreEntidad, nombreRol } from '../../formato'
import type { PaginaAuditoria, RegistroAuditoria } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const POR_PAGINA = 50

/** Consulta de la pista de auditoría (solo lectura), paginada. Administradores y autoridades. */
export function Auditoria() {
  useTituloPagina('Auditoría', true)
  const [registros, setRegistros] = useState<RegistroAuditoria[]>([])
  const [pagina, setPagina] = useState(0)
  const [total, setTotal] = useState(0)
  const [usuario, setUsuario] = useState('')
  const [entidad, setEntidad] = useState('')
  const [filtro, setFiltro] = useState({ usuario: '', entidad: '' })
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    const params = new URLSearchParams()
    if (filtro.usuario) params.set('usuario', filtro.usuario)
    if (filtro.entidad) params.set('entidad', filtro.entidad)
    params.set('pagina', String(pagina))
    params.set('tamano', String(POR_PAGINA))
    api<PaginaAuditoria>(`/auditoria?${params.toString()}`)
      .then((resultado) => {
        if (vigente) {
          setRegistros(resultado.registros)
          setTotal(resultado.total)
          setError(null)
        }
      })
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudo cargar la auditoría'))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [filtro, pagina])

  // Al cambiar el filtro se vuelve a la primera página
  function filtrar(e: FormEvent) {
    e.preventDefault()
    setCargando(true)
    setPagina(0)
    setFiltro({ usuario: usuario.trim(), entidad: entidad.trim() })
  }

  function limpiar() {
    setCargando(true)
    setUsuario('')
    setEntidad('')
    setPagina(0)
    setFiltro({ usuario: '', entidad: '' })
  }

  function irAPagina(numero: number) {
    setCargando(true)
    setPagina(numero)
  }

  const paginas = Math.max(1, Math.ceil(total / POR_PAGINA))
  const desde = total === 0 ? 0 : pagina * POR_PAGINA + 1
  const hasta = Math.min(total, (pagina + 1) * POR_PAGINA)

  const hayFiltro = filtro.usuario !== '' || filtro.entidad !== ''

  return (
    <>
      <EncabezadoSeccion titulo="Auditoría" />

      <form onSubmit={filtrar} className="barra-filtros" role="search">
        <label className="campo">
          <span>Usuario</span>
          <input value={usuario} onChange={(e) => setUsuario(e.target.value)} placeholder="Ej.: admin o 12345672" />
        </label>
        <label className="campo">
          <span>Entidad</span>
          <select value={entidad} onChange={(e) => setEntidad(e.target.value)}>
            <option value="">Todas</option>
            {Object.entries(ENTIDADES).map(([codigo, nombre]) => (
              <option key={codigo} value={codigo}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <div className="formulario-botones">
          <button type="submit">Filtrar</button>
          {hayFiltro && (
            <button type="button" className="boton-secundario" onClick={limpiar}>
              Ver todo
            </button>
          )}
        </div>
      </form>

      {error && (
        <p className="mensaje-error" role="alert">
          {error}
        </p>
      )}

      <div className="tabla-contenedor">
        <table className="tabla" aria-busy={cargando}>
          <thead>
            <tr>
              <th>Momento</th>
              <th>Usuario</th>
              <th>Acción</th>
              <th>Sobre</th>
              <th>Fundamento y detalle</th>
            </tr>
          </thead>
          <tbody>
            {cargando && registros.length === 0 && <FilasCargando columnas={5} filas={6} />}
            {registros.map((r) => (
              <tr key={r.id}>
                <td data-etiqueta="Momento" className="celda-nowrap">{formatearMomento(r.momento)}</td>
                <td data-etiqueta="Usuario">
                  <strong>{r.usuario}</strong>
                  {r.rol && <span className="celda-secundaria">{nombreRol(r.rol)}</span>}
                </td>
                <td data-etiqueta="Acción">{nombreAccion(r.accion)}</td>
                <td data-etiqueta="Sobre">
                  {nombreEntidad(r.entidad)}
                  <span className="celda-secundaria">{r.entidadId}</span>
                </td>
                <td data-etiqueta="Fundamento y detalle">
                  {r.fundamento && <span>{r.fundamento}</span>}
                  {r.detalle && <span className="celda-secundaria">{r.detalle}</span>}
                  {!r.fundamento && !r.detalle && <span className="celda-vacia">Sin detalle</span>}
                </td>
              </tr>
            ))}
            {!cargando && registros.length === 0 && (
              <tr>
                <td colSpan={5} className="tabla-vacia">
                  {hayFiltro ? 'No hay registros con esos filtros.' : 'Todavía no hay acciones registradas.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
      {total > 0 && (
        <nav className="paginacion" aria-label="Páginas de la auditoría">
          <p className="paginacion-resumen">
            {desde}–{hasta} de {total} registros, del más reciente al más antiguo
          </p>
          <div className="acciones">
            <button
              type="button"
              className="boton-chico boton-secundario"
              onClick={() => irAPagina(pagina - 1)}
              disabled={pagina === 0 || cargando}
            >
              Más recientes
            </button>
            <span className="paginacion-numero">
              Página {pagina + 1} de {paginas}
            </span>
            <button
              type="button"
              className="boton-chico boton-secundario"
              onClick={() => irAPagina(pagina + 1)}
              disabled={pagina + 1 >= paginas || cargando}
            >
              Más antiguos
            </button>
          </div>
        </nav>
      )}
    </>
  )
}
