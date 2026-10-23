import { Truck, UserRound } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { NavegacionFuncionario } from '../../componentes/NavegacionFuncionario'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { NavegacionChofer } from '../../componentes/NavegacionChofer'
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

  const roles = sesion?.roles ?? (sesion ? [sesion.rol] : [])
  const esCiudadano = roles.includes(ROLES.CIUDADANO)
  const esChofer = roles.includes(ROLES.CHOFER)
  const esFuncionario = roles.includes(ROLES.FUNCIONARIO)
  // gub.uy tambien asigna CIUDADANO a los funcionarios; en esta vista prevalece su area operativa.
  const puedeVerOpcionesEmpresa = !esFuncionario && (esCiudadano || esChofer)

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
    if (!puedeVerOpcionesEmpresa) {
      setVinculos(null)
      setErrorVinculos(null)
      setCargandoVinculos(false)
      return
    }
    let vigente = true
    setCargandoVinculos(true)
    setErrorVinculos(null)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then((respuesta) => { if (vigente) setVinculos(respuesta) })
      .catch((causa: unknown) => { if (vigente) setErrorVinculos(causa instanceof Error ? causa.message : 'No se pudo consultar tu vínculo con la empresa.') })
      .finally(() => { if (vigente) setCargandoVinculos(false) })
    return () => { vigente = false }
  }, [puedeVerOpcionesEmpresa, versionVinculos])

  // Al volver de otra pestaña se refleja una aprobación sin renovar el JWT.
  useEffect(() => {
    if (!puedeVerOpcionesEmpresa) return
    const actualizar = () => setVersionVinculos((version) => version + 1)
    window.addEventListener('focus', actualizar)
    return () => window.removeEventListener('focus', actualizar)
  }, [puedeVerOpcionesEmpresa])

  const verificado = perfil?.verificacionDnic === 'VERIFICADO'
  const esResponsable = vinculos?.some((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO') ?? false
  const mostrarAccesos = puedeVerOpcionesEmpresa && !esResponsable && !esChofer

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}>
          Salir
        </button>
      </EncabezadoSitio>

      <main className="pagina-contenido">
        {esFuncionario && <NavegacionFuncionario />}
        {puedeVerOpcionesEmpresa && esResponsable && <NavegacionEmpresa />}
        {!esFuncionario && !esResponsable && esChofer && <NavegacionChofer />}
        <h1 className="saludo">Hola, {sesion?.nombre}</h1>
        <p className="saludo-rol">{nombreRol(sesion?.rol)}</p>

        {error && (
          <p className="mensaje-error" role="alert">
            {error}
          </p>
        )}

        {puedeVerOpcionesEmpresa && cargandoVinculos && !vinculos && <p role="status">Consultando el vínculo con tu empresa…</p>}
        {puedeVerOpcionesEmpresa && errorVinculos && (
          <div>
            <p className="mensaje-error" role="alert">{errorVinculos}</p>
            <button type="button" className="boton-secundario" disabled={cargandoVinculos} onClick={() => setVersionVinculos((version) => version + 1)}>Reintentar consulta de empresa</button>
          </div>
        )}

        {/* Responsable y chofer navegan arriba; solo el ciudadano sin responsabilidad conserva el acceso por tarjeta. */}
        <div className={mostrarAccesos ? 'cuenta-grilla' : 'cuenta-grilla cuenta-grilla-simple'}>
          {mostrarAccesos && (
            <div className="cuenta-principal">
              <section className="ficha" aria-labelledby="titulo-empresa">
                <h2 id="titulo-empresa" className="titulo-con-icono">
                  <Truck size={22} aria-hidden="true" />
                  Guías de tu empresa
                </h2>
                <p className="ficha-texto">Registrá una guía para tu empresa. El sistema verifica que el vehículo y el chofer estén habilitados y disponibles.</p>
                <Link className="boton-enlace" to="/guias/nueva">Registrar guía</Link>
              </section>
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
        {puedeVerOpcionesEmpresa && vinculos && !errorVinculos && !esResponsable && <SolicitudResponsable />}
        {puedeVerOpcionesEmpresa && esCiudadano && <SolicitudAcceso />}
      </main>

      <PieSitio />
    </div>
  )
}
