import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { ConfirmacionesContext, type OpcionesConfirmacion } from './confirmaciones'

/**
 * Diálogo de confirmación con la identidad del sitio, en lugar del cuadro gris del navegador.
 * Usa el elemento <dialog> nativo: deja el resto de la página inerte, atrapa el foco adentro,
 * se cierra con Esc y devuelve el foco al botón que lo abrió.
 * El foco arranca en "Cancelar": así un Enter apurado no ejecuta una acción peligrosa.
 */
export function ProveedorConfirmaciones({ children }: { children: ReactNode }) {
  const dialogo = useRef<HTMLDialogElement>(null)
  const resolver = useRef<((confirmado: boolean) => void) | null>(null)
  const [opciones, setOpciones] = useState<OpcionesConfirmacion | null>(null)

  const confirmar = useCallback(
    (nuevas: OpcionesConfirmacion) =>
      new Promise<boolean>((resolve) => {
        resolver.current?.(false) // si había otra pendiente, se cancela
        resolver.current = resolve
        setOpciones(nuevas)
      }),
    [],
  )

  useEffect(() => {
    if (opciones && dialogo.current && !dialogo.current.open) {
      dialogo.current.showModal()
    }
  }, [opciones])

  function cerrar(confirmado: boolean) {
    resolver.current?.(confirmado)
    resolver.current = null
    dialogo.current?.close()
    setOpciones(null)
  }

  return (
    <ConfirmacionesContext.Provider value={confirmar}>
      {children}
      <dialog
        ref={dialogo}
        className={opciones?.peligro ? 'dialogo dialogo-peligro' : 'dialogo'}
        aria-labelledby="dialogo-titulo"
        aria-describedby="dialogo-mensaje"
        onCancel={(e) => {
          e.preventDefault()
          cerrar(false)
        }}
        onClick={(e) => {
          // Un clic en el fondo oscuro (fuera del contenido) cancela
          if (e.target === dialogo.current) cerrar(false)
        }}
      >
        {opciones && (
          <div className="dialogo-contenido">
            <h2 id="dialogo-titulo">{opciones.titulo}</h2>
            <p id="dialogo-mensaje">{opciones.mensaje}</p>
            <div className="dialogo-acciones">
              <button type="button" className="boton-secundario" onClick={() => cerrar(false)} autoFocus>
                Cancelar
              </button>
              <button
                type="button"
                className={opciones.peligro ? 'boton-peligro' : undefined}
                onClick={() => cerrar(true)}
              >
                {opciones.accion}
              </button>
            </div>
          </div>
        )}
      </dialog>
    </ConfirmacionesContext.Provider>
  )
}
