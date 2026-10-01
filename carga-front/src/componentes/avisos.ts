import { createContext, useContext } from 'react'

/** Muestra un aviso breve de confirmación ("Funcionario habilitado"). */
export const AvisosContext = createContext<(texto: string) => void>(() => {})

export function useAviso(): (texto: string) => void {
  return useContext(AvisosContext)
}
