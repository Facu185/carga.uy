import { borrarSesion, leerSesionGuardada } from '../auth/almacenamiento'

const BASE = '/carga/api'

export class ErrorApi extends Error {
  readonly status: number

  constructor(status: number, mensaje: string) {
    super(mensaje)
    this.status = status
  }
}

let avisarSesionExpirada: () => void = () => {}

/** El AuthProvider se suscribe para cerrar la sesión en pantalla cuando el central responde 401. */
export function alExpirarLaSesion(accion: () => void): () => void {
  avisarSesionExpirada = accion
  return () => {
    avisarSesionExpirada = () => {}
  }
}

/** ID de correlación de cada pedido: el central lo lleva a sus logs y a los sistemas que llama. */
function nuevoIdCorrelacion(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}

export async function api<T>(ruta: string, opciones: RequestInit = {}): Promise<T> {
  const headers = new Headers(opciones.headers)
  const idCorrelacion = nuevoIdCorrelacion()
  headers.set('X-Correlation-Id', idCorrelacion)
  if (opciones.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  const token = leerSesionGuardada()?.token ?? null
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let respuesta: Response
  try {
    respuesta = await fetch(BASE + ruta, { ...opciones, headers })
  } catch {
    throw new ErrorApi(0, 'No se pudo conectar con el servidor. Revisá tu conexión e intentá de nuevo.')
  }

  if (respuesta.status === 401 && token) {
    borrarSesion()
    avisarSesionExpirada()
  }
  if (!respuesta.ok) {
    // Con la referencia, un administrador encuentra en los logs exactamente qué pasó en este pedido
    const referencia = (respuesta.headers.get('X-Correlation-Id') ?? idCorrelacion).slice(0, 8)
    let mensaje =
      respuesta.status >= 500
        ? `El servidor no pudo completar la operación. Intentá de nuevo en unos segundos (referencia ${referencia}).`
        : `No se pudo completar la operación (código ${respuesta.status}).`
    try {
      const cuerpo = await respuesta.json()
      // El central responde {"error": "..."}
      const texto = cuerpo?.error ?? cuerpo?.mensaje
      if (texto) mensaje = texto
    } catch {
      // la respuesta no era JSON; nos quedamos con el mensaje genérico
    }
    throw new ErrorApi(respuesta.status, mensaje)
  }
  if (respuesta.status === 204) {
    return undefined as T
  }
  return (await respuesta.json()) as T
}

/** Descarga autenticada: conserva el Bearer y devuelve el archivo sin parsearlo como JSON. */
export async function descargarArchivo(ruta: string): Promise<Blob> {
  const headers = new Headers({ 'X-Correlation-Id': nuevoIdCorrelacion() })
  const token = leerSesionGuardada()?.token
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const respuesta = await fetch(BASE + ruta, { headers })
  if (respuesta.status === 401 && token) { borrarSesion(); avisarSesionExpirada() }
  if (!respuesta.ok) {
    let mensaje = `No se pudo descargar el archivo (código ${respuesta.status}).`
    try { mensaje = (await respuesta.json())?.error ?? mensaje } catch { /* respuesta no JSON */ }
    throw new ErrorApi(respuesta.status, mensaje)
  }
  return respuesta.blob()
}
