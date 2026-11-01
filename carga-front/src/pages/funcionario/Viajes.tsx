import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { consultarGuias, esEstadoGuia, ESTADOS_GUIA, identificadorValido, type PaginaConsultaGuias } from '../../api/consultaGuias'
import { useTituloPagina } from '../../titulo'
import { EstadoGuiaPersistido, HistorialViajePendiente, momentoViaje, NOMBRES_ESTADO_GUIA } from './DatosViaje'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; datos: PaginaConsultaGuias }

export function ViajesFuncionario() {
  const { id = '' } = useParams()
  const [parametros] = useSearchParams()
  const desde = parametros.get('desde') ?? '', hasta = parametros.get('hasta') ?? '', estado = (parametros.get('estado') ?? '').trim()
  const paginaTexto = parametros.get('pagina') ?? '0', tamanoTexto = parametros.get('tamano') ?? '20'
  const empresaId = Number(id), pagina = Number(paginaTexto), tamano = Number(tamanoTexto)
  const valido = identificadorValido(id)
    && /^\d+$/.test(paginaTexto) && Number.isSafeInteger(pagina) && pagina >= 0
    && /^\d+$/.test(tamanoTexto) && Number.isSafeInteger(tamano) && tamano >= 1 && tamano <= 100
    && pagina * tamano <= 2147483647
  // Cambiar URL descarta la carga anterior antes de dibujar el encabezado nuevo.
  return <ConsultaViajes key={`${id}:${parametros.toString()}`} empresaId={empresaId} desde={desde} hasta={hasta}
    estado={estado} pagina={pagina} tamano={tamano} valido={valido} />
}

function ConsultaViajes({ empresaId, desde, hasta, estado, pagina, tamano, valido }: {
  empresaId: number; desde: string; hasta: string; estado: string; pagina: number; tamano: number; valido: boolean
}) {
  useTituloPagina('Viajes · Área del funcionario')
  const [parametros, setParametros] = useSearchParams()
  const [desdeElegido, setDesdeElegido] = useState(desde), [hastaElegido, setHastaElegido] = useState(hasta)
  const [estadoElegido, setEstadoElegido] = useState(estado), [tamanoElegido, setTamanoElegido] = useState(String(tamano))
  const [errorFiltro, setErrorFiltro] = useState<string | null>(null)
  const mensajeFiltro = useRef<HTMLParagraphElement>(null)
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    if (!valido) setCarga({ estado: 'error', mensaje: 'El identificador de empresa o la paginación no son válidos.' })
    else consultarGuias(empresaId, desde, hasta, estado, pagina, tamano, controlador.signal)
      .then((datos) => { if (vigente) setCarga({ estado: 'lista', datos }) })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudieron consultar los viajes.' })
      })
    return () => { vigente = false; controlador.abort() }
  }, [empresaId, desde, hasta, estado, pagina, tamano, valido, version])

  useEffect(() => { if (errorFiltro) mensajeFiltro.current?.focus() }, [errorFiltro])

  function navegar(numero: number, inicio = desde, fin = hasta, filtroEstado = estado, cantidad = tamano) {
    setCarga({ estado: 'cargando' }); setErrorFiltro(null)
    const nuevos = new URLSearchParams({ pagina: String(numero), tamano: String(cantidad) })
    if (inicio) nuevos.set('desde', inicio)
    if (fin) nuevos.set('hasta', fin)
    if (filtroEstado) nuevos.set('estado', filtroEstado)
    setParametros(nuevos); setVersion((actual) => actual + 1)
  }

  function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const inicio = desdeElegido.trim(), fin = hastaElegido.trim(), cantidad = Number(tamanoElegido)
    if (!!inicio !== !!fin) { setErrorFiltro('Completa ambos límites del período o deja ambos vacíos.'); return }
    if (!Number.isSafeInteger(cantidad) || cantidad < 1 || cantidad > 100) {
      setErrorFiltro('Indica entre 1 y 100 viajes por página.'); return
    }
    navegar(0, inicio, fin, estadoElegido, cantidad)
  }

  function reconsultar() { setCarga({ estado: 'cargando' }); setVersion((actual) => actual + 1) }
  const datos = carga.estado === 'lista' ? carga.datos : null
  const base = `/funcionario/empresas/${empresaId}/viajes`
  const busqueda = parametros.toString() ? `?${parametros}` : ''
  return <>
    <Link to={Number.isSafeInteger(empresaId) && empresaId > 0 ? `/funcionario/empresas/${empresaId}` : '/funcionario/empresas'}>Volver a empresa</Link>
    <h1>Consulta de viajes</h1>
    <p>Empresa {Number.isSafeInteger(empresaId) && empresaId > 0 ? `#${empresaId}` : 'no válida'} · Solo lectura.</p>
    <p>El período es previsto y el estado es el persistido. El fin previsto pasado NO acredita que un viaje haya finalizado.
      Fechas mostradas en America/Montevideo.</p>
    {valido && <form className="formulario-grilla" onSubmit={buscar}>
      <p id="ayuda-periodo-viajes">Período opcional: completa ambos límites ISO-8601 con zona (por ejemplo, 2026-10-27T09:00:00-03:00).
        Incluye viajes que se solapan con el período; el límite final es exclusivo.</p>
      <label className="campo"><span>Desde (con zona)</span><input name="desde" value={desdeElegido}
        aria-describedby={`ayuda-periodo-viajes${errorFiltro ? ' error-filtro-viajes' : ''}`} onChange={(evento) => setDesdeElegido(evento.target.value)} /></label>
      <label className="campo"><span>Hasta (con zona)</span><input name="hasta" value={hastaElegido}
        aria-describedby={`ayuda-periodo-viajes${errorFiltro ? ' error-filtro-viajes' : ''}`} onChange={(evento) => setHastaElegido(evento.target.value)} /></label>
      <label className="campo"><span>Estado persistido</span><select name="estado" value={estadoElegido}
        onChange={(evento) => setEstadoElegido(evento.target.value)}>
        <option value="">Todos</option>
        {estadoElegido && !esEstadoGuia(estadoElegido) && <option value={estadoElegido}>Estado no válido</option>}
        {ESTADOS_GUIA.map((valor) => <option key={valor} value={valor}>{NOMBRES_ESTADO_GUIA[valor]}</option>)}
      </select></label>
      <label className="campo"><span>Viajes por página</span><input name="tamano" type="number" min={1} max={100} step={1}
        required value={tamanoElegido} aria-describedby={errorFiltro ? 'error-filtro-viajes' : undefined}
        onChange={(evento) => setTamanoElegido(evento.target.value)} /></label>
      {errorFiltro && <p id="error-filtro-viajes" ref={mensajeFiltro} tabIndex={-1} className="mensaje-error" role="alert">{errorFiltro}</p>}
      <div className="formulario-botones"><button type="submit">Buscar</button>
        <button type="button" className="boton-secundario" onClick={() => {
          setDesdeElegido(''); setHastaElegido(''); setEstadoElegido(''); navegar(0, '', '', '')
        }}>Limpiar filtros</button></div>
    </form>}
    {carga.estado === 'error' ? <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
      {valido && <button type="button" className="boton-secundario" onClick={reconsultar}>Volver a consultar</button>}</div>
      : !datos ? <p role="status">Consultando viajes…</p>
        : <>
          <div className="tabla-contenedor"><table className="tabla">
            <caption>Viajes de la empresa #{empresaId}, ordenados por inicio previsto e identificador descendentes</caption>
            <thead><tr><th scope="col">Guía / empresa</th><th scope="col">Asignación actual</th><th scope="col">Trayecto y período previsto</th>
              <th scope="col">Carga declarada</th><th scope="col">Estado persistido</th></tr></thead>
            <tbody>{datos.registros.map(({ guia, nombreEmpresa, matricula, nombreChofer }) => <tr key={guia.id}>
              <td data-etiqueta="Guía / empresa"><Link to={`${base}/${guia.id}${busqueda}`}>Guía #{guia.id}</Link>
                <span className="celda-secundaria">{nombreEmpresa}</span></td>
              <td data-etiqueta="Asignación actual">{matricula || (guia.vehiculoId ? `Vehículo #${guia.vehiculoId}` : 'Sin vehículo asignado')}
                <span className="celda-secundaria">{nombreChofer || (guia.choferId ? `Chofer #${guia.choferId}` : 'Sin chofer asignado')}</span></td>
              <td data-etiqueta="Trayecto y período previsto">{guia.origen} → {guia.destino}
                <span className="celda-secundaria">Desde {momentoViaje(guia.inicio)} hasta {momentoViaje(guia.fin)}</span></td>
              <td data-etiqueta="Carga declarada">{guia.pesoKg.toLocaleString('es-UY')} kg
                <span className="celda-secundaria">{guia.volumenM3 == null ? 'Volumen sin datos' : `${guia.volumenM3.toLocaleString('es-UY')} m³`}</span></td>
              <td data-etiqueta="Estado persistido"><EstadoGuiaPersistido estado={guia.estado} /></td>
            </tr>)}
              {datos.registros.length === 0 && <tr><td colSpan={5} className="tabla-vacia">
                {datos.total === 0 ? 'No hay viajes para esta consulta.' : 'No hay viajes en esta página. Puedes volver a la anterior.'}
              </td></tr>}
            </tbody>
          </table></div>
          <nav className="paginacion" aria-label="Páginas de viajes">
            <p className="paginacion-resumen" role="status">{datos.total} viajes encontrados</p>
            <div className="acciones"><button type="button" className="boton-secundario" disabled={pagina === 0}
              onClick={() => navegar(pagina - 1)}>Anterior</button>
              <span className="paginacion-numero">Página {pagina + 1} · {Math.max(1, Math.ceil(datos.total / tamano))} páginas con resultados</span>
              <button type="button" className="boton-secundario" disabled={(pagina + 1) * tamano >= datos.total || (pagina + 1) * tamano > 2147483647}
                onClick={() => navegar(pagina + 1)}>Siguiente</button></div>
          </nav>
          <button type="button" className="boton-secundario" onClick={reconsultar}>Actualizar consulta</button>
        </>}
    <p>La consulta de pesajes asociados está disponible en el detalle de cada viaje con vehículo asignado.</p>
    <HistorialViajePendiente />
  </>
}
