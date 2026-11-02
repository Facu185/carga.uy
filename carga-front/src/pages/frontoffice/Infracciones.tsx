import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { infracciones, presentarDescargo, TIPOS, type Caso, type PaginaCasos } from '../../api/fiscalizacion'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { EstadoDelCaso, FichaCaso } from '../../componentes/FichaCaso'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearMomento } from '../../formato'
import type { VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

/** Formulario del descargo, dentro del plazo. Uno por caso. */
function FormularioDescargo({ empresaId, caso, alPresentar }: { empresaId: string; caso: Caso; alPresentar: (c: Caso) => void }) {
  const [texto, setTexto] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setEnviando(true)
    setError(null)
    try {
      alPresentar(await presentarDescargo(empresaId, caso.id, texto))
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo presentar el descargo.')
    } finally {
      setEnviando(false)
    }
  }

  return <form onSubmit={(evento) => void enviar(evento)} className="publico-formulario">
    <label className="campo"><span>Tu descargo</span>
      <textarea value={texto} onChange={(evento) => setTexto(evento.target.value)} rows={4} required minLength={10}
        placeholder="Explicá por qué la infracción no corresponde. Un funcionario del MTOP lo va a revisar antes de resolver." />
    </label>
    {caso.venceDescargo && <p className="ficha-nota">Podés presentarlo hasta el {formatearMomento(caso.venceDescargo)}. Se admite un solo descargo por caso.</p>}
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    <div className="formulario-botones"><button type="submit" disabled={enviando || texto.trim().length < 10}>Presentar descargo</button></div>
  </form>
}

/** Infracciones detectadas sobre los vehículos y viajes de la empresa, con la evidencia y el descargo (P5). */
export function Infracciones() {
  useTituloPagina('Infracciones de la empresa')
  const { logout, sesion } = useAuth()
  const [empresas, setEmpresas] = useState<VinculoEmpresa[] | null>(null)
  const [empresaId, setEmpresaId] = useState('')
  const [datos, setDatos] = useState<PaginaCasos | null>(null)
  const [abierto, setAbierto] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then((vinculos) => {
        const responsables = vinculos.filter((v) => v.tipo === 'RESPONSABLE' && v.estado === 'ACTIVO')
        setEmpresas(responsables)
        setEmpresaId(responsables[0] ? String(responsables[0].empresaId) : '')
      })
      .catch((causa: unknown) => setError(causa instanceof Error ? causa.message : 'No se pudieron cargar tus empresas.'))
  }, [])

  useEffect(() => {
    if (!empresaId) return
    const controlador = new AbortController()
    setDatos(null)
    infracciones(empresaId, 0, controlador.signal).then(setDatos).catch((causa: unknown) => {
      if (!controlador.signal.aborted) setError(causa instanceof Error ? causa.message : 'No se pudieron cargar las infracciones.')
    })
    return () => controlador.abort()
  }, [empresaId])

  function actualizar(c: Caso) {
    setDatos((anterior) => anterior && { ...anterior, registros: anterior.registros.map((x) => (x.id === c.id ? c : x)) })
  }

  return <div className="pagina">
    <EncabezadoSitio>
      <span className="sitio-usuario">{sesion?.nombre}</span>
      <button type="button" className="boton-secundario boton-chico" onClick={logout}>Salir</button>
    </EncabezadoSitio>
    <main className="pagina-contenido empresa-contenido">
      <NavegacionEmpresa />
      <header className="seccion-encabezado">
        <div><h1>Infracciones</h1><p>Posibles incumplimientos que el MTOP detectó sobre los vehículos y viajes de tu empresa. Podés ver la evidencia y presentar tu descargo dentro del plazo.</p></div>
      </header>
      {error && <p className="mensaje-error" role="alert">{error}</p>}
      {empresas && empresas.length === 0 && <p>No sos responsable activo de ninguna empresa.</p>}
      {empresas && empresas.length > 1 && <label className="campo"><span>Empresa</span>
        <select value={empresaId} onChange={(evento) => setEmpresaId(evento.target.value)}>
          {empresas.map((e) => <option key={e.empresaId} value={e.empresaId}>{e.nombreEmpresa}</option>)}
        </select>
      </label>}
      {empresaId && !datos && !error && <p role="status">Cargando las infracciones…</p>}
      {datos && datos.registros.length === 0 && <p className="ficha-nota">Tu empresa no tiene infracciones detectadas.</p>}
      {datos?.registros.map((c) => <article key={c.id} className="ficha ficha-ancha">
        <div className="ficha-encabezado">
          <h2>#{c.id} · {TIPOS[c.tipo]} · {c.matricula}</h2>
          <EstadoDelCaso caso={c} />
        </div>
        <p className="ficha-nota">Detectado el {formatearMomento(c.detectadaEn)}.{c.admiteDescargo && ' Podés presentar tu descargo.'}</p>
        <button type="button" className="boton-secundario boton-chico" aria-expanded={abierto === c.id}
          onClick={() => setAbierto(abierto === c.id ? null : c.id)}>{abierto === c.id ? 'Ocultar detalle' : 'Ver evidencia'}</button>
        {abierto === c.id && <>
          <FichaCaso caso={c} />
          {c.admiteDescargo && <FormularioDescargo empresaId={empresaId} caso={c} alPresentar={actualizar} />}
        </>}
      </article>)}
    </main>
    <PieSitio />
  </div>
}
