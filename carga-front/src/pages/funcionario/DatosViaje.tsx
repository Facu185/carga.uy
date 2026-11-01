import type { EstadoGuia } from '../../api/consultaGuias'
import { Etiqueta } from '../../componentes/Etiqueta'

export const NOMBRES_ESTADO_GUIA: Record<EstadoGuia, string> = {
  CREADA: 'Creada', ASIGNADA: 'Asignada', EN_CURSO: 'En curso', FINALIZADA: 'Finalizada',
}

export function EstadoGuiaPersistido({ estado }: { estado: EstadoGuia }) {
  return <Etiqueta tono="neutro">{NOMBRES_ESTADO_GUIA[estado]}</Etiqueta>
}

/** La zona de presentacion es explicita; no se calcula estado a partir de estas fechas. */
export function momentoViaje(iso: string): string {
  return new Date(iso).toLocaleString('es-UY', { timeZone: 'America/Montevideo', dateStyle: 'short', timeStyle: 'short' })
}

export function HistorialViajePendiente() {
  return <section aria-label="Historial pendiente de integración">
    <h2>Historial real del viaje pendiente</h2>
    <p>La lectura de eventos para el funcionario sigue pendiente de contrato e integración con P3.</p>
    <p>Esto no significa que el viaje carezca de eventos. El estado persistido no sustituye el historial ni acredita seguimiento en tiempo real.</p>
  </section>
}
