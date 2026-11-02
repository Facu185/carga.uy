import { useState, type FormEvent } from 'react'
import { validarEmpresa, validarVehiculo, type EstadoVigencia, type ValidacionEmpresa, type ValidacionVehiculo } from '../../api/publico'
import { Etiqueta, type TonoEtiqueta } from '../../componentes/Etiqueta'
import { formatearFecha } from '../../formato'
import { useTituloPagina } from '../../titulo'
import { PaginaPublica } from './PaginaPublica'

type Modo = 'vehiculo' | 'empresa'
type Resultado = { modo: 'vehiculo'; datos: ValidacionVehiculo } | { modo: 'empresa'; datos: ValidacionEmpresa }

const VIGENCIA: Record<EstadoVigencia, [string, TonoEtiqueta]> = {
  VIGENTE: ['Vigente', 'ok'],
  FUTURO: ['Todavía no empieza', 'aviso'],
  VENCIDO: ['Vencido', 'error'],
  SIN_DATOS: ['Sin datos registrados', 'neutro'],
}

function Vigencia({ estado }: { estado: EstadoVigencia | null }) {
  const [texto, tono] = VIGENCIA[estado ?? 'SIN_DATOS']
  return <Etiqueta tono={tono}>{texto}</Etiqueta>
}

function FichaVehiculo({ datos }: { datos: ValidacionVehiculo }) {
  if (!datos.encontrado) {
    return <section className="ficha ficha-ancha" aria-live="polite">
      <h2>{datos.matricula}</h2>
      <p>No hay un vehículo de carga registrado con esa matrícula.</p>
    </section>
  }
  return <section className="ficha ficha-ancha" aria-live="polite">
    <div className="ficha-encabezado">
      <h2>{datos.matricula}</h2>
      {datos.vigente ? <Etiqueta tono="ok">Puede circular</Etiqueta> : <Etiqueta tono="error">No está habilitado</Etiqueta>}
    </div>
    <dl className="ficha-datos">
      <div><dt>Permiso Nacional de Circulación</dt><dd><Vigencia estado={datos.estadoPermiso} />
        {datos.permisoHasta && <span className="celda-secundaria">Hasta el {formatearFecha(datos.permisoHasta)}</span>}</dd></div>
      <div><dt>Inspección Técnica Vehicular</dt><dd><Vigencia estado={datos.estadoItv} />
        {datos.itvVencimiento && <span className="celda-secundaria">Vence el {formatearFecha(datos.itvVencimiento)}</span>}</dd></div>
      <div><dt>Vehículo</dt><dd>{datos.vehiculoHabilitado ? 'Habilitado' : 'Dado de baja'}</dd></div>
      <div><dt>Empresa</dt><dd>{datos.empresaHabilitada ? 'Habilitada' : 'Dada de baja'}</dd></div>
    </dl>
    <p className="ficha-nota">Situación al {formatearFecha(datos.fechaReferencia)}. Por privacidad no se informa a qué empresa pertenece ni quién lo maneja.</p>
  </section>
}

function FichaEmpresa({ datos }: { datos: ValidacionEmpresa }) {
  return <section className="ficha ficha-ancha" aria-live="polite">
    {datos.encontrada ? <>
      <div className="ficha-encabezado">
        <h2>{datos.nombre}</h2>
        {datos.habilitada ? <Etiqueta tono="ok">Habilitada</Etiqueta> : <Etiqueta tono="error">Dada de baja</Etiqueta>}
      </div>
      <p>Empresa de transporte de carga N.º {datos.numeroEmpresa}, situación al {formatearFecha(datos.fechaReferencia)}.</p>
    </> : <>
      <h2>Empresa N.º {datos.numeroEmpresa}</h2>
      <p>No hay una empresa de transporte de carga registrada con ese número.</p>
    </>}
  </section>
}

export function ValidarPermiso() {
  useTituloPagina('Validar un permiso')
  const [modo, setModo] = useState<Modo>('vehiculo')
  const [valor, setValor] = useState('')
  const [resultado, setResultado] = useState<Resultado | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [consultando, setConsultando] = useState(false)

  async function consultar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setConsultando(true)
    setError(null)
    setResultado(null)
    try {
      setResultado(modo === 'vehiculo'
        ? { modo, datos: await validarVehiculo(valor) }
        : { modo, datos: await validarEmpresa(valor) })
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo consultar. Intentá de nuevo.')
    } finally {
      setConsultando(false)
    }
  }

  return <PaginaPublica>
    <header className="seccion-encabezado">
      <div>
        <h1>Validar un permiso</h1>
        <p>Comprobá si un vehículo de carga puede circular o si una empresa de transporte está habilitada. No hace falta ingresar.</p>
      </div>
    </header>
    <section className="ficha ficha-ancha">
      <form onSubmit={(evento) => void consultar(evento)} className="publico-formulario">
        <fieldset className="publico-modo">
          <legend>¿Qué querés validar?</legend>
          <label><input type="radio" name="modo" checked={modo === 'vehiculo'} onChange={() => { setModo('vehiculo'); setResultado(null) }} /> Un vehículo, por matrícula</label>
          <label><input type="radio" name="modo" checked={modo === 'empresa'} onChange={() => { setModo('empresa'); setResultado(null) }} /> Una empresa, por número de empresa</label>
        </fieldset>
        <label className="campo">
          <span>{modo === 'vehiculo' ? 'Matrícula' : 'Número de empresa'}</span>
          <input value={valor} onChange={(evento) => setValor(evento.target.value)} required
            inputMode={modo === 'empresa' ? 'numeric' : 'text'} autoComplete="off"
            placeholder={modo === 'vehiculo' ? 'Ej.: STZ 1234' : 'Ej.: 1001'} />
        </label>
        <div className="formulario-botones"><button type="submit" disabled={consultando || !valor.trim()}>{consultando ? 'Consultando…' : 'Validar'}</button></div>
      </form>
    </section>
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    {resultado?.modo === 'vehiculo' && <FichaVehiculo datos={resultado.datos} />}
    {resultado?.modo === 'empresa' && <FichaEmpresa datos={resultado.datos} />}
  </PaginaPublica>
}
