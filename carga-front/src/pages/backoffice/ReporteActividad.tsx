import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { consultarActividad, consultarViajesFinalizados, consultarTiposCargaActividad, errorPeriodoActividad, errorTipoCargaActividad,
  etiquetaCargaActividad, type ConteoActividad, type ReporteActividad as DatosReporte, type TipoCargaActividad } from '../../api/reportes'
import { ErrorApi } from '../../api/cliente'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { formatearFecha } from '../../formato'
import { useTituloPagina } from '../../titulo'
import './reporteActividad.css'
import { IndicadoresFiscalizacion } from './IndicadoresFiscalizacion'

type Carga = { estado: 'cargando'; consulta: string }
  | { estado: 'lista'; consulta: string; datos: DatosReporte }
  | { estado: 'error'; consulta: string; mensaje: string }

type Catalogo = { estado: 'cargando' }
  | { estado: 'lista'; tipos: TipoCargaActividad[] }
  | { estado: 'error'; mensaje: string }

const ESTADOS: Record<string, string> = { CREADA: 'Creada', ASIGNADA: 'Asignada', EN_CURSO: 'En curso', FINALIZADA: 'Finalizada' }
function numero(valor: number): string { return valor.toLocaleString('es-UY') }

function EvolucionDiaria({ grupos, finalizados }: { grupos: ConteoActividad[]; finalizados: boolean }) {
  const maximo = Math.max(1, ...grupos.map((grupo) => grupo.cantidad))
  const puntos = grupos.map((grupo, i) => `${grupos.length === 1 ? 320 : 30 + i * 580 / (grupos.length - 1)},${150 - grupo.cantidad * 125 / maximo}`).join(' ')
  return <section className="reporte-panel" aria-labelledby="titulo-evolucion">
    <h2 id="titulo-evolucion">Evolución diaria</h2>
    <p>{finalizados ? 'Viajes finalizados por día del evento FIN.' : 'Guías registradas por día de creación.'} Los días sin registros se muestran con cero.</p>
    <div className="reporte-grafico" aria-hidden="true">
      <svg viewBox="0 0 640 180" focusable="false">
        <line x1="30" y1="25" x2="610" y2="25" className="reporte-guia-grafico" />
        <line x1="30" y1="150" x2="610" y2="150" className="reporte-guia-grafico" />
        <polyline points={puntos} className="reporte-linea" />
        {grupos.length === 1 && <circle cx="320" cy={150 - grupos[0].cantidad * 125 / maximo} r="4" className="reporte-punto" />}
        <text x="30" y="16">Máximo de escala: {numero(maximo)} {finalizados ? 'viajes' : 'guías'}</text>
        <text x="30" y="173">{formatearFecha(grupos[0].clave)}</text>
        <text x="610" y="173" textAnchor="end">{formatearFecha(grupos[grupos.length - 1].clave)}</text>
      </svg>
    </div>
    <details className="reporte-tabla-diaria">
      <summary>Consultar los datos diarios en una tabla</summary>
      <div className="tabla-contenedor"><table className="tabla">
        <caption>{finalizados ? 'Viajes por fecha del evento FIN' : 'Guías por fecha de creación'} en America/Montevideo</caption>
        <thead><tr><th scope="col">Fecha</th><th scope="col">{finalizados ? 'Viajes finalizados' : 'Guías registradas'}</th></tr></thead>
        <tbody>{grupos.map((grupo) => <tr key={grupo.clave}>
          <th scope="row">{formatearFecha(grupo.clave)}</th><td>{numero(grupo.cantidad)}</td>
        </tr>)}</tbody>
      </table></div>
    </details>
  </section>
}

function DistribucionRubros({ grupos, total, finalizados }: { grupos: ConteoActividad[]; total: number; finalizados: boolean }) {
  return <section className="reporte-panel" aria-labelledby="titulo-rubros">
    <h2 id="titulo-rubros">Distribución por rubro</h2>
    <p>Rubro declarado en la guía, no el rubro actual de la empresa.</p>
    {grupos.length === 0 ? <p>No hay rubros para los filtros aplicados.</p>
      : <div className="tabla-contenedor"><table className="tabla reporte-rubros">
        <caption>{finalizados ? 'Viajes finalizados' : 'Guías'} y proporción del total por rubro</caption>
        <thead><tr><th scope="col">Rubro</th><th scope="col">{finalizados ? 'Viajes' : 'Guías'}</th><th scope="col">Proporción</th></tr></thead>
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
  const desde = parametros.get('desde'), hasta = parametros.get('hasta'), tipoCargaId = parametros.get('tipoCargaId')
  const vista = parametros.get('vista') ?? 'guias'
  const [borrador, setBorrador] = useState({ desde: desde ?? '', hasta: hasta ?? '', tipoCargaId, vista })
  const [catalogo, setCatalogo] = useState<Catalogo>({ estado: 'cargando' })
  const [versionCatalogo, setVersionCatalogo] = useState(0)
  const [version, setVersion] = useState(0)
  const consulta = JSON.stringify([desde, hasta, tipoCargaId, vista, version])
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando', consulta })
  const [errorFiltro, setErrorFiltro] = useState<string | null>(null)
  const focoError = useRef<HTMLParagraphElement>(null)

  useEffect(() => {
    setBorrador({ desde: desde ?? '', hasta: hasta ?? '', tipoCargaId, vista }); setErrorFiltro(null)
  }, [desde, hasta, tipoCargaId, vista])

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setCatalogo({ estado: 'cargando' })
    consultarTiposCargaActividad(controlador.signal).then((tipos) => {
      if (vigente) setCatalogo({ estado: 'lista', tipos })
    }).catch((error: unknown) => {
      if (vigente) setCatalogo({ estado: 'error', mensaje: error instanceof Error ? error.message : 'No se pudo consultar el catálogo.' })
    })
    return () => { vigente = false; controlador.abort() }
  }, [versionCatalogo])

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setCarga({ estado: 'cargando', consulta })
    if (vista !== 'guias' && vista !== 'finalizados') {
      setCarga({ estado: 'error', consulta, mensaje: 'La consulta solicitada no es válida. Selecciona Guías registradas o Viajes finalizados.' })
      return () => { vigente = false; controlador.abort() }
    }
    const consultar = vista === 'finalizados' ? consultarViajesFinalizados : consultarActividad
    consultar(desde, hasta, controlador.signal, tipoCargaId).then((datos) => {
      if (!vigente) return
      setCarga({ estado: 'lista', consulta, datos })
      if (desde === null && hasta === null)
        setBorrador((actual) => ({ ...actual, desde: datos.desde, hasta: datos.hasta }))
    }).catch((error: unknown) => {
      if (!vigente) return
      const mensaje = error instanceof ErrorApi && error.status === 403
        ? 'Tu sesión no tiene permiso para consultar reportes de autoridad.'
        : error instanceof Error ? error.message : 'No se pudo consultar el reporte.'
      setCarga({ estado: 'error', consulta, mensaje })
    })
    return () => { vigente = false; controlador.abort() }
  }, [desde, hasta, tipoCargaId, vista, version, consulta])

  useEffect(() => { if (errorFiltro) focoError.current?.focus() }, [errorFiltro])

  function aplicar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const error = errorPeriodoActividad(borrador.desde, borrador.hasta) ?? errorTipoCargaActividad(borrador.tipoCargaId)
      ?? (borrador.vista === 'guias' || borrador.vista === 'finalizados' ? null : 'Selecciona una consulta válida.')
    setErrorFiltro(error)
    if (error) { focoError.current?.focus(); return }
    const siguientes = new URLSearchParams({ desde: borrador.desde, hasta: borrador.hasta })
    if (borrador.tipoCargaId !== null) siguientes.set('tipoCargaId', borrador.tipoCargaId)
    if (borrador.vista === 'finalizados') siguientes.set('vista', 'finalizados')
    setParametros(siguientes)
    setVersion((actual) => actual + 1)
  }

  // Un cambio de URL invalida el resultado anterior incluso antes de ejecutar el efecto.
  const actual = carga.consulta === consulta ? carga : { estado: 'cargando' as const }
  const datos = actual.estado === 'lista' ? actual.datos : null
  const cargando = actual.estado === 'cargando'
  const finalizados = datos?.criterioTemporal === 'FIN_VIAJE'
  const tipos = catalogo.estado === 'lista' ? catalogo.tipos : []
  const tipoFueraDelCatalogo = borrador.tipoCargaId !== null
    && !tipos.some((tipo) => String(tipo.id) === borrador.tipoCargaId)
  return <>
    <EncabezadoSeccion titulo="Reportes de actividad" descripcion="Guías registradas o viajes finalizados según los eventos aceptados por el central. Solo lectura.">
      <Link to="/backoffice/auditoria">Consultar auditoría</Link>
    </EncabezadoSeccion>
    <aside className="reporte-alcance">
      <strong>Qué mide este tablero</strong>
      <p>Guías registradas usa la fecha de creación. Viajes finalizados usa el instante del evento FIN aceptado por el central,
        no la fecha prevista ni la de recepción. Todos los días se interpretan en America/Montevideo.
        Los estados son los actuales al consultar, no los del cierre del período.</p>
      <p>El tipo de carga filtra únicamente la consulta de actividad seleccionada. Fiscalización conserva todos los tipos y los casos sin guía.</p>
    </aside>
    <form className="reporte-filtros" onSubmit={aplicar} aria-label="Consulta y filtros de actividad">
      <p id="ayuda-periodo-reporte">Ambas fechas incluidas. Máximo 366 días por consulta. Sin fechas en la URL, el central selecciona los últimos 30 días.</p>
      <label className="campo"><span>Consulta</span>
        <select name="vista" value={borrador.vista} aria-invalid={!!errorFiltro}
          aria-describedby={`ayuda-consulta-reporte${errorFiltro ? ' error-periodo-reporte' : ''}`}
          onChange={(evento) => setBorrador((actual) => ({ ...actual, vista: evento.target.value }))}>
          {borrador.vista !== 'guias' && borrador.vista !== 'finalizados' && <option value={borrador.vista}>Consulta no válida en la URL</option>}
          <option value="guias">Guías registradas</option>
          <option value="finalizados">Viajes finalizados</option>
        </select>
      </label>
      <label className="campo"><span>Desde</span><input type="date" name="desde" required min="0001-01-01" max="9999-12-31"
        value={borrador.desde} disabled={cargando && desde === null && hasta === null}
        aria-invalid={!!errorFiltro} aria-describedby={`ayuda-periodo-reporte${errorFiltro ? ' error-periodo-reporte' : ''}`}
        onChange={(evento) => setBorrador((actual) => ({ ...actual, desde: evento.target.value }))} /></label>
      <label className="campo"><span>Hasta</span><input type="date" name="hasta" required min="0001-01-01" max="9999-12-31"
        value={borrador.hasta} disabled={cargando && desde === null && hasta === null}
        aria-invalid={!!errorFiltro} aria-describedby={`ayuda-periodo-reporte${errorFiltro ? ' error-periodo-reporte' : ''}`}
        onChange={(evento) => setBorrador((actual) => ({ ...actual, hasta: evento.target.value }))} /></label>
      <label className="campo"><span>Tipo de carga (solo actividad)</span>
        <select name="tipoCargaId" value={borrador.tipoCargaId === null ? '' : `tipo:${borrador.tipoCargaId}`}
          aria-describedby={`ayuda-tipo-carga${errorFiltro ? ' error-periodo-reporte' : ''}`} aria-invalid={!!errorFiltro}
          onChange={(evento) => setBorrador((actual) => ({ ...actual,
            tipoCargaId: evento.target.value === '' ? null : evento.target.value.slice(5) }))}>
          <option value="">Todos los tipos</option>
          {tipoFueraDelCatalogo && borrador.tipoCargaId !== null && <option value={`tipo:${borrador.tipoCargaId}`}>
            {datos?.tipoCargaId != null && String(datos.tipoCargaId) === borrador.tipoCargaId
              ? datos.tipoCargaNombre : 'Tipo solicitado (no disponible en el catálogo)'}
          </option>}
          {tipos.map((tipo) => <option key={tipo.id} value={`tipo:${tipo.id}`}>{tipo.nombre}</option>)}
        </select>
      </label>
      <p id="ayuda-consulta-reporte">El período se aplica a creación de guías o finalización del viaje, según la consulta elegida.
        Restablecer vuelve a Guías registradas, últimos 30 días y todos los tipos.</p>
      <p id="ayuda-tipo-carga">No modifica las cifras de fiscalización. Todos los tipos incluye guías sin tipo declarado.</p>
      {catalogo.estado === 'cargando' && <p role="status">Consultando tipos de carga…</p>}
      {catalogo.estado === 'lista' && tipos.length === 0 && <p>No hay tipos de carga en el catálogo.</p>}
      {catalogo.estado === 'error' && <div role="alert">
        <p className="mensaje-error">{catalogo.mensaje} Puedes consultar todos los tipos o quitar el filtro.</p>
        <button type="button" className="boton-secundario" onClick={() => setVersionCatalogo((actual) => actual + 1)}>Reintentar catálogo</button>
      </div>}
      <div className="reporte-filtros-acciones">
        <button type="submit" disabled={cargando && desde === null && hasta === null}>Aplicar filtros</button>
        {tipoCargaId !== null && <button type="button" className="boton-secundario" onClick={() => {
          const siguientes = new URLSearchParams(parametros); siguientes.delete('tipoCargaId')
          setParametros(siguientes); setVersion((actual) => actual + 1)
        }}>Quitar filtro de carga</button>}
        <button type="button" className="boton-secundario" onClick={() => {
          setErrorFiltro(null); setParametros({}); setVersion((actual) => actual + 1)
        }}>Restablecer consulta</button>
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
        <button type="button" className="boton-secundario no-imprimir" onClick={() => setVersion((anterior) => anterior + 1)}>Actualizar reporte</button>
      </div>
      <p className="reporte-alcance" role="status">Consulta aplicada: <strong>{finalizados ? 'Viajes finalizados por fecha del evento FIN' : 'Guías registradas por fecha de creación'}</strong>.
        {' '}Tipo de carga aplicado: <strong>{etiquetaCargaActividad(datos)}</strong>.
        {' '}Este filtro se aplica a los totales, estados, evolución y rubros de actividad, no a fiscalización.</p>
      {finalizados && <p className="reporte-alcance">Finalizaciones registradas según eventos aceptados, no constatación física del viaje.
        Los eventos sincronizados más tarde pueden actualizar resultados de períodos anteriores.
        Sin evento FIN no se infiere una finalización por el estado ni las fechas previstas.</p>}
      {datos.totalGuias === 0 && <p className="reporte-vacio" role="status">{finalizados ? 'No hay viajes finalizados' : 'No se registraron guías'} con los filtros aplicados.</p>}
      <div className="reporte-indicadores">
        <section className="reporte-indicador"><h2>{finalizados ? 'Viajes finalizados' : 'Guías registradas'}</h2>
          <p className="reporte-cifra">{numero(datos.totalGuias)}</p><p>{finalizados ? 'Un viaje por guía con FIN en el período aplicado.' : 'Creadas durante el período aplicado.'}</p></section>
        <section className="reporte-indicador"><h2>{finalizados ? 'Empresas con viajes finalizados' : 'Empresas con guías'}</h2>
          <p className="reporte-cifra">{numero(datos.empresasConGuias)}</p><p>Empresas distintas de la consulta aplicada, incluidas las hoy inactivas.</p></section>
        <section className="reporte-indicador"><h2>Estado actual de las guías</h2>
          <dl className="reporte-estados">{datos.porEstado.map((grupo) => <div key={grupo.clave}>
            <dt>{ESTADOS[grupo.clave]}</dt><dd>{numero(grupo.cantidad)}</dd>
          </div>)}</dl></section>
      </div>
      <EvolucionDiaria grupos={datos.porDia} finalizados={finalizados} />
      <DistribucionRubros grupos={datos.porRubro} total={datos.totalGuias} finalizados={finalizados} />
      <IndicadoresFiscalizacion actividad={datos} />
      <p className="nota-seccion">No consulta nodos en vivo ni expone datos personales.</p>
    </>}
  </>
}
