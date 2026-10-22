import { useEffect, useState } from 'react'
import { api } from '../../api/cliente'
import { useAviso } from '../../componentes/avisos'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { formatearCedula, formatearFecha } from '../../formato'
import type { VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

type Accion = 'rechazar' | 'transferir' | null

/** Bandeja para que funcionarios y administradores resuelvan solicitudes de responsables. */
export function SolicitudesEmpresa({ backoffice = true }: { backoffice?: boolean }) {
  useTituloPagina(backoffice ? 'Solicitudes de empresas' : 'Solicitudes de responsables · Área del funcionario', backoffice)
  const avisar = useAviso()
  const [solicitudes, setSolicitudes] = useState<VinculoEmpresa[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [accion, setAccion] = useState<{ id: number; tipo: Exclude<Accion, null> } | null>(null)
  const [motivo, setMotivo] = useState('')
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let vigente = true
    setCargando(true)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos?estado=PENDIENTE')
      .then((lista) => { if (vigente) setSolicitudes(lista.filter((vinculo) => vinculo.tipo === 'RESPONSABLE')) })
      .catch((causa: unknown) => { if (vigente) setError(causa instanceof Error ? causa.message : 'No se pudieron cargar las solicitudes.') })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [version])

  async function ejecutar(ruta: string, mensaje: string, cuerpo?: object) {
    setError(null)
    try {
      await api<VinculoEmpresa>(ruta, { method: 'POST', body: cuerpo ? JSON.stringify(cuerpo) : undefined })
      setAccion(null)
      setMotivo('')
      setVersion((actual) => actual + 1)
      avisar(mensaje)
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo resolver la solicitud.')
    }
  }

  return (
    <>
      <EncabezadoSeccion titulo="Solicitudes de responsables" descripcion="Revisá y aprobá las solicitudes para administrar empresas transportistas." />
      {error && <p className="mensaje-error" role="alert">{error}</p>}
      {cargando && <p className="nota-seccion">Cargando solicitudes…</p>}
      {!cargando && solicitudes.length === 0 && <p className="nota-seccion">No hay solicitudes de responsables pendientes.</p>}
      {!cargando && solicitudes.length > 0 && (
        <section className="ficha ficha-ancha ficha-destacada" aria-label="Solicitudes pendientes">
          <ul className="lista-solicitudes">
            {solicitudes.map((solicitud) => {
              const accionActual = accion?.id === solicitud.id ? accion : null
              return <li key={solicitud.id}>
                <div className="solicitud-datos">
                  <strong>{solicitud.nombreSolicitante ?? 'Persona solicitante'}</strong>
                  {solicitud.cedulaSolicitante && <span>Cédula {formatearCedula(solicitud.cedulaSolicitante)}</span>}
                  <span>Empresa: {solicitud.nombreEmpresa}</span>
                  <span>Solicitada el {formatearFecha(solicitud.solicitadoEn)}</span>
                </div>
                {accionActual ? (
                  <div className="solicitud-rechazo">
                    <label className="campo"><span>{accionActual.tipo === 'transferir' ? 'Motivo de la transferencia' : 'Motivo del rechazo'}</span><input value={motivo} onChange={(evento) => setMotivo(evento.target.value)} maxLength={500} autoFocus required /></label>
                    <button type="button" onClick={() => void ejecutar(`/solicitudes-empresa/vinculos/${solicitud.id}/${accionActual.tipo}`, accionActual.tipo === 'transferir' ? 'Responsabilidad transferida' : 'Solicitud rechazada', { motivo })} disabled={!motivo.trim()}>{accionActual.tipo === 'transferir' ? 'Transferir' : 'Rechazar'}</button>
                    <button type="button" className="boton-secundario" onClick={() => { setAccion(null); setMotivo('') }}>Cancelar</button>
                  </div>
                ) : (
                  <div className="acciones">
                    <button type="button" onClick={() => void ejecutar(`/solicitudes-empresa/vinculos/${solicitud.id}/aprobar`, 'Solicitud aprobada')}>Aprobar</button>
                    <button type="button" className="boton-secundario" onClick={() => setAccion({ id: solicitud.id, tipo: 'transferir' })}>Transferir responsabilidad</button>
                    <button type="button" className="boton-secundario" onClick={() => setAccion({ id: solicitud.id, tipo: 'rechazar' })}>Rechazar</button>
                  </div>
                )}
              </li>
            })}
          </ul>
        </section>
      )}
    </>
  )
}
