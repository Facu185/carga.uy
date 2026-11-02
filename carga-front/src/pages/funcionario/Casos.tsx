import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { bandeja, ESTADOS, TIPOS, type EstadoCaso, type PaginaCasos, type TipoInfraccion } from '../../api/fiscalizacion'
import { FilasCargando } from '../../componentes/FilasCargando'
import { EstadoDelCaso } from '../../componentes/FichaCaso'
import { formatearMomento } from '../../formato'
import { useTituloPagina } from '../../titulo'

/** Bandeja de trabajo del funcionario con los casos detectados automáticamente (P5, ADR-0022). */
export function CasosFuncionario() {
  useTituloPagina('Casos de fiscalización · Área del funcionario')
  const [parametros, setParametros] = useSearchParams()
  const estado = parametros.get('estado') ?? ''
  const tipo = parametros.get('tipo') ?? ''
  const pagina = Number(parametros.get('pagina') ?? '0') || 0
  const clave = `${estado}:${tipo}:${pagina}`
  const [respuesta, setRespuesta] = useState<{ clave: string; datos: PaginaCasos } | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controlador = new AbortController()
    setError(null)
    bandeja({ estado, tipo, pagina }, controlador.signal)
      .then((datos) => setRespuesta({ clave, datos }))
      .catch((causa: unknown) => {
        if (!controlador.signal.aborted) setError(causa instanceof Error ? causa.message : 'No se pudo cargar la bandeja.')
      })
    return () => controlador.abort()
  }, [estado, tipo, pagina, clave])

  function filtrar(nombre: string, valor: string) {
    const nuevos = new URLSearchParams(parametros)
    if (valor) nuevos.set(nombre, valor)
    else nuevos.delete(nombre)
    nuevos.delete('pagina')
    setParametros(nuevos)
  }

  const datos = respuesta?.clave === clave ? respuesta.datos : null
  const paginas = datos ? Math.max(1, Math.ceil(datos.total / datos.tamano)) : 1
  return <>
    <header className="seccion-encabezado">
      <div>
        <h1>Casos de fiscalización</h1>
        <p>Incumplimientos que la plataforma detectó al cruzar las guías con los pesajes y el tracking. Revisá la evidencia y el descargo, y resolvé con fundamento.</p>
      </div>
    </header>
    <div className="formulario-grilla" role="search">
      <label className="campo"><span>Estado</span>
        <select value={estado} onChange={(evento) => filtrar('estado', evento.target.value)}>
          <option value="">Todos</option>
          {(Object.keys(ESTADOS) as EstadoCaso[]).map((e) => <option key={e} value={e}>{ESTADOS[e][0]}</option>)}
        </select>
      </label>
      <label className="campo"><span>Tipo</span>
        <select value={tipo} onChange={(evento) => filtrar('tipo', evento.target.value)}>
          <option value="">Todos</option>
          {(Object.keys(TIPOS) as TipoInfraccion[]).map((t) => <option key={t} value={t}>{TIPOS[t]}</option>)}
        </select>
      </label>
    </div>
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    <div className="tabla-contenedor">
      <table className="tabla" aria-busy={!datos}>
        <thead><tr><th>Caso</th><th>Empresa y vehículo</th><th>Detectado</th><th>Estado</th></tr></thead>
        <tbody>
          {!datos && !error && <FilasCargando columnas={4} />}
          {datos?.registros.map((c) => <tr key={c.id}>
            <td data-etiqueta="Caso"><Link to={`/funcionario/casos/${c.id}`}><strong>#{c.id} · {TIPOS[c.tipo]}</strong></Link></td>
            <td data-etiqueta="Empresa y vehículo">{c.empresaNombre ?? '—'}<span className="celda-secundaria">{c.matricula}</span></td>
            <td data-etiqueta="Detectado">{formatearMomento(c.detectadaEn)}</td>
            <td data-etiqueta="Estado"><EstadoDelCaso caso={c} /></td>
          </tr>)}
          {datos && datos.registros.length === 0 && <tr><td colSpan={4} className="tabla-vacia">No hay casos con estos filtros.</td></tr>}
        </tbody>
      </table>
    </div>
    {datos && datos.total > datos.tamano && <nav className="paginacion" aria-label="Páginas de la bandeja">
      <p className="paginacion-resumen">Página {pagina + 1} de {paginas} · {datos.total} casos</p>
      <button type="button" className="boton-secundario boton-chico" disabled={pagina === 0}
        onClick={() => { const n = new URLSearchParams(parametros); n.set('pagina', String(pagina - 1)); setParametros(n) }}>Anterior</button>
      <button type="button" className="boton-secundario boton-chico" disabled={pagina + 1 >= paginas}
        onClick={() => { const n = new URLSearchParams(parametros); n.set('pagina', String(pagina + 1)); setParametros(n) }}>Siguiente</button>
    </nav>}
  </>
}
