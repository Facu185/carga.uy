import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { caso as consultarCaso, resolver, type Caso } from '../../api/fiscalizacion'
import { useAviso } from '../../componentes/avisos'
import { FichaCaso } from '../../componentes/FichaCaso'
import { formatearMomento } from '../../formato'
import { useTituloPagina } from '../../titulo'

/** Detalle del caso con su evidencia y el descargo, y la resolución fundada del funcionario. */
export function DetalleCasoFuncionario() {
  useTituloPagina('Caso de fiscalización · Área del funcionario')
  const { id = '' } = useParams()
  const avisar = useAviso()
  const [caso, setCaso] = useState<Caso | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [fundamento, setFundamento] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    const controlador = new AbortController()
    consultarCaso(id, controlador.signal).then(setCaso).catch((causa: unknown) => {
      if (!controlador.signal.aborted) setError(causa instanceof Error ? causa.message : 'No se pudo cargar el caso.')
    })
    return () => controlador.abort()
  }, [id])

  async function resolverCaso(confirmar: boolean, evento?: FormEvent) {
    evento?.preventDefault()
    if (!caso) return
    setEnviando(true)
    setError(null)
    try {
      setCaso(await resolver(caso.id, confirmar, fundamento))
      setFundamento('')
      avisar(confirmar ? 'Infracción confirmada' : 'Caso desestimado')
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo resolver el caso.')
    } finally {
      setEnviando(false)
    }
  }

  const pendiente = caso && (caso.estado === 'NOTIFICADA' || caso.estado === 'EN_DESCARGO')
  // Confirmar sin escuchar a la empresa, solo vencido el plazo (el central lo vuelve a controlar)
  const puedeConfirmar = caso && (caso.estado === 'EN_DESCARGO' || caso.plazoVencido)
  return <>
    <Link to="/funcionario/casos">Volver a la bandeja</Link>
    <h1>Caso de fiscalización</h1>
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    {!caso && !error && <p role="status">Cargando el caso…</p>}
    {caso && <FichaCaso caso={caso} />}
    {caso && pendiente && <section className="ficha ficha-ancha" aria-labelledby="titulo-resolver">
      <h2 id="titulo-resolver">Resolver</h2>
      {!puedeConfirmar && caso.venceDescargo && <p className="ficha-nota">
        La empresa puede presentar su descargo hasta el {formatearMomento(caso.venceDescargo)}. Hasta entonces se puede desestimar, pero no confirmar.
      </p>}
      <form onSubmit={(evento) => void resolverCaso(true, evento)} className="publico-formulario">
        <label className="campo"><span>Fundamento de la resolución</span>
          <textarea value={fundamento} onChange={(evento) => setFundamento(evento.target.value)} rows={4} required minLength={10}
            placeholder="Qué se verificó y por qué se confirma o se desestima. Queda en el caso y en la auditoría." />
        </label>
        <div className="formulario-botones">
          <button type="submit" disabled={enviando || !puedeConfirmar || fundamento.trim().length < 10}>Confirmar infracción</button>
          <button type="button" className="boton-secundario" disabled={enviando || fundamento.trim().length < 10}
            onClick={() => void resolverCaso(false)}>Desestimar</button>
        </div>
      </form>
    </section>}
  </>
}
