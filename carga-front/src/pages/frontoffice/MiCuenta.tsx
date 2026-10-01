import { Truck, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearCedula, formatearFecha, nombreRol } from '../../formato'
import { ROLES, type Perfil } from '../../tipos'
import { SolicitudAcceso } from './SolicitudAcceso'
import { useTituloPagina } from '../../titulo'

function textoEdad(perfil: Perfil): string {
  if (perfil.verificacionDnic === 'PENDIENTE') return 'Sin verificar'
  if (perfil.mayorDeEdad === null) return 'DNIC no informó la fecha de nacimiento'
  return perfil.mayorDeEdad ? 'Mayor de edad' : 'Menor de edad'
}

/** Punto de entrada del frontoffice. P2 y P5 cuelgan sus pantallas de acá. */
export function MiCuenta() {
  useTituloPagina('Mi cuenta')
  const { sesion, logout } = useAuth()
  const [perfil, setPerfil] = useState<Perfil | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let vigente = true
    api<Perfil>('/mi-perfil')
      .then((p) => vigente && setPerfil(p))
      .catch((err: unknown) => vigente && setError(err instanceof Error ? err.message : 'No se pudo cargar tu perfil'))
    return () => {
      vigente = false
    }
  }, [])

  const verificado = perfil?.verificacionDnic === 'VERIFICADO'

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}>
          Salir
        </button>
      </EncabezadoSitio>

      <main className="pagina-contenido">
        <h1 className="saludo">Hola, {sesion?.nombre}</h1>
        <p className="saludo-rol">{nombreRol(sesion?.rol)}</p>

        {error && (
          <p className="mensaje-error" role="alert">
            {error}
          </p>
        )}

        {perfil && (
          <section className="ficha" aria-labelledby="titulo-datos">
            <div className="ficha-encabezado">
              <h2 id="titulo-datos" className="titulo-con-icono">
                <UserRound size={22} aria-hidden="true" />
                Tus datos
              </h2>
              {verificado ? (
                <Etiqueta tono="ok">Verificado con DNIC</Etiqueta>
              ) : (
                <Etiqueta tono="aviso">Verificación pendiente</Etiqueta>
              )}
            </div>
            <dl className="ficha-datos">
              <div>
                <dt>Cédula</dt>
                <dd>{formatearCedula(perfil.cedula)}</dd>
              </div>
              <div>
                <dt>Fecha de nacimiento</dt>
                <dd>{formatearFecha(perfil.fechaNacimiento)}</dd>
              </div>
              <div>
                <dt>Correo</dt>
                <dd>{perfil.email ?? '—'}</dd>
              </div>
              <div>
                <dt>Edad</dt>
                <dd>{textoEdad(perfil)}</dd>
              </div>
            </dl>
            <p className="ficha-nota">
              {verificado
                ? 'Nombre y correo provienen de gub.uy; la fecha de nacimiento, de DNIC.'
                : 'DNIC no estaba disponible cuando ingresaste. La verificación se reintenta en tu próximo ingreso.'}
            </p>
          </section>
        )}

        {sesion?.rol === ROLES.CIUDADANO && (
          <section className="ficha" aria-labelledby="titulo-empresa">
            <h2 id="titulo-empresa" className="titulo-con-icono">
              <Truck size={22} aria-hidden="true" />
              Tu empresa
            </h2>
            {/* P2 reemplaza este estado vacío por la empresa asociada y su gestión (o las guías, si es chofer) */}
            <p className="ficha-texto sin-margen">
              Todavía no estás asociado a ninguna empresa de transporte. Cuando un funcionario del MTOP te asocie a tu
              empresa, vas a ver acá su gestión, o tus guías de carga si sos chofer.
            </p>
          </section>
        )}
        {sesion?.rol === ROLES.CIUDADANO && <SolicitudAcceso />}
      </main>

      <PieSitio />
    </div>
  )
}
