import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { Etiqueta } from '../../componentes/Etiqueta'
import { formatearFecha } from '../../formato'
import type { Empresa, VinculoEmpresa } from '../../tipos'

/** Permite solicitar el vínculo RESPONSABLE; su activación siempre queda sujeta a revisión del MTOP. */
export function SolicitudResponsable() {
  const [empresas, setEmpresas] = useState<Empresa[]>([])
  const [vinculos, setVinculos] = useState<VinculoEmpresa[]>([])
  const [empresaId, setEmpresaId] = useState('')
  const [abierto, setAbierto] = useState(false)
  const [cargando, setCargando] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  function recargar() {
    setCargando(true)
    Promise.all([api<Empresa[]>('/empresas'), api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')])
      .then(([listaEmpresas, listaVinculos]) => {
        setEmpresas(listaEmpresas.filter((empresa) => empresa.activa))
        setVinculos(listaVinculos.filter((vinculo) => vinculo.tipo === 'RESPONSABLE'))
      })
      .catch((causa: unknown) => setError(causa instanceof Error ? causa.message : 'No se pudo cargar la información de empresas.'))
      .finally(() => setCargando(false))
  }

  useEffect(() => { recargar() }, [])

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!empresaId) return
    setError(null)
    setEnviando(true)
    try {
      await api<VinculoEmpresa>(`/solicitudes-empresa/vinculos/${empresaId}`, { method: 'POST' })
      setEmpresaId('')
      setAbierto(false)
      recargar()
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo enviar la solicitud.')
    } finally {
      setEnviando(false)
    }
  }

  const pendientes = vinculos.filter((vinculo) => vinculo.estado === 'PENDIENTE')
  return (
    <section className="ficha" aria-labelledby="titulo-responsable">
      <div className="ficha-encabezado">
        <h2 id="titulo-responsable">Responsabilidad de empresa</h2>
        {pendientes.length > 0 && <Etiqueta tono="aviso">Solicitud pendiente</Etiqueta>}
      </div>
      <p className="ficha-texto">Solicitá administrar una empresa transportista. Un funcionario del MTOP debe validar la solicitud antes de habilitarte.</p>
      {pendientes.map((vinculo) => <p className="ficha-texto" key={vinculo.id}>Solicitud para <strong>{vinculo.nombreEmpresa}</strong>, enviada el {formatearFecha(vinculo.solicitadoEn)}.</p>)}
      {error && <p className="mensaje-error" role="alert">{error}</p>}
      {!abierto ? (
        <button type="button" className="boton-secundario" onClick={() => setAbierto(true)} disabled={cargando}>Solicitar ser responsable</button>
      ) : (
        <form onSubmit={(evento) => void enviar(evento)} className="formulario-en-linea">
          <label className="campo">
            <span>Empresa</span>
            <select value={empresaId} onChange={(evento) => setEmpresaId(evento.target.value)} required autoFocus>
              <option value="">Seleccioná una empresa</option>
              {empresas.map((empresa) => <option value={empresa.id} key={empresa.id}>{empresa.nombre}</option>)}
            </select>
          </label>
          <button type="submit" disabled={!empresaId || enviando}>{enviando ? 'Enviando…' : 'Enviar solicitud'}</button>
          <button type="button" className="boton-secundario" onClick={() => setAbierto(false)} disabled={enviando}>Cancelar</button>
        </form>
      )}
    </section>
  )
}
