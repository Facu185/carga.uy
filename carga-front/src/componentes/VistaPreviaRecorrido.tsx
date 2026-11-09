import { useEffect, useMemo, useState } from 'react'
import { calcularRuta, puntoDeCampos, type RutaPrevia, type SolicitudRuta } from '../api/planificacionGuias'
import { MapaGuia } from './MapaGuia'

interface Props {
  vehiculoId: string
  cargaKg: string
  origenLat: string
  origenLon: string
  destinoLat: string
  destinoLon: string
  revision: number
}

export function VistaPreviaRecorrido(props: Props) {
  const origen = puntoDeCampos(props.origenLat, props.origenLon)
  const destino = puntoDeCampos(props.destinoLat, props.destinoLon)
  const solicitud = useMemo<SolicitudRuta | null>(() => {
    const a = puntoDeCampos(props.origenLat, props.origenLon), b = puntoDeCampos(props.destinoLat, props.destinoLon)
    const vehiculoId = Number(props.vehiculoId), cargaKg = Number(props.cargaKg)
    return a && b && Number.isSafeInteger(vehiculoId) && vehiculoId > 0
      && props.cargaKg.trim() !== '' && Number.isFinite(cargaKg) && cargaKg > 0
      ? { origen: a, destino: b, vehiculoId, cargaKg } : null
  }, [props.origenLat, props.origenLon, props.destinoLat, props.destinoLon, props.vehiculoId, props.cargaKg])
  const clave = solicitud ? JSON.stringify([props.revision, solicitud]) : ''
  const [resultado, setResultado] = useState<{ clave: string; ruta: RutaPrevia } | null>(null)
  const [fallo, setFallo] = useState<{ clave: string; mensaje: string } | null>(null)
  const [pendiente, setPendiente] = useState<string | null>(null)
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    if (!clave || !solicitud) return
    const control = new AbortController()
    const espera = window.setTimeout(() => {
      setPendiente(clave); setFallo(null)
      calcularRuta(solicitud, control.signal)
        .then((ruta) => { if (!control.signal.aborted) setResultado({ clave, ruta }) })
        .catch((causa: unknown) => {
          if (!control.signal.aborted) setFallo({ clave, mensaje: causa instanceof Error ? causa.message : 'No se pudo calcular el recorrido.' })
        })
        .finally(() => { if (!control.signal.aborted) setPendiente(null) })
    }, 700)
    return () => { window.clearTimeout(espera); control.abort() }
  }, [clave, intento, solicitud])

  const ruta = resultado?.clave === clave && fallo?.clave !== clave ? resultado.ruta : null
  const error = fallo?.clave === clave ? fallo.mensaje : null
  const cargando = pendiente === clave && clave !== ''
  const formato = new Intl.NumberFormat('es-UY', { maximumFractionDigits: 1 })

  return <div className="vista-previa-recorrido">
    <h3>Vista previa del recorrido</h3>
    <p className="recorrido-aviso">Ruta orientativa según el peso y las restricciones registradas en el mapa. No certifica que todas las vías estén habilitadas. El trazado no se guarda con la guía.</p>
    {!solicitud && <p>Selecciona origen y destino, un vehículo y el peso de la carga para calcular el recorrido.</p>}
    {clave && !ruta && !error && <p role="status">{cargando ? 'Calculando recorrido con OpenRouteService…' : 'Preparando cálculo del recorrido…'}</p>}
    {error && <div><p role="alert" className="mensaje-error">{error}</p>
      <button type="button" className="boton-secundario boton-chico" disabled={cargando} onClick={() => setIntento((v) => v + 1)}>Reintentar recorrido</button></div>}
    {ruta && <div aria-live="polite">
      <dl className="recorrido-resumen"><div><dt>Distancia estimada</dt><dd>{formato.format(ruta.distanciaMetros / 1000)} km</dd></div>
        <div><dt>Duración estimada</dt><dd>{formato.format(ruta.duracionSegundos / 60)} min</dd></div>
        <div><dt>Peso total considerado</dt><dd>{formato.format(ruta.pesoTotalKg)} kg (vehículo + carga)</dd></div></dl>
      {ruta.advertenciasProveedor && <p className="recorrido-aviso">OpenRouteService devolvió advertencias sobre este recorrido. Revisa su viabilidad antes de viajar.</p>}
    </div>}
    <MapaGuia origen={origen} destino={destino} puntos={ruta?.puntos ?? null} />
    <p className="recorrido-coordenadas">Origen: {origen ? `${origen.latitud}, ${origen.longitud}` : 'sin ubicación'}.
      {' '}Destino: {destino ? `${destino.latitud}, ${destino.longitud}` : 'sin ubicación'}.</p>
    <p className="ficha-texto">Si el mapa o los servicios no están disponibles, puedes conservar las direcciones e ingresar coordenadas manualmente. El registro mantiene sus validaciones habituales.</p>
  </div>
}
