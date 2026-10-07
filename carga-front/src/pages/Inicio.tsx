import { ShieldCheck, Truck } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/contexto'
import { INGRESO_GUBUY } from '../auth/gubuy'
import { EncabezadoSitio } from '../componentes/EncabezadoSitio'
import { PieSitio } from '../componentes/PieSitio'
import { ROLES } from '../tipos'
import { Ruta } from './Ruta'
import { useTituloPagina } from '../titulo'

/** Una guía de carga de ejemplo: muestra el objeto real con el que trabaja el sistema. */
function GuiaDeEjemplo() {
  return (
    <figure className="guia" aria-label="Ejemplo de guía de carga">
      <div className="guia-encabezado">
        <span className="guia-titulo">Guía de carga</span>
        <span className="guia-numero">N.º 2026-0418</span>
      </div>
      <dl className="guia-datos">
        <div>
          <dt>Vehículo</dt>
          <dd>SBC 1234</dd>
        </div>
        <div>
          <dt>Carga</dt>
          <dd>Granos</dd>
        </div>
        <div className="guia-dato-ancho">
          <dt>Recorrido</dt>
          <dd>Montevideo a Minas por Ruta 8</dd>
        </div>
        <div>
          <dt>Peso declarado</dt>
          <dd>28 t</dd>
        </div>
      </dl>
      <div className="guia-pesaje">
        <div>
          <span className="guia-pesaje-lugar">Peaje Pando, 14:05</span>
          <span className="guia-pesaje-peso">27,6 t</span>
          <span className="guia-pesaje-diferencia">0,4 t menos que lo declarado</span>
        </div>
        <span className="etiqueta etiqueta-ok">En regla</span>
      </div>
      <figcaption>Ejemplo ilustrativo</figcaption>
    </figure>
  )
}

export function Inicio() {
  useTituloPagina('')
  const { sesion } = useAuth()
  const conGubUy = sesion?.rol === ROLES.CIUDADANO || sesion?.rol === ROLES.CHOFER || sesion?.rol === ROLES.FUNCIONARIO
  const conBackoffice = sesion?.rol === ROLES.ADMINISTRADOR || sesion?.rol === ROLES.AUTORIDAD

  const accesoGubUy = conGubUy ? (
    <Link className="boton-acceso" to="/mi-cuenta">
      Ir a Mi cuenta
    </Link>
  ) : (
    <a className="boton-acceso" href={INGRESO_GUBUY}>
      Ingresar con gub.uy
    </a>
  )

  return (
    <div className="pagina portada">
      <EncabezadoSitio>
        <span className="sitio-organismo">Ministerio de Transporte y Obras Públicas</span>
        <Link className="sitio-acceso" to={conBackoffice ? '/backoffice' : '/backoffice/login'}>
          {conBackoffice ? 'Backoffice' : 'Acceso MTOP'}
        </Link>
      </EncabezadoSitio>

      <main>
        <section className="portada-hero">
          <div className="portada-hero-texto">
            <h1>Transporte de carga en Uruguay, de la guía al peaje</h1>
            <p>
              Las empresas gestionan sus permisos y guías de carga. El MTOP verifica pesajes y recorridos para que la
              carga viaje en regla.
            </p>
            <div className="portada-hero-accion">
              {accesoGubUy}
              <span>Empresas, choferes y funcionarios del MTOP</span>
            </div>
          </div>
          <GuiaDeEjemplo />
        </section>

        {/* Cómo funciona: la persona recorre el viaje etapa por etapa */}
        <section className="portada-recorrido" aria-labelledby="titulo-recorrido">
          <h2 id="titulo-recorrido">Cómo funciona</h2>
          <p className="portada-recorrido-ayuda">Elegí una etapa para ver qué pasa en cada punto del viaje.</p>
          <Ruta />
        </section>

        {/* Dos puertas: cada persona encuentra la suya según quién es */}
        <section className="portada-puertas" aria-labelledby="titulo-puertas">
          <h2 id="titulo-puertas">Cómo ingresar</h2>
          <div className="puertas">
            <article className="puerta puerta-principal">
              <span className="puerta-icono" aria-hidden="true">
                <Truck size={24} />
              </span>
              <h3>Empresas, choferes y funcionarios</h3>
              <p>Con tu usuario gub.uy. Si es la primera vez, tu cuenta se crea al ingresar.</p>
              {accesoGubUy}
            </article>
            <article className="puerta">
              <span className="puerta-icono" aria-hidden="true">
                <ShieldCheck size={24} />
              </span>
              <h3>Autoridades y administración del MTOP</h3>
              <p>Con el usuario y la contraseña que te asignó un administrador.</p>
              <Link className="boton-acceso boton-acceso-secundario" to={conBackoffice ? '/backoffice' : '/backoffice/login'}>
                {conBackoffice ? 'Ir al backoffice' : 'Ingresar al backoffice'}
              </Link>
            </article>
          </div>
        </section>
      </main>

      <PieSitio />
    </div>
  )
}
