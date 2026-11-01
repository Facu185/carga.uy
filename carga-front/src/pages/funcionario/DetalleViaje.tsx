import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { consultarDetalleGuia, identificadorValido, type ConsultaGuia } from '../../api/consultaGuias'
import { useTituloPagina } from '../../titulo'
import { EstadoGuiaPersistido, HistorialViajePendiente, momentoViaje } from './DatosViaje'
import { PesajesViaje } from './PesajesViaje'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string } | { estado: 'lista'; datos: ConsultaGuia }

export function DetalleViajeFuncionario() {
  const { id = '', guiaId = '' } = useParams()
  return <DetalleViaje key={`${id}:${guiaId}`} empresaTexto={id} guiaTexto={guiaId} />
}

function DetalleViaje({ empresaTexto, guiaTexto }: { empresaTexto: string; guiaTexto: string }) {
  useTituloPagina('Detalle de viaje · Área del funcionario')
  const { search } = useLocation()
  const empresaId = Number(empresaTexto), guiaId = Number(guiaTexto)
  const valido = identificadorValido(empresaTexto) && identificadorValido(guiaTexto)
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)
  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    if (!valido) setCarga({ estado: 'error', mensaje: 'La empresa y la guía deben tener identificadores positivos válidos.' })
    else consultarDetalleGuia(empresaId, guiaId, controlador.signal)
      .then((datos) => { if (vigente) setCarga({ estado: 'lista', datos }) })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudo consultar el viaje.' })
      })
    return () => { vigente = false; controlador.abort() }
  }, [empresaId, guiaId, valido, version])

  const datos = carga.estado === 'lista' ? carga.datos : null
  const guia = datos?.guia
  return <>
    <Link to={identificadorValido(empresaTexto) ? `/funcionario/empresas/${empresaId}/viajes${search}` : '/funcionario/empresas'}>Volver a viajes</Link>
    <h1>Detalle de viaje</h1>
    {carga.estado === 'error' ? <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
      {valido && <button type="button" className="boton-secundario" onClick={() => {
        setCarga({ estado: 'cargando' }); setVersion((actual) => actual + 1)
      }}>Volver a consultar</button>}</div>
      : !datos || !guia ? <p role="status">Consultando viaje…</p>
        : <section className="ficha ficha-ancha" aria-labelledby="titulo-viaje">
          <h2 id="titulo-viaje">Guía #{guia.id} · {datos.nombreEmpresa}</h2>
          <p>Solo lectura. Datos relacionados actuales, no una instantánea histórica. Fechas en America/Montevideo.</p>
          <dl className="ficha-datos">
            <div><dt>Empresa</dt><dd>{datos.nombreEmpresa} (#{guia.empresaId})</dd></div>
            <div><dt>Vehículo</dt><dd>{guia.vehiculoId == null ? 'Sin vehículo asignado' : `${datos.matricula || 'Sin matrícula disponible'} (#${guia.vehiculoId})`}</dd></div>
            <div><dt>Chofer</dt><dd>{guia.choferId == null ? 'Sin chofer asignado' : `${datos.nombreChofer || 'Sin nombre disponible'} (#${guia.choferId})`}</dd></div>
            <div><dt>Origen</dt><dd>{guia.origen}</dd></div><div><dt>Destino</dt><dd>{guia.destino}</dd></div>
            <div><dt>Inicio previsto</dt><dd>{momentoViaje(guia.inicio)}</dd></div>
            <div><dt>Fin previsto (exclusivo)</dt><dd>{momentoViaje(guia.fin)}</dd></div>
            <div><dt>Peso declarado</dt><dd>{guia.pesoKg.toLocaleString('es-UY')} kg</dd></div>
            <div><dt>Volumen declarado</dt><dd>{guia.volumenM3 == null ? 'Sin datos' : `${guia.volumenM3.toLocaleString('es-UY')} m³`}</dd></div>
            <div><dt>Rubro</dt><dd>{guia.rubroId == null ? 'Sin datos' : `#${guia.rubroId}`}</dd></div>
            <div><dt>Tipo de carga</dt><dd>{guia.tipoCargaId == null ? 'Sin datos' : `#${guia.tipoCargaId}`}</dd></div>
            <div><dt>Guía creada</dt><dd>{momentoViaje(guia.creadaEn)}</dd></div>
            <div><dt>Estado persistido</dt><dd><EstadoGuiaPersistido estado={guia.estado} /></dd></div>
          </dl>
          <p>El estado persistido no sustituye el historial de eventos de P3. El período previsto no acredita inicio ni fin reales.</p>
        </section>}
    {guia && <PesajesViaje key={guia.id} guiaId={guia.id} vehiculoAsignado={guia.vehiculoId != null} />}
    <HistorialViajePendiente />
  </>
}
