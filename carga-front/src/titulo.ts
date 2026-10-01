import { useEffect } from 'react'

/**
 * Título de la pestaña del navegador para cada pantalla: "Auditoría | Backoffice carga.uy".
 * Con varias pestañas abiertas (o en el historial) se distingue cada una.
 */
export function useTituloPagina(titulo: string, backoffice = false): void {
  useEffect(() => {
    document.title = titulo
      ? `${titulo} | ${backoffice ? 'Backoffice carga.uy' : 'carga.uy'}`
      : 'carga.uy | Transporte de carga en Uruguay'
  }, [titulo, backoffice])
}
