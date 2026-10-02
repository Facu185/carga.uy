import type { ReactNode } from 'react'

export type TonoEtiqueta = 'ok' | 'aviso' | 'error' | 'neutro'

/** Estado corto y de un vistazo (verificado, pendiente, rechazado...). El color nunca va solo: siempre con texto. */
export function Etiqueta({ tono, children }: { tono: TonoEtiqueta; children: ReactNode }) {
  return <span className={`etiqueta etiqueta-${tono}`}>{children}</span>
}
