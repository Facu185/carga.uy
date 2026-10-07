import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/contexto'
import { INGRESO_GUBUY } from '../../auth/gubuy'
import { PantallaAcceso } from '../../componentes/PantallaAcceso'
import type { Rol, Sesion } from '../../tipos'
import { useTituloPagina } from '../../titulo'

/** Lee lo que dejó el central en el fragmento (#) al volver de gub.uy. */
function leerFragmento(): { sesion: Sesion | null; error: string | null; cancelado: boolean } {
  const datos = new URLSearchParams(window.location.hash.slice(1))
  if (datos.get('cancelado')) {
    return { sesion: null, error: null, cancelado: true }
  }
  const error = datos.get('error')
  const token = datos.get('token')
  if (error || !token) {
    return { sesion: null, error: error ?? 'No se recibió la sesión de gub.uy', cancelado: false }
  }
  return {
    sesion: {
      token,
      expiraEn: datos.get('expiraEn') ?? '',
      usuario: datos.get('usuario') ?? '',
      nombre: datos.get('nombre') ?? '',
      rol: datos.get('rol') as Rol,
      roles: (datos.get('roles')?.split(',').filter(Boolean) ?? [datos.get('rol')]) as Rol[],
    },
    error: null,
    cancelado: false,
  }
}

export function IngresoCallback() {
  useTituloPagina('Ingresando')
  const { establecerSesion } = useAuth()
  const navegar = useNavigate()
  const [resultado] = useState(leerFragmento)

  useEffect(() => {
    // Borra el token de la barra de direcciones y del historial
    window.history.replaceState(null, '', window.location.pathname)
    if (resultado.sesion) {
      establecerSesion(resultado.sesion)
      navegar('/mi-cuenta', { replace: true })
    } else if (resultado.cancelado) {
      navegar('/', { replace: true })
    }
  }, [resultado, establecerSesion, navegar])

  if (resultado.error) {
    return (
      <PantallaAcceso titulo="No se pudo ingresar">
        <p className="mensaje-error" role="alert">
          {resultado.error}
        </p>
        <a className="boton-enlace boton-bloque" href={INGRESO_GUBUY}>
          Volver a intentar
        </a>
      </PantallaAcceso>
    )
  }
  return <PantallaAcceso titulo="Ingresando…" descripcion="Verificando tu identidad con gub.uy." />
}
