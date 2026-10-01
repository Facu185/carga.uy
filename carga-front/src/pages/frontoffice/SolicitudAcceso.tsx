import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { Etiqueta } from '../../componentes/Etiqueta'
import { SelectorDependencia } from '../../componentes/SelectorDependencia'
import { formatearFecha } from '../../formato'
import type { SolicitudFuncionario } from '../../tipos'

/**
 * Acceso como funcionario del MTOP, desde "Mi cuenta" de un ciudadano.
 * Discreto a propósito: la gran mayoría son choferes o personal de empresas, que no tienen que pedir nada
 * (los asocia un funcionario). Si ya pidió acceso, muestra en qué quedó.
 */
export function SolicitudAcceso() {
  const [abierto, setAbierto] = useState(false)
  const [solicitud, setSolicitud] = useState<SolicitudFuncionario | null>(null)
  const [cargando, setCargando] = useState(true)
  const [dependencia, setDependencia] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let vigente = true
    // 204 (nunca pidió) llega como undefined
    api<SolicitudFuncionario | undefined>('/solicitudes-funcionario/mia')
      .then((s) => vigente && setSolicitud(s ?? null))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'Error'))
      .finally(() => vigente && setCargando(false))
    return () => {
      vigente = false
    }
  }, [])

  async function enviar(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      setSolicitud(
        await api<SolicitudFuncionario>('/solicitudes-funcionario', {
          method: 'POST',
          body: JSON.stringify({ dependencia }),
        }),
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo enviar la solicitud')
    } finally {
      setEnviando(false)
    }
  }

  if (cargando) {
    return null
  }

  const formulario = (
    <form onSubmit={(e) => void enviar(e)} className="formulario-en-linea">
      <SelectorDependencia valor={dependencia} onChange={setDependencia} etiqueta="Dependencia donde trabajás" requerido />
      <button type="submit" disabled={enviando || !dependencia.trim()}>
        {enviando ? 'Enviando…' : 'Pedir acceso'}
      </button>
    </form>
  )
  const mensajeError = error && (
    <p className="mensaje-error" role="alert">
      {error}
    </p>
  )

  // Ya pidió acceso como funcionario: se muestra en qué quedó
  if (solicitud) {
    return (
      <section className="ficha" aria-labelledby="titulo-solicitud">
        <div className="ficha-encabezado">
          <h2 id="titulo-solicitud">Tu acceso como funcionario del MTOP</h2>
          {solicitud.estado === 'PENDIENTE' && <Etiqueta tono="aviso">Pendiente de aprobación</Etiqueta>}
          {solicitud.estado === 'APROBADA' && <Etiqueta tono="ok">Aprobada</Etiqueta>}
          {solicitud.estado === 'RECHAZADA' && <Etiqueta tono="error">Rechazada</Etiqueta>}
        </div>
        {solicitud.estado === 'PENDIENTE' && (
          <p className="ficha-texto">
            Pediste acceso para <strong>{solicitud.dependencia}</strong> el {formatearFecha(solicitud.creadaEn)}. Un
            administrador la tiene que aprobar.
          </p>
        )}
        {solicitud.estado === 'APROBADA' && (
          <p className="ficha-texto">
            Ya tenés acceso como funcionario. Salí y volvé a ingresar con gub.uy para empezar a usarlo.
          </p>
        )}
        {solicitud.estado === 'RECHAZADA' && (
          <>
            <p className="ficha-texto">
              Motivo: {solicitud.motivoRechazo}. Si corresponde, podés pedirlo de nuevo.
            </p>
            {formulario}
          </>
        )}
        {mensajeError}
      </section>
    )
  }

  // Nunca pidió acceso: una línea discreta que abre el formulario
  if (!abierto) {
    return (
      <p className="nota-seccion acceso-funcionario">
        ¿Sos funcionario del MTOP?{' '}
        <button type="button" className="boton-texto" onClick={() => setAbierto(true)}>
          Pedir acceso como funcionario
        </button>
      </p>
    )
  }
  return (
    <section className="ficha" aria-labelledby="titulo-solicitud-nueva">
      <h2 id="titulo-solicitud-nueva">Acceso como funcionario del MTOP</h2>
      <p className="ficha-texto">
        Solo para personal del ministerio. Un administrador revisa la solicitud con tus datos de gub.uy.
      </p>
      {formulario}
      <p className="nota-seccion">
        <button type="button" className="boton-texto" onClick={() => setAbierto(false)}>
          Cancelar
        </button>
      </p>
      {mensajeError}
    </section>
  )
}
