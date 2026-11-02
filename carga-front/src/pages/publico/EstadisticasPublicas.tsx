import { useEffect, useState } from 'react'
import { consultarEstadisticas, type CifraPublica, type EstadisticasPublicas as Datos } from '../../api/publico'
import { useTituloPagina } from '../../titulo'
import { PaginaPublica } from './PaginaPublica'
import '../backoffice/reporteActividad.css'

const MESES = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'setiembre', 'octubre', 'noviembre', 'diciembre']

function numero(valor: number): string { return valor.toLocaleString('es-UY') }
function mes(clave: string): string {
  const [anio, numeroMes] = clave.split('-')
  return `${MESES[Number(numeroMes) - 1]} ${anio}`
}

function Cifra({ titulo, valor, detalle }: { titulo: string; valor: number | null | undefined; detalle: string }) {
  return <section className="reporte-indicador">
    <h2>{titulo}</h2>
    <p className="reporte-cifra">{valor == null ? 'Reservado' : numero(valor)}</p>
    <p>{valor == null ? 'La cifra no se publica porque permitiría identificar a una empresa.' : detalle}</p>
  </section>
}

function Tabla({ titulo, descripcion, columna, filas, etiqueta }: {
  titulo: string; descripcion: string; columna: string; filas: CifraPublica[]; etiqueta: (fila: CifraPublica) => string
}) {
  return <section className="reporte-panel">
    <h2>{titulo}</h2>
    <p>{descripcion}</p>
    {filas.length === 0 ? <p>No hay viajes registrados en este período.</p>
      : <div className="tabla-contenedor"><table className="tabla">
        <thead><tr><th scope="col">{columna}</th><th scope="col">Viajes</th><th scope="col">Toneladas declaradas</th></tr></thead>
        <tbody>{filas.map((fila) => <tr key={fila.clave}>
          <th scope="row">{etiqueta(fila)}</th>
          {fila.reservada
            ? <td colSpan={2} className="celda-secundaria">Reservado</td>
            : <><td>{numero(fila.guias ?? 0)}</td><td>{(fila.toneladas ?? 0).toLocaleString('es-UY', { maximumFractionDigits: 1 })}</td></>}
        </tr>)}</tbody>
      </table></div>}
  </section>
}

export function EstadisticasPublicas() {
  useTituloPagina('Cifras del transporte de carga')
  const [datos, setDatos] = useState<Datos | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controlador = new AbortController()
    consultarEstadisticas(controlador.signal)
      .then(setDatos)
      .catch((causa: unknown) => {
        if (!controlador.signal.aborted) setError(causa instanceof Error ? causa.message : 'No se pudieron consultar las cifras.')
      })
    return () => controlador.abort()
  }, [])

  return <PaginaPublica>
    <header className="seccion-encabezado">
      <div>
        <h1>Cifras del transporte de carga</h1>
        <p>Información pública y agregada sobre empresas, vehículos y viajes registrados en carga.uy.</p>
      </div>
    </header>
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    {!datos && !error && <p role="status">Consultando las cifras…</p>}
    {datos && <>
      <aside className="reporte-alcance">
        <strong>Cómo se protege la información</strong>
        <p>Solo se publica una cifra si reúne al menos {datos.umbralGuias} viajes de al menos {datos.umbralEmpresas} empresas distintas.
          Las que no llegan se agrupan o figuran como «Reservado», para que ninguna empresa, vehículo o chofer pueda identificarse.
          Período: {mes(datos.desde)} a {mes(datos.hasta)}, según la fecha de inicio del viaje.</p>
      </aside>
      <div className="reporte-indicadores">
        <Cifra titulo="Empresas habilitadas" valor={datos.empresasHabilitadas} detalle="Empresas de transporte de carga activas hoy." />
        <Cifra titulo="Vehículos habilitados" valor={datos.vehiculosHabilitados} detalle="Vehículos activos de empresas habilitadas." />
        <Cifra titulo="Vehículos al día" valor={datos.vehiculosAlDia} detalle="Con permiso nacional e inspección técnica vigentes hoy." />
      </div>
      <Tabla titulo="Viajes por mes" descripcion="Guías de carga según el mes en que empieza el viaje." columna="Mes"
        filas={datos.porMes} etiqueta={(fila) => mes(fila.clave)} />
      <Tabla titulo="Viajes por tipo de carga" descripcion="Tipo de carga declarado en la guía." columna="Tipo de carga"
        filas={datos.porTipoCarga} etiqueta={(fila) => fila.etiqueta} />
      <Tabla titulo="Viajes por rubro" descripcion="Rubro del cliente declarado en la guía." columna="Rubro"
        filas={datos.porRubro} etiqueta={(fila) => fila.etiqueta} />
    </>}
  </PaginaPublica>
}
