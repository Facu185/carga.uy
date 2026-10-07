import type { Sesion } from '../tipos'

/**
 * La sesión vive en sessionStorage: es la única fuente de verdad del token.
 * El cliente de la API la lee de acá en cada pedido, así funciona desde el primer render
 * (antes, al recargar la página, los primeros pedidos salían sin token).
 */
const CLAVE = 'cargauy.sesion'

export function leerSesionGuardada(): Sesion | null {
  try {
    const texto = sessionStorage.getItem(CLAVE)
    if (!texto) return null
    const sesion = JSON.parse(texto) as Sesion
    return new Date(sesion.expiraEn) > new Date() ? sesion : null
  } catch {
    return null
  }
}

export function guardarSesion(sesion: Sesion): void {
  sessionStorage.setItem(CLAVE, JSON.stringify(sesion))
}

export function borrarSesion(): void {
  sessionStorage.removeItem(CLAVE)
}

/**
 * Por qué terminó la última sesión. Decide a dónde va el próximo ingreso:
 * - "manual" (tocó Salir): el próximo puede ser otra persona, así que NO se la manda a la página anterior.
 * - "vencida" (el central rechazó el token): es la misma persona, vuelve a donde estaba y se le avisa.
 */
export type MotivoSalida = 'manual' | 'vencida'

const CLAVE_SALIDA = 'cargauy.motivoSalida'

export function registrarSalida(motivo: MotivoSalida): void {
  try {
    sessionStorage.setItem(CLAVE_SALIDA, motivo)
  } catch {
    // sin almacenamiento, simplemente no se recuerda el motivo
  }
}

export function motivoSalida(): MotivoSalida | null {
  try {
    const valor = sessionStorage.getItem(CLAVE_SALIDA)
    return valor === 'manual' || valor === 'vencida' ? valor : null
  } catch {
    return null
  }
}

export function olvidarSalida(): void {
  try {
    sessionStorage.removeItem(CLAVE_SALIDA)
  } catch {
    // nada que hacer
  }
}
