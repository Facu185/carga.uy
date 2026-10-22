import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { consultarVehiculos, type EstadoPermiso, type PaginaConsultaVehiculos } from '../../api/consultaVehiculos'
import { Etiqueta } from '../../componentes/Etiqueta'
import { formatearFecha } from '../../formato'
import { useTituloPagina } from '../../titulo'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; datos: PaginaConsultaVehiculos }

const ESTADOS: Record<EstadoPermiso, string> = {
  VIGENTE: 'Vigente', FUTURO: 'Futuro', VENCIDO: 'Vencido', SIN_DATOS: 'Sin datos completos',
}

function Vigencia({ estado }: { estado: EstadoPermiso }) {
  return <Etiqueta tono={estado === 'VIGENTE' ? 'ok' : estado === 'VENCIDO' ? 'error' : 'aviso'}>{ESTADOS[estado]}</Etiqueta>
}

export function VehiculosFuncionario() {
  const { id = '' } = useParams()
  const [parametros] = useSearchParams()
  const matricula = (parametros.get('matricula') ?? '').trim()
  const paginaTexto = parametros.get('pagina') ?? '0'
  const tamanoTexto = parametros.get('tamano') ?? '20'
  const empresaId = Number(id), pagina = Number(paginaTexto), tamano = Number(tamanoTexto)
  const valido = /^[1-9]\d*$/.test(id) && Number.isSafeInteger(empresaId)
    && /^\d+$/.test(paginaTexto) && Number.isSafeInteger(pagina) && pagina >= 0
    && /^\d+$/.test(tamanoTexto) && Number.isSafeInteger(tamano) && tamano >= 1 && tamano <= 100
    && pagina * tamano <= 2147483647
  return <ConsultaVehiculos key={`${id}:${matricula}:${paginaTexto}:${tamanoTexto}`}
    empresaId={empresaId} matricula={matricula} pagina={pagina} tamano={tamano} valido={valido} />
}

function ConsultaVehiculos({ empresaId, matricula, pagina, tamano, valido }: {
  empresaId: number; matricula: string; pagina: number; tamano: number; valido: boolean
}) {
  useTituloPagina('Vehículos · Área del funcionario')
  const [, setParametros] = useSearchParams()
  const [borrador, setBorrador] = useState(matricula)
  const [tamanoElegido, setTamanoElegido] = useState(String(tamano))
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setCarga({ estado: 'cargando' })
    if (!valido) setCarga({ estado: 'error', mensaje: 'El identificador de empresa o la paginación no son válidos.' })
    else consultarVehiculos(empresaId, matricula, pagina, tamano, controlador.signal)
      .then((datos) => { if (vigente) setCarga({ estado: 'lista', datos }) })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudieron consultar los vehículos.' })
      })
    return () => { vigente = false; controlador.abort() }
  }, [empresaId, matricula, pagina, tamano, valido, version])

  function navegar(numero: number, filtro = matricula, cantidad = tamano) {
    setCarga({ estado: 'cargando' })
    const parametros = new URLSearchParams({ pagina: String(numero), tamano: String(cantidad) })
    if (filtro) parametros.set('matricula', filtro)
    setParametros(parametros)
    setVersion((actual) => actual + 1)
  }

  function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    const cantidad = Number(tamanoElegido)
    if (!Number.isSafeInteger(cantidad) || cantidad < 1 || cantidad > 100) return
    navegar(0, borrador.trim(), cantidad)
  }

  const datos = carga.estado === 'lista' ? carga.datos : null
  return (
    <>
      <Link to={Number.isSafeInteger(empresaId) && empresaId > 0 ? `/funcionario/empresas/${empresaId}` : '/funcionario/empresas'}>
        Volver a empresa
      </Link>
      <h1>Consulta de vehículos</h1>
      <p>Empresa {Number.isSafeInteger(empresaId) && empresaId > 0 ? `#${empresaId}` : 'no válida'} · Solo lectura, incluidos los vehículos dados de baja.</p>
      <p>La habilitación registrada, la vigencia del permiso y la ITV se muestran por separado. Esta pantalla no modifica vehículos ni asignaciones de guías.</p>
      {valido && <form className="formulario-grilla" onSubmit={buscar}>
        <label className="campo"><span>Matrícula (coincidencia parcial)</span>
          <input name="matricula" value={borrador} onChange={(evento) => setBorrador(evento.target.value)} /></label>
        <label className="campo"><span>Vehículos por página</span>
          <input name="tamano" type="number" min={1} max={100} step={1} required value={tamanoElegido}
            onChange={(evento) => setTamanoElegido(evento.target.value)} /></label>
        <div className="formulario-botones"><button type="submit">Buscar</button>
          <button type="button" className="boton-secundario" onClick={() => { setBorrador(''); navegar(0, '') }}>Limpiar filtro</button></div>
      </form>}
      {carga.estado === 'error' ? <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
        {valido && <button type="button" className="boton-secundario" onClick={() => {
          setCarga({ estado: 'cargando' }); setVersion((actual) => actual + 1)
        }}>Volver a consultar</button>}</div>
        : !datos ? <p role="status">Consultando vehículos…</p>
          : <>
            <p role="status">Vigencias al {formatearFecha(datos.fechaReferencia)} ({datos.zonaHoraria}), según el servidor.</p>
            <div className="tabla-contenedor"><table className="tabla">
              <caption>Vehículos de la empresa #{empresaId}, ordenados por matrícula e identificador</caption>
              <thead><tr><th scope="col">Matrícula</th><th scope="col">Habilitación registrada</th>
                <th scope="col">Permiso de circulación</th><th scope="col">ITV</th></tr></thead>
              <tbody>{datos.registros.map((vehiculo) => <tr key={vehiculo.id}>
                <td data-etiqueta="Matrícula"><strong>{vehiculo.matricula}</strong></td>
                <td data-etiqueta="Habilitación registrada"><Etiqueta tono={vehiculo.habilitado ? 'neutro' : 'aviso'}>
                  {vehiculo.habilitado ? 'Habilitado en el registro' : 'Baja lógica'}</Etiqueta></td>
                <td data-etiqueta="Permiso de circulación"><strong>{vehiculo.numeroPermiso || 'Sin número'}</strong>
                  <span className="celda-secundaria">Desde {formatearFecha(vehiculo.permisoDesde)} hasta {formatearFecha(vehiculo.permisoHasta)}</span>
                  <Vigencia estado={vehiculo.estadoPermiso} /></td>
                <td data-etiqueta="ITV"><span className="celda-secundaria">Vencimiento: {formatearFecha(vehiculo.itvVencimiento)}</span>
                  <Vigencia estado={vehiculo.estadoItv} /></td>
              </tr>)}
                {datos.registros.length === 0 && <tr><td colSpan={4} className="tabla-vacia">
                  {datos.total === 0 ? 'No hay vehículos para esta consulta.' : 'No hay vehículos en esta página. Puedes volver a la anterior.'}
                </td></tr>}
              </tbody>
            </table></div>
            <nav className="paginacion" aria-label="Páginas de vehículos">
              <p className="paginacion-resumen" role="status">{datos.total} vehículos encontrados</p>
              <div className="acciones">
                <button type="button" className="boton-secundario" disabled={pagina === 0} onClick={() => navegar(pagina - 1)}>Anterior</button>
                <span className="paginacion-numero">Página {pagina + 1} · {Math.max(1, Math.ceil(datos.total / tamano))} páginas con resultados</span>
                <button type="button" className="boton-secundario"
                  disabled={(pagina + 1) * tamano >= datos.total || (pagina + 1) * tamano > 2147483647}
                  onClick={() => navegar(pagina + 1)}>Siguiente</button>
              </div>
            </nav>
            <p>Los límites de vigencia son inclusivos. «Sin datos completos» no significa baja ni permiso vencido. Para actualizar la fecha y los datos, vuelve a consultar.</p>
            <button type="button" className="boton-secundario" onClick={() => {
              setCarga({ estado: 'cargando' }); setVersion((actual) => actual + 1)
            }}>Actualizar consulta</button>
          </>}
    </>
  )
}
