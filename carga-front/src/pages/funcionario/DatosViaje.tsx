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

export function FuentesViajePendientes() {
  return <section aria-label="Fuentes pendientes de integración">
    <h2>Información pendiente de integración</h2>
    <p><strong>Historial real del viaje:</strong> pendiente de contrato e integración con P3.</p>
    <p><strong>Pesajes asociados a la guía:</strong> pendiente de contrato e integración con P4.</p>
    <p>Estos mensajes no significan que el viaje carezca de eventos o pesajes ni que un nodo esté caído.
      Esta entrega muestra únicamente datos registrados de P2, no seguimiento en tiempo real.</p>
  </section>
}
