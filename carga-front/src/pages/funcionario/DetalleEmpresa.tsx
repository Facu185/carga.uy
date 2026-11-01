import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ErrorApi } from '../../api/cliente'
import { consultarEmpresas, interpretarEmpresa, type DatosEmpresaAdministrativa, type EmpresaAdministrativa } from '../../api/empresas'
import { useConfirmar } from '../../componentes/confirmaciones'
import { formatearFecha } from '../../formato'
import { useTituloPagina } from '../../titulo'
import { FormularioEmpresa, type ValoresEmpresa } from './FormularioEmpresa'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; empresa: EmpresaAdministrativa }

function valoresDe(empresa: EmpresaAdministrativa): ValoresEmpresa {
  return { nombre: empresa.nombre, razonSocial: empresa.razonSocial ?? '', numeroEmpresa: String(empresa.numeroEmpresa),
    direccion: empresa.direccion ?? '', fechaAlta: empresa.fechaAlta ?? '' }
}

export function DetalleEmpresaFuncionario() {
  const { id } = useParams()
  // El cambio de ruta descarta también borradores, confirmaciones pendientes y resultados tardíos.
  return <DetalleEmpresa key={id} idTexto={id ?? ''} />
}

function DetalleEmpresa({ idTexto }: { idTexto: string }) {
  useTituloPagina('Detalle de empresa · Área del funcionario')
  const id = Number(idTexto)
  const idValido = /^[1-9]\d*$/.test(idTexto) && Number.isSafeInteger(id)
  const confirmar = useConfirmar()
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)
  const [valores, setValores] = useState<ValoresEmpresa | null>(null)
  const [ocupado, setOcupado] = useState(false)
  const [incierto, setIncierto] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)
  const generacion = useRef(0)
  const enviando = useRef(false)
  const mensaje = useRef<HTMLParagraphElement>(null)

  useEffect(() => {
    const actual = ++generacion.current
    const controlador = new AbortController()
    setCarga({ estado: 'cargando' }); setValores(null); setIncierto(false)
    if (!idValido) {
      setCarga({ estado: 'error', mensaje: 'El identificador de empresa no es válido.' })
    } else {
      consultarEmpresas('', controlador.signal).then((empresas) => {
        if (generacion.current !== actual) return
        const empresa = empresas.find((candidata) => candidata.id === id)
        if (!empresa) throw new Error('La empresa no se encuentra en el listado disponible.')
        setCarga({ estado: 'lista', empresa })
      }).catch((causa: unknown) => {
        if (generacion.current === actual) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudo consultar la empresa.' })
      })
    }
    return () => { generacion.current++; controlador.abort() }
  }, [id, idValido, version])

  useEffect(() => { if (error || exito) mensaje.current?.focus() }, [error, exito])

  const empresa = carga.estado === 'lista' ? carga.empresa : null
  const numeroEditable = empresa && Number.isSafeInteger(empresa.numeroEmpresa) && empresa.numeroEmpresa > 0

  function reconsultar() {
    setCarga({ estado: 'cargando' }); setValores(null); setError(null)
    setVersion((valor) => valor + 1)
  }

  function registrarFallo(causa: unknown) {
    const texto = causa instanceof Error ? causa.message : 'No se pudo completar la operación.'
    if (causa instanceof ErrorApi && (causa.status === 401 || causa.status === 403)) {
      setValores(null); setCarga({ estado: 'error', mensaje: texto })
    } else if (causa instanceof ErrorApi && causa.status === 400) {
      setError(texto)
    } else {
      setIncierto(true)
      setError('No se pudo confirmar el resultado. Vuelve a consultar antes de intentar otra operación; no se reenviará automáticamente.')
    }
  }

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (enviando.current || !empresa || !valores || !numeroEditable || incierto) return
    const numero = Number(valores.numeroEmpresa)
    if (!valores.nombre.trim() || !Number.isSafeInteger(numero) || numero <= 0) {
      setExito(null); setError('Indica un nombre y un número de empresa entero positivo válido.'); return
    }
    const datos: DatosEmpresaAdministrativa = { nombre: valores.nombre.trim(), razonSocial: valores.razonSocial.trim() || null,
      numeroEmpresa: numero, direccion: valores.direccion.trim() || null, fechaAlta: valores.fechaAlta || null }
    const actual = generacion.current
    enviando.current = true; setOcupado(true); setError(null); setExito(null)
    try {
      const respuesta = await api<unknown>(`/empresas/${empresa.id}`, { method: 'PUT', body: JSON.stringify(datos) })
      if (generacion.current !== actual) return
      const actualizada = interpretarEmpresa(respuesta)
      if (actualizada.id !== empresa.id || !Number.isSafeInteger(actualizada.numeroEmpresa) || actualizada.numeroEmpresa <= 0)
        throw new Error('Respuesta de guardado no válida.')
      setCarga({ estado: 'lista', empresa: actualizada }); setValores(null); setExito('Empresa modificada.')
    } catch (causa) {
      if (generacion.current === actual) registrarFallo(causa)
    } finally {
      if (generacion.current === actual) { enviando.current = false; setOcupado(false) }
    }
  }

  async function darDeBaja() {
    if (enviando.current || !empresa?.activa || incierto || valores) return
    const actual = generacion.current
    enviando.current = true; setOcupado(true); setError(null); setExito(null)
    try {
      const confirmado = await confirmar({ titulo: 'Dar de baja la empresa', peligro: true, accion: 'Dar de baja',
        mensaje: `Se desactivará ${empresa.nombre} y se revocarán sus vínculos activos. Los datos históricos se conservarán. ¿Confirmas la baja?` })
      if (!confirmado || generacion.current !== actual) return
      await api<void>(`/empresas/${empresa.id}`, { method: 'DELETE' })
      if (generacion.current !== actual) return
      setExito('Baja registrada. Se vuelve a consultar el estado de la empresa.'); reconsultar()
    } catch (causa) {
      if (generacion.current === actual) registrarFallo(causa)
    } finally {
      if (generacion.current === actual) { enviando.current = false; setOcupado(false) }
    }
  }

  return (
    <>
      <Link to="/funcionario/empresas">Volver a empresas</Link>
      <h1>Detalle de empresa</h1>
      {error && <p id="error-empresa-funcionario" ref={mensaje} tabIndex={-1} className="mensaje-error" role="alert">{error}</p>}
      {exito && <p ref={mensaje} tabIndex={-1} role="status">{exito}</p>}
      {carga.estado === 'error' ? <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
        {idValido && <button type="button" className="boton-secundario" onClick={reconsultar}>Volver a consultar</button>}</div>
        : !empresa ? <p role="status">Consultando empresa…</p>
          : <section className="ficha ficha-ancha" aria-labelledby="titulo-empresa-funcionario">
            <h2 id="titulo-empresa-funcionario">{empresa.nombre}</h2>
            <dl className="ficha-datos">
              <div><dt>Número de empresa</dt><dd>{numeroEditable ? empresa.numeroEmpresa : 'Fuera del rango editable del portal'}</dd></div>
              <div><dt>Razón social</dt><dd>{empresa.razonSocial || 'Sin datos'}</dd></div>
              <div><dt>Dirección</dt><dd>{empresa.direccion || 'Sin datos'}</dd></div>
              <div><dt>Fecha de alta</dt><dd>{formatearFecha(empresa.fechaAlta)}</dd></div>
              <div><dt>Estado</dt><dd>{empresa.activa ? 'Activa' : 'Inactiva'}</dd></div>
            </dl>
            {!numeroEditable && <p role="alert">El número de empresa no puede editarse de forma segura en este portal. Requiere revisión administrativa por otro consumidor.</p>}
            {valores ? <FormularioEmpresa valores={valores} ocupado={ocupado} bloqueado={ocupado || incierto}
              errorId={error ? 'error-empresa-funcionario' : undefined}
              cambiar={setValores} guardar={(evento) => void guardar(evento)} cancelar={() => { setValores(null); setError(null); setExito(null) }} />
              : <div className="formulario-botones">
                <button type="button" disabled={ocupado || incierto || !numeroEditable} onClick={() => {
                  setValores(valoresDe(empresa)); setError(null); setExito(null)
                }}>Modificar empresa</button>
                <button type="button" className="boton-peligro" disabled={ocupado || incierto || !empresa.activa}
                  onClick={() => void darDeBaja()}>{ocupado ? 'Procesando…' : 'Dar de baja'}</button>
              </div>}
            {incierto && <button type="button" className="boton-secundario" onClick={reconsultar}>Volver a consultar</button>}
            <h3>Consultas relacionadas</h3>
            <p>Consultas de solo lectura. Los viajes muestran datos P2 y consulta de pesajes P4 en su detalle; el historial de eventos P3 sigue pendiente de integración.</p>
            <div className="formulario-botones">
              <Link className="boton-enlace" to={`/funcionario/empresas/${empresa.id}/vehiculos`}>Consultar vehículos, permiso e ITV</Link>
              <Link className="boton-enlace" to={`/funcionario/empresas/${empresa.id}/viajes`}>Consultar viajes</Link>
            </div>
          </section>}
    </>
  )
}
