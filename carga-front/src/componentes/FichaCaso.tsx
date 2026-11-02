import { ESTADOS, TIPOS, type Caso } from '../api/fiscalizacion'
import { formatearMomento } from '../formato'
import { Etiqueta } from './Etiqueta'

/** Etiqueta del estado del caso. El color nunca va solo: siempre con texto. */
export function EstadoDelCaso({ caso }: { caso: Caso }) {
  const [texto, tono] = ESTADOS[caso.estado]
  return <span className="etiquetas">
    <Etiqueta tono={tono}>{texto}</Etiqueta>
    {caso.plazoVencido && <Etiqueta tono="neutro">Plazo de descargo vencido</Etiqueta>}
  </span>
}

/** Evidencia, plazos, descargo y resolución de un caso. La comparten el funcionario y la empresa. */
export function FichaCaso({ caso }: { caso: Caso }) {
  return <section className="ficha ficha-ancha" aria-labelledby={`titulo-caso-${caso.id}`}>
    <div className="ficha-encabezado">
      <h2 id={`titulo-caso-${caso.id}`}>Caso #{caso.id} · {TIPOS[caso.tipo]}</h2>
      <EstadoDelCaso caso={caso} />
    </div>
    <dl className="ficha-datos">
      {caso.empresaNombre && <div><dt>Empresa</dt><dd>{caso.empresaNombre}</dd></div>}
      <div><dt>Vehículo</dt><dd>{caso.matricula ?? '—'}</dd></div>
      <div><dt>Guía</dt><dd>{caso.guiaId ? `#${caso.guiaId}` : 'Sin guía'}</dd></div>
      <div><dt>Detectado</dt><dd>{formatearMomento(caso.detectadaEn)}</dd></div>
      {caso.venceDescargo && <div><dt>Plazo de descargo</dt><dd>Hasta el {formatearMomento(caso.venceDescargo)}</dd></div>}
    </dl>
    <h3>Evidencia</h3>
    <p className="ficha-texto">{caso.evidencia ?? 'Sin evidencia registrada.'}</p>
    {caso.descargo && <>
      <h3>Descargo de la empresa</h3>
      <p className="ficha-texto">{caso.descargo.fundamento}</p>
      <p className="ficha-nota">Presentado el {formatearMomento(caso.descargo.presentadoEn)}.</p>
    </>}
    {caso.resueltaEn && <>
      <h3>Resolución</h3>
      <p className="ficha-texto">{caso.fundamento}</p>
      <p className="ficha-nota">{caso.estado === 'CONFIRMADA' ? 'Infracción confirmada' : 'Caso desestimado'} el {formatearMomento(caso.resueltaEn)}.</p>
    </>}
  </section>
}
