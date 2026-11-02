import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { consultarActividad, errorPeriodoActividad, type ConteoActividad, type ReporteActividad as DatosReporte } from '../../api/reportes'
import { ErrorApi } from '../../api/cliente'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { formatearFecha } from '../../formato'
import { useTituloPagina } from '../../titulo'
import './reporteActividad.css'
import { IndicadoresFiscalizacion } from './IndicadoresFiscalizacion'

type Carga = { estado: 'cargando'; consulta: string }
  | { estado: 'lista'; consulta: string; datos: DatosReporte }
  | { estado: 'error'; consulta: string; mensaje: string }

const ESTADOS: Record<string, string> = { CREADA: 'Creada', ASIGNADA: 'Asignada', EN_CURSO: 'En curso', FINALIZADA: 'Finalizada' }
function numero(valor: number): string { return valor.toLocaleString('es-UY') }

function EvolucionDiaria({ grupos }: { grupos: ConteoActividad[] }) {
  const maximo = Math.max(1, ...grupos.map((grupo) => grupo.cantidad))
  const puntos = grupos.map((grupo, i) => `${grupos.length === 1 ? 320 : 30 + i * 580 / (grupos.length - 1)},${150 - grupo.cantidad * 125 / maximo}`).join(' ')
  return <section className="reporte-panel" aria-labelledby="titulo-evolucion">
    <h2 id="titulo-evolucion">Evolución diaria</h2>
    <p>Guías registradas por día. Los días sin registros se muestran con cero.</p>
    <div className="reporte-grafico" aria-hidden="true">
      <svg viewBox="0 0 640 180" focusable="false">
        <line x1="30" y1="25" x2="610" y2="25" className="reporte-guia-grafico" />
        <line x1="30" y1="150" x2="610" y2="150" className="reporte-guia-grafico" />
        <polyline points={puntos} className="reporte-linea" />
        {grupos.length === 1 && <circle cx="320" cy={150 - grupos[0].cantidad * 125 / maximo} r="4" className="reporte-punto" />}
        <text x="30" y="16">Máximo de escala: {numero(maximo)} guías</text>
        <text x="30" y="173">{formatearFecha(grupos[0].clave)}</text>
        <text x="610" y="173" textAnchor="end">{formatearFecha(grupos[grupos.length - 1].clave)}</text>
      </svg>
    </div>
    <details className="reporte-tabla-diaria">
      <summary>Consultar los datos diarios en una tabla</summary>
      <div className="tabla-contenedor"><table className="tabla">
        <caption>Guías por fecha de creación en America/Montevideo</caption>
        <thead><tr><th scope="col">Fecha</th><th scope="col">Guías registradas</th></tr></thead>
        <tbody>{grupos.map((grupo) => <tr key={grupo.clave}>
          <th scope="row">{formatearFecha(grupo.clave)}</th><td>{numero(grupo.cantidad)}</td>
        </tr>)}</tbody>
      </table></div>
    </details>
  </section>
}

function DistribucionRubros({ grupos, total }: { grupos: ConteoActividad[]; total: number }) {
  return <section className="reporte-panel" aria-labelledby="titulo-rubros">
    <h2 id="titulo-rubros">Distribución por rubro</h2>
    <p>Rubro declarado en la guía, no el rubro actual de la empresa.</p>
    {grupos.length === 0 ? <p>No hay rubros para este período porque no se registraron guías.</p>
      : <div className="tabla-contenedor"><table className="tabla reporte-rubros">
        <caption>Guías y proporción del total por rubro</caption>
        <thead><tr><th scope="col">Rubro</th><th scope="col">Guías</th><th scope="col">Proporción</th></tr></thead>
        <tbody>{grupos.map((grupo) => {
          const porcentaje = grupo.cantidad * 100 / total
          return <tr key={grupo.clave}>
            <th scope="row">{grupo.etiqueta}{grupo.clave === 'SIN_RUBRO' && <span className="celda-secundaria">No declarado</span>}</th>
            <td>{numero(grupo.cantidad)}</td>
            <td><span>{porcentaje.toLocaleString('es-UY', { maximumFractionDigits: 1 })} %</span>
              <span className="reporte-barra" aria-hidden="true"><span style={{ width: `${porcentaje}%` }} /></span></td>
          </tr>
        })}</tbody>
      </table></div>}
  </section>
}

export function ReporteActividad() {
  useTituloPagina('Reportes de actividad', true)
  const [parametros, setParametros] = useSearchParams()
  const desde = parametros.get('desde'), hasta = parametros.get('hasta')
  const [borrador, setBorrador] = useState({ desde: desde ?? '', hasta: hasta ?? '' })
  const [version, setVersion] = useState(0)
  const consulta = JSON.stringify([desde, hasta, version])
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando', consulta })
  const [errorFiltro, setErrorFiltro] = useState<string | null>(null)
  const focoError = useRef<HTMLParagraphElement>(null)

  useEffect(() => {
    setBorrador({ desde: desde ?? '', hasta: hasta ?? '' }); setErrorFiltro(null)
  }, [desde, hasta])

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setCarga({ estado: 'cargando', consulta })
    consultarActividad(desde, hasta, controlador.signal).then((datos) => {
      if (!vigente) return
      setCarga({ estado: 'lista', consulta, datos })
      if (desde === null && hasta === null) setBorrador({ desde: datos.desde, hasta: datos.hasta })
    }).catch((error: unknown) => {
      if (!vigente) return
      const mensaje = error instanceof ErrorApi && error.status === 403
        ? 'Tu sesión no tiene permiso para consultar reportes de autoridad.'
        : error instanceof Error ? error.message : 'No se pudo consultar el reporte.'
      setCarga({ estado: 'error', consulta, mensaje })
    })
    return () => { vigente = false; controlador.abort() }
  }, [desde, hasta, version, consulta])

  useEffect(() => { if (errorFiltro) focoError.current?.focus() }, [errorFiltro])

  function aplicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const error = errorPeriodoActividad(borrador.desde, borrador.hasta)
    setErrorFiltro(error)
    if (error) { focoError.current?.focus(); return }
    setParametros({ desde: borrador.desde, hasta: borrador.hasta })
    setVersion((actual) => actual + 1)
  }

  // Un cambio de URL invalida el resultado anterior incluso antes de ejecutar el efecto.
  const actual = carga.consulta === consulta ? carga : { estado: 'cargando' as const }
  const datos = actual.estado === 'lista' ? actual.datos : null
  const cargando = actual.estado === 'cargando'
  return <>
    <EncabezadoSeccion titulo="Reportes de actividad" descripcion="Una mirada agregada al registro de guías de transporte. Solo lectura.">
      <Link to="/backoffice/auditoria">Consultar auditoría</Link>
    </EncabezadoSeccion>
    <aside className="reporte-alcance">
      <strong>Qué mide este tablero</strong>
      <p>Guías según su fecha de creación, no viajes efectivamente realizados. Todos los días se interpretan en America/Montevideo.
        Los estados son los actuales al consultar, no los del cierre del período.</p>
    </aside>
    <form className="reporte-filtros" onSubmit={aplicar} aria-label="Período del reporte">
      <p id="ayuda-periodo-reporte">Ambas fechas incluidas. Máximo 366 días por consulta. Sin filtros en la URL, el central selecciona los últimos 30 días.</p>
      <label className="campo"><span>Desde</span><input type="date" name="desde" required min="0001-01-01" max="9999-12-31"
        value={borrador.desde} disabled={cargando && desde === null && hasta === null}
        aria-invalid={!!errorFiltro} aria-describedby={`ayuda-periodo-reporte${errorFiltro ? ' error-periodo-reporte' : ''}`}
        onChange={(evento) => setBorrador((actual) => ({ ...actual, desde: evento.target.value }))} /></label>
      <label className="campo"><span>Hasta</span><input type="date" name="hasta" required min="0001-01-01" max="9999-12-31"
        value={borrador.hasta} disabled={cargando && desde === null && hasta === null}
        aria-invalid={!!errorFiltro} aria-describedby={`ayuda-periodo-reporte${errorFiltro ? ' error-periodo-reporte' : ''}`}
        onChange={(evento) => setBorrador((actual) => ({ ...actual, hasta: evento.target.value }))} /></label>
      <div className="reporte-filtros-acciones">
        <button type="submit" disabled={cargando && desde === null && hasta === null}>Aplicar período</button>
        <button type="button" className="boton-secundario" onClick={() => {
          setErrorFiltro(null); setParametros({}); setVersion((actual) => actual + 1)
        }}>Últimos 30 días</button>
      </div>
      {errorFiltro && <p id="error-periodo-reporte" ref={focoError} tabIndex={-1} className="mensaje-error" role="alert">{errorFiltro}</p>}
    </form>
    {cargando && <p role="status">Consultando el reporte de actividad…</p>}
    {actual.estado === 'error' && <div className="reporte-error">
      <p className="mensaje-error" role="alert">{actual.mensaje}</p>
      <p>No se muestran cifras porque la consulta no pudo completarse.</p>
      <button type="button" className="boton-secundario" onClick={() => setVersion((anterior) => anterior + 1)}>Volver a consultar</button>
    </div>}
    {datos && <>
      <div className="reporte-periodo">
        <p role="status">Período aplicado: <strong>{formatearFecha(datos.desde)} al {formatearFecha(datos.hasta)}</strong> · America/Montevideo</p>
        <button type="button" className="boton-secundario" onClick={() => setVersion((anterior) => anterior + 1)}>Actualizar reporte</button>
      </div>
      {datos.totalGuias === 0 && <p className="reporte-vacio" role="status">No se registraron guías durante este período.</p>}
      <div className="reporte-indicadores">
        <section className="reporte-indicador"><h2>Guías registradas</h2>
          <p className="reporte-cifra">{numero(datos.totalGuias)}</p><p>Creadas durante el período aplicado.</p></section>
        <section className="reporte-indicador"><h2>Empresas con guías</h2>
          <p className="reporte-cifra">{numero(datos.empresasConGuias)}</p><p>Empresas distintas con guías en este período, incluidas las hoy inactivas.</p></section>
        <section className="reporte-indicador"><h2>Estado actual de las guías</h2>
          <dl className="reporte-estados">{datos.porEstado.map((grupo) => <div key={grupo.clave}>
            <dt>{ESTADOS[grupo.clave]}</dt><dd>{numero(grupo.cantidad)}</dd>
          </div>)}</dl></section>
      </div>
      <EvolucionDiaria grupos={datos.porDia} />
      <DistribucionRubros grupos={datos.porRubro} total={datos.totalGuias} />
      <IndicadoresFiscalizacion actividad={datos} />
      <p className="nota-seccion">No consulta nodos en vivo ni expone datos personales.</p>
    </>}
  </>
}
