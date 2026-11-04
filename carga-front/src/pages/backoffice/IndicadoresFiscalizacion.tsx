import { useEffect, useState } from 'react'
import { descargarCsv, reporteFiscalizacion, TIPOS, type ReporteFiscalizacion } from '../../api/fiscalizacion'
import { etiquetaCargaActividad, type ReporteActividad } from '../../api/reportes'

function numero(valor: number): string { return valor.toLocaleString('es-UY') }
function porcentaje(valor: number | null | undefined): string {
  return valor == null ? '—' : `${(valor * 100).toLocaleString('es-UY', { maximumFractionDigits: 1 })} %`
}

/**
 * Indicadores de fiscalización del tablero de la autoridad (P5, ADR-0022): infracciones detectadas por
 * tipo y tasa de confirmadas sobre detectadas, en el mismo período que el reporte de actividad.
 * Exporta el tablero en CSV y en PDF (impresión del navegador).
 */
export function IndicadoresFiscalizacion({ actividad }: { actividad: ReporteActividad }) {
  const { desde, hasta } = actividad
  const finalizados = actividad.criterioTemporal === 'FIN_VIAJE'
  const unidad = finalizados ? 'Viajes finalizados' : 'Guías registradas'
  const criterio = finalizados ? 'Instante del evento FIN aceptado por el central' : 'Fecha de creación de la guía'
  const [datos, setDatos] = useState<{ clave: string; reporte: ReporteFiscalizacion } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const clave = `${desde}:${hasta}`

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setError(null)
    reporteFiscalizacion(desde, hasta, controlador.signal)
      .then((reporte) => { if (vigente) setDatos({ clave, reporte }) })
      .catch((causa: unknown) => {
        if (vigente) setError(causa instanceof Error ? causa.message : 'No se pudieron consultar los indicadores de fiscalización.')
      })
    return () => { vigente = false; controlador.abort() }
  }, [desde, hasta, clave])

  const r = datos?.clave === clave ? datos.reporte : null

  function exportarCsv() {
    if (!r) return
    descargarCsv(`carga-uy-tablero-${actividad.criterioTemporal}-${desde}-${hasta}-carga-${actividad.tipoCargaId ?? 'todos'}.csv`, [
      ['Indicador', 'Valor'],
      ['Período', `${desde} a ${hasta}`],
      ['Zona horaria', 'America/Montevideo'],
      ['Sección', unidad],
      ['Criterio temporal de actividad', criterio],
      ['Código del criterio', actividad.criterioTemporal],
      ...(finalizados ? [['Alcance de finalizaciones', 'Una guía por viaje con evento FIN; no constatación física. Eventos tardíos pueden actualizar períodos anteriores.']] : []),
      ['Tipo de carga (solo actividad)', etiquetaCargaActividad(actividad)],
      [unidad, actividad.totalGuias],
      [finalizados ? 'Empresas con viajes finalizados' : 'Empresas con guías', actividad.empresasConGuias],
      [],
      ['Estado actual de la guía', `${unidad} (filtros de actividad aplicados)`],
      ...actividad.porEstado.map((g) => [g.etiqueta, g.cantidad]),
      [],
      ['Rubro', `${unidad} (filtros de actividad aplicados)`],
      ...actividad.porRubro.map((g) => [g.etiqueta, g.cantidad]),
      [],
      [finalizados ? 'Día del evento FIN' : 'Día de creación', `${unidad} (filtros de actividad aplicados)`],
      ...actividad.porDia.map((g) => [g.clave, g.cantidad]),
      [],
      ['Sección', 'Fiscalización general'],
      ['Criterio temporal de fiscalización', 'Fecha de detección del caso'],
      ['Alcance de fiscalización', 'Todos los tipos de carga y casos sin guía; sin filtro por tipo de carga ni por viajes finalizados'],
      ['Infracciones detectadas', r.detectados],
      ['Infracciones confirmadas', r.confirmados],
      ['Infracciones desestimadas', r.desestimados],
      ['Casos pendientes de resolución', r.pendientes],
      ['Tasa de confirmadas sobre detectadas', r.tasaConfirmacion == null ? '' : r.tasaConfirmacion.toFixed(4)],
      [],
      ['Tipo de infracción', 'Detectadas', 'Confirmadas', 'Desestimadas'],
      ...r.porTipo.map((t) => [TIPOS[t.tipo], t.detectados, t.confirmados, t.desestimados]),
    ])
  }

  return <section className="reporte-panel" aria-labelledby="titulo-fiscalizacion">
    <h2 id="titulo-fiscalizacion">Fiscalización</h2>
    <p>Casos detectados automáticamente en el período, según su estado actual.</p>
    <p className="reporte-alcance"><strong>Fiscalización general, sin filtro por tipo de carga.</strong>{' '}
      Estos indicadores incluyen todos los tipos de carga y los casos sin guía. El período se aplica a la fecha de detección del caso,
      no a la fecha de creación o finalización del viaje. La consulta de viajes finalizados no filtra estos casos.</p>
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    {!r && !error && <p role="status">Consultando los indicadores de fiscalización…</p>}
    {r && <>
      <div className="reporte-indicadores">
        <section className="reporte-indicador"><h3>Infracciones detectadas</h3><p className="reporte-cifra">{numero(r.detectados)}</p>
          <p>{numero(r.pendientes)} todavía sin resolver.</p></section>
        <section className="reporte-indicador"><h3>Confirmadas sobre detectadas</h3><p className="reporte-cifra">{porcentaje(r.tasaConfirmacion)}</p>
          <p>{numero(r.confirmados)} confirmadas y {numero(r.desestimados)} desestimadas.</p></section>
      </div>
      <div className="tabla-contenedor"><table className="tabla">
        <caption>Infracciones por tipo</caption>
        <thead><tr><th scope="col">Tipo</th><th scope="col">Detectadas</th><th scope="col">Confirmadas</th><th scope="col">Desestimadas</th></tr></thead>
        <tbody>{r.porTipo.map((t) => <tr key={t.tipo}>
          <th scope="row">{TIPOS[t.tipo]}</th><td>{numero(t.detectados)}</td><td>{numero(t.confirmados)}</td><td>{numero(t.desestimados)}</td>
        </tr>)}</tbody>
      </table></div>
      <div className="reporte-filtros-acciones no-imprimir">
        <button type="button" className="boton-secundario" onClick={exportarCsv}>Exportar CSV</button>
        <button type="button" className="boton-secundario" onClick={() => window.print()}>Exportar PDF</button>
      </div>
    </>}
  </section>
}
