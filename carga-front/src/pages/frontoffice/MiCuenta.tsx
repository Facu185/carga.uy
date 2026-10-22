import { Truck, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearCedula, formatearFecha, nombreRol } from '../../formato'
import { ROLES, type Perfil, type VinculoEmpresa } from '../../tipos'
import { SolicitudAcceso } from './SolicitudAcceso'
import { SolicitudResponsable } from './SolicitudResponsable'
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
  const [vinculos, setVinculos] = useState<VinculoEmpresa[] | null>(null)
  const [cargandoVinculos, setCargandoVinculos] = useState(true)
  const [errorVinculos, setErrorVinculos] = useState<string | null>(null)
  const [versionVinculos, setVersionVinculos] = useState(0)
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

  useEffect(() => {
    let vigente = true
    setCargandoVinculos(true)
    setErrorVinculos(null)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then((respuesta) => { if (vigente) setVinculos(respuesta) })
      .catch((causa: unknown) => { if (vigente) setErrorVinculos(causa instanceof Error ? causa.message : 'No se pudo consultar tu vínculo con la empresa.') })
      .finally(() => { if (vigente) setCargandoVinculos(false) })
    return () => { vigente = false }
  }, [versionVinculos])

  // Al volver de otra pestaña se refleja una aprobación sin renovar el JWT.
  useEffect(() => {
    const actualizar = () => setVersionVinculos((version) => version + 1)
    window.addEventListener('focus', actualizar)
    return () => window.removeEventListener('focus', actualizar)
  }, [])

  const verificado = perfil?.verificacionDnic === 'VERIFICADO'
  const roles = sesion?.roles ?? (sesion ? [sesion.rol] : [])
  const esCiudadano = roles.includes(ROLES.CIUDADANO)
  const esChofer = roles.includes(ROLES.CHOFER)
  const esFuncionario = roles.includes(ROLES.FUNCIONARIO)
  const puedeSolicitarResponsabilidad = esCiudadano || esChofer
  const esResponsable = vinculos?.some((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO') ?? false

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

        {esFuncionario && (
          <section className="ficha" aria-labelledby="titulo-area-funcionario">
            <h2 id="titulo-area-funcionario" className="titulo-con-icono"><Truck size={22} aria-hidden="true" />Área del funcionario</h2>
            <p className="ficha-texto">Consulta y administra empresas existentes con tu identidad de funcionario.</p>
            <Link className="boton-enlace" to="/funcionario/empresas">Gestionar empresas</Link>
          </section>
        )}

        {/* En pantallas anchas: lo que la persona viene a hacer a la izquierda y sus datos al costado */}
        <div className={esCiudadano || esChofer ? 'cuenta-grilla' : 'cuenta-grilla cuenta-grilla-simple'}>
          {(esCiudadano || esChofer) && (
            <div className="cuenta-principal">
              <section className="ficha" aria-labelledby="titulo-empresa">
                <h2 id="titulo-empresa" className="titulo-con-icono">
                  <Truck size={22} aria-hidden="true" />
                  {esChofer ? 'Tus guías' : 'Guías de tu empresa'}
                </h2>
                {esChofer ? (
                  <>
                    <p className="ficha-texto">Consultá las guías de carga que fueron asignadas a tu cédula.</p>
                    <Link className="boton-enlace" to="/mis-guias">Ver mis guías</Link>
                  </>
                ) : (
                  <>
                    <p className="ficha-texto">Registrá una guía para tu empresa. El sistema verifica que el vehículo y el chofer estén habilitados y disponibles.</p>
                    <Link className="boton-enlace" to="/guias/nueva">Registrar guía</Link>
                  </>
                )}
              </section>
              {esResponsable && (
                <section className="ficha" aria-labelledby="titulo-administrar-empresa">
                  <h2 id="titulo-administrar-empresa" className="titulo-con-icono">
                    <Truck size={22} aria-hidden="true" />
                    Administración de empresa
                  </h2>
                  <p className="ficha-texto">Gestioná los choferes habilitados de la empresa de la que sos responsable.</p>
                  <Link className="boton-enlace" to="/mi-empresa">Administrar choferes</Link>
                </section>
              )}
              {esResponsable && (
                <section className="ficha" aria-labelledby="titulo-guias-viajes">
                  <h2 id="titulo-guias-viajes" className="titulo-con-icono">
                    <Truck size={22} aria-hidden="true" />
                    Guías de viajes
                  </h2>
                  <p className="ficha-texto">Consultá todas las guías de tu empresa, incluidas las finalizadas, junto con el chofer y el vehículo asignados.</p>
                  <Link className="boton-enlace" to="/mi-empresa/guias">Ver guías de viajes</Link>
                </section>
              )}
              {puedeSolicitarResponsabilidad && cargandoVinculos && !vinculos && <p role="status">Consultando el vínculo con tu empresa…</p>}
              {puedeSolicitarResponsabilidad && errorVinculos && (
                <div>
                  <p className="mensaje-error" role="alert">{errorVinculos}</p>
                  <button type="button" className="boton-secundario" disabled={cargandoVinculos} onClick={() => setVersionVinculos((version) => version + 1)}>Reintentar consulta de empresa</button>
                </div>
              )}
              {puedeSolicitarResponsabilidad && vinculos && !errorVinculos && !esResponsable && <SolicitudResponsable />}
              {esCiudadano && <SolicitudAcceso />}
            </div>
          )}
          <div className="cuenta-datos">
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
          </div>
        </div>
      </main>

      <PieSitio />
    </div>
  )
}
