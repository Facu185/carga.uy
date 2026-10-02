import { useState } from 'react'

/**
 * Dependencias del MTOP que usan carga.uy: la que fiscaliza el transporte de carga y la de rutas.
 * "Otra" deja escribir cualquier otra, así nadie queda trabado.
 */
const DEPENDENCIAS_MTOP = ['Dirección Nacional de Transporte', 'Dirección Nacional de Vialidad'] as const

const OTRA = '__otra__'

export function SelectorDependencia({
  valor,
  onChange,
  etiqueta = 'Dependencia',
  requerido = false,
  id,
}: {
  valor: string
  onChange: (valor: string) => void
  etiqueta?: string
  requerido?: boolean
  id?: string
}) {
  const esDeLaLista = (DEPENDENCIAS_MTOP as readonly string[]).includes(valor)
  // Si el valor guardado no está en la lista (se cargó como "Otra"), se abre directamente en modo texto
  const [escribiendoOtra, setEscribiendoOtra] = useState(valor !== '' && !esDeLaLista)

  return (
    <div className="selector-dependencia">
      <label className="campo">
        <span>{etiqueta}</span>
        <select
          id={id}
          value={escribiendoOtra ? OTRA : valor}
          required={requerido && !escribiendoOtra}
          onChange={(e) => {
            if (e.target.value === OTRA) {
              setEscribiendoOtra(true)
              onChange('')
            } else {
              setEscribiendoOtra(false)
              onChange(e.target.value)
            }
          }}
        >
          <option value="" disabled>
            Elegí una dependencia
          </option>
          {DEPENDENCIAS_MTOP.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
          <option value={OTRA}>Otra (escribirla)</option>
        </select>
      </label>
      {escribiendoOtra && (
        <label className="campo">
          <span>¿Cuál?</span>
          <input
            value={valor}
            onChange={(e) => onChange(e.target.value)}
            placeholder="Nombre de la dependencia"
            maxLength={150}
            required={requerido}
            autoFocus
          />
        </label>
      )}
    </div>
  )
}
