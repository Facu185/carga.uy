import { useEffect, useState, type FormEvent } from 'react'
import { modificarParametro, parametros, type ParametroRegla } from '../../api/fiscalizacion'
import { useAviso } from '../../componentes/avisos'
import { EncabezadoSeccion } from '../../componentes/EncabezadoSeccion'
import { FilasCargando } from '../../componentes/FilasCargando'
import { useTituloPagina } from '../../titulo'

const NOMBRES: Record<string, string> = {
  'exceso_carga.tolerancia_kg': 'Exceso de carga: tolerancia (kg)',
  'desvio_ruta.tolerancia_m': 'Desvío de ruta: ancho mínimo del corredor (m)',
  'desvio_ruta.tolerancia_relativa_pct': 'Desvío de ruta: ancho del corredor (% de la distancia)',
  'desvio_ruta.porcentaje_puntos': 'Desvío de ruta: posiciones fuera del corredor (%)',
  'descargo.plazo_dias': 'Plazo para el descargo (días)',
}

/** Parametrización de las reglas de fiscalización sin redesplegar (Administrador, P5, ADR-0022). */
export function ReglasFiscalizacion() {
  useTituloPagina('Reglas de fiscalización', true)
  const avisar = useAviso()
  const [lista, setLista] = useState<ParametroRegla[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [editando, setEditando] = useState<string | null>(null)
  const [valor, setValor] = useState('')
  const [motivo, setMotivo] = useState('')

  useEffect(() => {
    parametros().then(setLista).catch((causa: unknown) => setError(causa instanceof Error ? causa.message : 'No se pudieron cargar los parámetros.'))
  }, [])

  async function guardar(evento: FormEvent, clave: string) {
    evento.preventDefault()
    setError(null)
    try {
      const nuevo = await modificarParametro(clave, valor, motivo)
      setLista((anterior) => anterior && anterior.map((p) => (p.clave === clave ? nuevo : p)))
      setEditando(null)
      avisar('Parámetro actualizado: rige desde la próxima evaluación')
    } catch (causa) {
      setError(causa instanceof Error ? causa.message : 'No se pudo guardar el parámetro.')
    }
  }

  return <>
    <EncabezadoSeccion titulo="Reglas de fiscalización"
      descripcion="Umbrales con que la plataforma detecta incumplimientos. Un cambio rige desde la próxima evaluación, sin redesplegar, y queda en la auditoría con su motivo." />
    {error && <p className="mensaje-error" role="alert">{error}</p>}
    <div className="tabla-contenedor">
      <table className="tabla" aria-busy={!lista}>
        <thead><tr><th>Parámetro</th><th>Valor</th><th><span className="solo-lectores">Acciones</span></th></tr></thead>
        <tbody>
          {!lista && !error && <FilasCargando columnas={3} />}
          {lista?.map((p) => <tr key={p.clave}>
            <td data-etiqueta="Parámetro"><strong>{NOMBRES[p.clave] ?? p.clave}</strong><span className="celda-secundaria">{p.descripcion}</span></td>
            <td data-etiqueta="Valor">{editando === p.clave
              ? <form id={`form-${p.clave}`} onSubmit={(evento) => void guardar(evento, p.clave)} className="edicion-en-linea">
                <input aria-label="Nuevo valor" value={valor} onChange={(evento) => setValor(evento.target.value)} inputMode="decimal" required />
                <input aria-label="Motivo del cambio" placeholder="Motivo del cambio" value={motivo} onChange={(evento) => setMotivo(evento.target.value)} required minLength={10} />
              </form>
              : p.valor}</td>
            <td data-etiqueta="" className="celda-acciones">{editando === p.clave
              ? <div className="acciones">
                <button type="submit" form={`form-${p.clave}`} className="boton-chico" disabled={motivo.trim().length < 10}>Guardar</button>
                <button type="button" className="boton-chico boton-secundario" onClick={() => setEditando(null)}>Cancelar</button>
              </div>
              : <button type="button" className="boton-chico boton-secundario" onClick={() => { setEditando(p.clave); setValor(p.valor); setMotivo('') }}>Modificar</button>}
            </td>
          </tr>)}
        </tbody>
      </table>
    </div>
  </>
}
