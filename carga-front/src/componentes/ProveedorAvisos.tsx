import { CircleCheck } from 'lucide-react'
import { useCallback, useState, type ReactNode } from 'react'
import { AvisosContext } from './avisos'

interface Aviso {
  id: number
  texto: string
}

const DURACION_MS = 4000

/** Avisos que aparecen abajo a la derecha y se van solos. Los lectores de pantalla los anuncian. */
export function ProveedorAvisos({ children }: { children: ReactNode }) {
  const [avisos, setAvisos] = useState<Aviso[]>([])

  const mostrar = useCallback((texto: string) => {
    const id = Date.now() + Math.random()
    setAvisos((actuales) => [...actuales, { id, texto }])
    window.setTimeout(() => setAvisos((actuales) => actuales.filter((a) => a.id !== id)), DURACION_MS)
  }, [])

  return (
    <AvisosContext.Provider value={mostrar}>
      {children}
      <div className="avisos" role="status" aria-live="polite">
        {avisos.map((a) => (
          <div key={a.id} className="aviso-flotante">
            <CircleCheck size={18} aria-hidden="true" className="aviso-flotante-icono" />
            {a.texto}
          </div>
        ))}
      </div>
    </AvisosContext.Provider>
  )
}
