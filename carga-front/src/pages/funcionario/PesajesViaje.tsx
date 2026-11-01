import { useEffect, useState } from 'react'
import { consultarPesajesDeGuia, LIMITE_PESAJES_GUIA, type PesajesDeGuia } from '../../api/pesajes'
import { Etiqueta } from '../../componentes/Etiqueta'
import { momentoViaje } from './DatosViaje'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'lista'; datos: PesajesDeGuia }

/** Se monta solo tras obtener el detalle C14 de la empresa seleccionada. */
export function PesajesViaje({ guiaId, vehiculoAsignado }: { guiaId: number; vehiculoAsignado: boolean }) {
  return <section className="ficha ficha-ancha" aria-labelledby="titulo-pesajes-viaje">
    <h2 id="titulo-pesajes-viaje">Pesajes asociados a la guía</h2>
    {vehiculoAsignado ? <ConsultaPesajes key={guiaId} guiaId={guiaId} />
      : <p>No se puede consultar esta fuente para una guía sin vehículo asignado. Esto no acredita ausencia de pesajes.</p>}
  </section>
}

function ConsultaPesajes({ guiaId }: { guiaId: number }) {
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)
  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    consultarPesajesDeGuia(guiaId, controlador.signal)
      .then((datos) => { if (vigente) setCarga({ estado: 'lista', datos }) })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudieron consultar los pesajes.' })
      })
    return () => { vigente = false; controlador.abort() }
  }, [guiaId, version])

  function volverAConsultar() {
    setCarga({ estado: 'cargando' })
    setVersion((actual) => actual + 1)
  }

  return <>
    <p>Solo lectura de registros guardados en el central. Esta consulta no sincroniza ni comprueba Balanzas en tiempo real.</p>
    {carga.estado === 'cargando' ? <p role="status">Consultando pesajes…</p>
      : carga.estado === 'error' ? <p className="mensaje-error" role="alert">{carga.mensaje}</p>
        : <DatosPesajes datos={carga.datos} />}
    <button type="button" className="boton-secundario" disabled={carga.estado === 'cargando'} onClick={volverAConsultar}>
      {carga.estado === 'error' ? 'Reintentar consulta de pesajes' : 'Volver a consultar pesajes'}
    </button>
  </>
}

function DatosPesajes({ datos }: { datos: PesajesDeGuia }) {
  return <>
    {!datos.nodoDisponible && <p role="status"><Etiqueta tono="aviso">Posibles pesajes pendientes</Etiqueta>{' '}
      El circuito de Balanzas está abierto. Puede haber pesajes pendientes de obtener; se muestran los registros guardados.</p>}
    {datos.truncado && <p role="status"><Etiqueta tono="aviso">Resultados truncados</Etiqueta>{' '}
      Se alcanzó el límite de {LIMITE_PESAJES_GUIA} registros. La respuesta no incluye todos los pesajes vinculados ni ofrece paginación.</p>}
    <p role="status">Registros recibidos: {datos.cantidad}. Su disponibilidad no garantiza que se hayan obtenido todos los pesajes del viaje.</p>
    <div className="tabla-contenedor"><table className="tabla">
      <caption>Pesadas guardadas para la guía #{datos.guiaId}, en orden de la balanza. Fechas en America/Montevideo.</caption>
      <thead><tr><th scope="col">Pesada</th><th scope="col">Matrícula registrada</th><th scope="col">Peaje</th>
        <th scope="col">Peso</th><th scope="col">Instante de balanza</th><th scope="col">Obtenida por el central</th></tr></thead>
      <tbody>{datos.pesadas.map((pesada) => <tr key={pesada.idPesada}>
        <td data-etiqueta="Pesada"><span className="celda-url">{pesada.idPesada}</span></td>
        <td data-etiqueta="Matrícula registrada"><strong>{pesada.matricula}</strong></td>
        <td data-etiqueta="Peaje"><span>{pesada.peaje || 'Sin datos'}</span></td>
        <td data-etiqueta="Peso"><span>{pesada.pesoKg.toLocaleString('es-UY')} kg</span></td>
        <td data-etiqueta="Instante de balanza"><time dateTime={pesada.instante}>{momentoViaje(pesada.instante)}</time></td>
        <td data-etiqueta="Obtenida por el central"><time dateTime={pesada.obtenidaEn}>{momentoViaje(pesada.obtenidaEn)}</time></td>
      </tr>)}
        {datos.pesadas.length === 0 && <tr><td colSpan={6} className="tabla-vacia">
          {datos.nodoDisponible ? 'No hay pesajes registrados en el central para esta guía.'
            : 'No hay registros disponibles en el central para esta guía; puede haber pesajes pendientes de obtener.'}
        </td></tr>}
      </tbody>
    </table></div>
  </>
}
