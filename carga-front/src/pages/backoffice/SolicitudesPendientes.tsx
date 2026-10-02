import { useEffect, useState } from 'react'
import { api } from '../../api/cliente'
import { useAviso } from '../../componentes/avisos'
import { formatearCedula, formatearFecha } from '../../formato'
import type { SolicitudFuncionario } from '../../tipos'

/** Bandeja de pedidos de acceso: cédula, nombre y correo vienen verificados por gub.uy. */
export function SolicitudesPendientes({ alResolver }: { alResolver: () => void }) {
  const avisar = useAviso()
  const [solicitudes, setSolicitudes] = useState<SolicitudFuncionario[]>([])
  const [rechazando, setRechazando] = useState<number | null>(null)
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)

  useEffect(() => {
    let vigente = true
    api<SolicitudFuncionario[]>('/solicitudes-funcionario?estado=PENDIENTE')
      .then((lista) => vigente && setSolicitudes(lista))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudieron cargar las solicitudes'))
    return () => {
      vigente = false
    }
  }, [version])

  async function ejecutar(accion: () => Promise<unknown>, mensaje: string) {
    setError(null)
    try {
      await accion()
      setRechazando(null)
      setMotivo('')
      setVersion((v) => v + 1)
      alResolver()
      avisar(mensaje)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo completar la acción')
    }
  }

  const aprobar = (s: SolicitudFuncionario) =>
    void ejecutar(
      () => api(`/solicitudes-funcionario/${s.id}/aprobar`, { method: 'POST' }),
      `Acceso aprobado para ${s.nombre ?? formatearCedula(s.cedula)}`,
    )

  const confirmarRechazo = (s: SolicitudFuncionario) =>
    void ejecutar(
      () => api(`/solicitudes-funcionario/${s.id}/rechazar`, { method: 'POST', body: JSON.stringify({ motivo }) }),
      'Solicitud rechazada',
    )

  if (solicitudes.length === 0 && !error) {
    return null
  }

  return (
    <section className="ficha ficha-ancha ficha-destacada" aria-labelledby="titulo-solicitudes">
      <h2 id="titulo-solicitudes">
        {solicitudes.length === 1 ? '1 solicitud de acceso pendiente' : `${solicitudes.length} solicitudes de acceso pendientes`}
      </h2>
      <p className="ficha-texto">Cédula, nombre y correo vienen verificados por gub.uy. La dependencia la indicó la persona.</p>
      {error && (
        <p className="mensaje-error" role="alert">
          {error}
        </p>
      )}
      <ul className="lista-solicitudes">
        {solicitudes.map((s) => (
          <li key={s.id}>
            <div className="solicitud-datos">
              <strong>{s.nombre ?? 'Sin nombre informado'}</strong>
              <span>Cédula {formatearCedula(s.cedula)}</span>
              {s.email && <span>{s.email}</span>}
              <span>
                {s.dependencia}, pedido el {formatearFecha(s.creadaEn)}
              </span>
            </div>
            {rechazando === s.id ? (
              <div className="solicitud-rechazo">
                <label className="campo">
                  <span>Motivo del rechazo</span>
                  <input
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                    maxLength={500}
                    placeholder="Ej.: No figura en la nómina del MTOP"
                    autoFocus
                  />
                </label>
                <div className="acciones">
                  <button type="button" onClick={() => confirmarRechazo(s)} disabled={!motivo.trim()}>
                    Rechazar solicitud
                  </button>
                  <button type="button" className="boton-secundario" onClick={() => setRechazando(null)}>
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <div className="acciones">
                <button type="button" onClick={() => aprobar(s)}>
                  Aprobar
                </button>
                <button type="button" className="boton-secundario" onClick={() => setRechazando(s.id)}>
                  Rechazar
                </button>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
