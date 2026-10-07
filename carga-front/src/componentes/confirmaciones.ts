import { createContext, useContext } from 'react'

export interface OpcionesConfirmacion {
  titulo: string
  mensaje: string
  /** Texto del botón que confirma: la acción concreta ("Dar de baja"), nunca "Aceptar". */
  accion: string
  /** La acción no se puede deshacer o corta un acceso: el botón va en rojo. */
  peligro?: boolean
}

/** Pide confirmación con el diálogo propio del sitio. Devuelve true si la persona confirma. */
export const ConfirmacionesContext = createContext<(opciones: OpcionesConfirmacion) => Promise<boolean>>(
  (opciones) => Promise.resolve(window.confirm(opciones.mensaje)),
)

export function useConfirmar(): (opciones: OpcionesConfirmacion) => Promise<boolean> {
  return useContext(ConfirmacionesContext)
}
