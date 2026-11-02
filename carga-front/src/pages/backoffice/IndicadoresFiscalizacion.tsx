import { useEffect, useState } from 'react'
import { descargarCsv, reporteFiscalizacion, TIPOS, type ReporteFiscalizacion } from '../../api/fiscalizacion'
import type { ReporteActividad } from '../../api/reportes'

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
  const [datos, setDatos] = useState<{ clave: string; reporte: ReporteFiscalizacion } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const clave = `${desde}:${hasta}`

  useEffect(() => {
    const controlador = new AbortController()
    setError(null)
    reporteFiscalizacion(desde, hasta, controlador.signal)
      .then((reporte) => setDatos({ clave, reporte }))
      .catch((causa: unknown) => {
        if (!controlador.signal.aborted) setError(causa instanceof Error ? causa.message : 'No se pudieron consultar los indicadores de fiscalización.')
      })
    return () => controlador.abort()
  }, [desde, hasta, clave])

  const r = datos?.clave === clave ? datos.reporte : null

  function exportarCsv() {
    if (!r) return
    descargarCsv(`carga-uy-tablero-${desde}-${hasta}.csv`, [
      ['Indicador', 'Valor'],
      ['Período', `${desde} a ${hasta}`],
      ['Guías registradas', actividad.totalGuias],
      ['Empresas con guías', actividad.empresasConGuias],
      ['Infracciones detectadas', r.detectados],
      ['Infracciones confirmadas', r.confirmados],
      ['Infracciones desestimadas', r.desestimados],
      ['Casos pendientes de resolución', r.pendientes],
      ['Tasa de confirmadas sobre detectadas', r.tasaConfirmacion == null ? '' : r.tasaConfirmacion.toFixed(4)],
      [],
      ['Tipo de infracción', 'Detectadas', 'Confirmadas', 'Desestimadas'],
      ...r.porTipo.map((t) => [TIPOS[t.tipo], t.detectados, t.confirmados, t.desestimados]),
      [],
      ['Rubro', 'Guías'],
      ...actividad.porRubro.map((g) => [g.etiqueta, g.cantidad]),
      [],
      ['Día', 'Guías'],
      ...actividad.porDia.map((g) => [g.clave, g.cantidad]),
    ])
  }

  return <section className="reporte-panel" aria-labelledby="titulo-fiscalizacion">
    <h2 id="titulo-fiscalizacion">Fiscalización</h2>
    <p>Casos detectados automáticamente en el período, según su estado actual.</p>
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
