import { LogOut } from 'lucide-react'
import { useEffect, useRef, useState, type FormEvent } from 'react'
import { api, ErrorApi } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearFecha } from '../../formato'
import type { DatosPerfilEmpresa, PerfilEmpresa as Perfil, VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; perfil: Perfil; token: string }
type Valores = { nombre: string; razonSocial: string; numeroEmpresa: string; direccion: string }
const VACIO: Valores = { nombre: '', razonSocial: '', numeroEmpresa: '', direccion: '' }

function valoresDe(perfil: Perfil): Valores {
  return {
    nombre: perfil.nombre, razonSocial: perfil.razonSocial ?? '',
    numeroEmpresa: String(perfil.numeroEmpresa), direccion: perfil.direccion ?? '',
  }
}

/** Presentacion F10: la pertenencia y los campos editables se vuelven a comprobar en el EJB. */
export function PerfilEmpresa() {
  useTituloPagina('Perfil de mi empresa')
  const { sesion, logout } = useAuth()
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [valores, setValores] = useState<Valores>(VACIO)
  const [version, setVersion] = useState(0)
  const [guardando, setGuardando] = useState(false)
  const [incierto, setIncierto] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [exito, setExito] = useState<string | null>(null)
  const generacion = useRef(0)
  const enviando = useRef(false)
  const mensaje = useRef<HTMLParagraphElement>(null)
  const token = sesion?.token

  useEffect(() => {
    const actual = ++generacion.current
    const controlador = new AbortController()
    enviando.current = false
    setGuardando(false); setIncierto(false); setError(null); setExito(null)
    setCarga({ estado: 'cargando' }); setValores(VACIO)
    async function cargar() {
      try {
        if (!token) return
        const vinculos = await api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios', { signal: controlador.signal })
        if (generacion.current !== actual) return
        if (!Array.isArray(vinculos)) throw new Error('No se pudieron interpretar los vínculos de tu empresa.')
        const responsables = vinculos.filter((vinculo) => vinculo?.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO')
        if (responsables.length === 0) throw new Error('No tienes una empresa asignada como responsable activo.')
        if (responsables.length !== 1) throw new Error('Hay más de una empresa responsable activa. Solicita revisar tus vínculos antes de editar el perfil.')
        const id = responsables[0].empresaId
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error('El identificador de la empresa no es válido para este portal.')
        const perfil = await api<Perfil>(`/empresas/${id}/perfil`, { signal: controlador.signal })
        if (generacion.current !== actual) return
        if (!perfil || perfil.id !== id || !perfil.activa || typeof perfil.nombre !== 'string')
          throw new Error('No se pudo obtener el perfil autorizado de tu empresa.')
        if (!Number.isSafeInteger(perfil.numeroEmpresa) || perfil.numeroEmpresa <= 0)
          throw new Error('El número de empresa no puede editarse de forma segura en este portal. Solicita su revisión administrativa.')
        setValores(valoresDe(perfil)); setCarga({ estado: 'lista', perfil, token })
      } catch (causa) {
        if (generacion.current === actual)
          setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudo cargar el perfil.' })
      }
    }
    void cargar()
    return () => { generacion.current++; controlador.abort() }
  }, [token, version])

  useEffect(() => { if (error || exito) mensaje.current?.focus() }, [error, exito])

  // Una nueva sesion nunca ve el formulario ni los datos cargados con el token anterior.
  const perfil = carga.estado === 'lista' && carga.token === token ? carga.perfil : null

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (enviando.current || !perfil || !token || incierto) return
    const numero = Number(valores.numeroEmpresa)
    if (!valores.nombre.trim() || !Number.isSafeInteger(numero) || numero <= 0) {
      setError('Indica un nombre y un número de empresa entero positivo válido.'); setExito(null)
      return
    }
    const datos: DatosPerfilEmpresa = {
      nombre: valores.nombre.trim(), razonSocial: valores.razonSocial.trim() || null,
      numeroEmpresa: numero, direccion: valores.direccion.trim() || null,
    }
    const actual = generacion.current
    enviando.current = true; setGuardando(true); setError(null); setExito(null)
    try {
      const actualizado = await api<Perfil>(`/empresas/${perfil.id}/perfil`, { method: 'PUT', body: JSON.stringify(datos) })
      if (generacion.current !== actual) return
      if (!actualizado || actualizado.id !== perfil.id || !actualizado.activa
          || typeof actualizado.nombre !== 'string' || !Number.isSafeInteger(actualizado.numeroEmpresa)
          || actualizado.numeroEmpresa <= 0) throw new Error('No se pudo confirmar la respuesta de guardado.')
      setValores(valoresDe(actualizado))
      setCarga({ estado: 'lista', perfil: actualizado, token })
      setExito('Perfil de empresa guardado.')
    } catch (causa) {
      if (generacion.current !== actual) return
      const texto = causa instanceof Error ? causa.message : 'No se pudo guardar el perfil.'
      if (causa instanceof ErrorApi && (causa.status === 401 || causa.status === 403)) {
        setValores(VACIO); setCarga({ estado: 'error', mensaje: texto })
      } else if (causa instanceof ErrorApi && causa.status === 400) {
        setError(texto)
      } else {
        setIncierto(true)
        setError('No se pudo confirmar el guardado. Vuelve a consultar el perfil antes de intentar guardar otra vez.')
      }
    } finally {
      if (generacion.current === actual) { enviando.current = false; setGuardando(false) }
    }
  }

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}><LogOut size={15} aria-hidden="true" /> Salir</button>
      </EncabezadoSitio>
      <main className="pagina-contenido empresa-contenido">
        <NavegacionEmpresa />
        <header className="seccion-encabezado"><div><h1>Perfil de mi empresa</h1><p>Consulta y actualiza los datos de tu empresa.</p></div></header>
        {carga.estado === 'error' && <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
          <button type="button" className="boton-secundario" onClick={() => setVersion((valor) => valor + 1)}>Volver a consultar</button></div>}
        {carga.estado !== 'error' && !perfil && <p role="status">Cargando perfil de empresa…</p>}
        {perfil && (
          <section className="ficha ficha-ancha" aria-labelledby="titulo-perfil-empresa">
            <h2 id="titulo-perfil-empresa">{perfil.nombre}</h2>
            <p>Estado: activa. Fecha de alta: {formatearFecha(perfil.fechaAlta)}. Estos datos no se modifican desde el perfil.</p>
            <p id="ayuda-perfil-empresa">Los campos con * son obligatorios. La empresa se determina por tu vínculo; no puede elegirse otra desde este formulario.</p>
            {error && <p ref={mensaje} tabIndex={-1} id="error-perfil-empresa" className="mensaje-error" role="alert">{error}</p>}
            {exito && <p ref={mensaje} tabIndex={-1} role="status">{exito}</p>}
            <form className="formulario-grilla" aria-busy={guardando}
              aria-describedby={`ayuda-perfil-empresa${error ? ' error-perfil-empresa' : ''}`} onSubmit={(evento) => void guardar(evento)}>
              <label className="campo"><span>Nombre *</span><input name="nombre" value={valores.nombre} required maxLength={200}
                disabled={guardando || incierto} onChange={(evento) => setValores({ ...valores, nombre: evento.target.value })} /></label>
              <label className="campo"><span>Razón social</span><input name="razonSocial" value={valores.razonSocial} maxLength={200}
                disabled={guardando || incierto} onChange={(evento) => setValores({ ...valores, razonSocial: evento.target.value })} /></label>
              <label className="campo"><span>Número de empresa *</span><input name="numeroEmpresa" type="number" min={1} max={Number.MAX_SAFE_INTEGER} step={1}
                value={valores.numeroEmpresa} required disabled={guardando || incierto}
                onChange={(evento) => setValores({ ...valores, numeroEmpresa: evento.target.value })} /></label>
              <label className="campo"><span>Dirección</span><input name="direccion" value={valores.direccion} maxLength={300}
                disabled={guardando || incierto} onChange={(evento) => setValores({ ...valores, direccion: evento.target.value })} /></label>
              <div className="formulario-botones">
                <button type="submit" disabled={guardando || incierto}>{guardando ? 'Guardando…' : 'Guardar cambios'}</button>
                <button type="button" className="boton-secundario" disabled={guardando || incierto}
                  onClick={() => { setValores(valoresDe(perfil)); setError(null); setExito(null) }}>Cancelar cambios</button>
                {incierto && <button type="button" className="boton-secundario" onClick={() => setVersion((valor) => valor + 1)}>Volver a consultar</button>}
              </div>
            </form>
          </section>
        )}
      </main>
      <PieSitio />
    </div>
  )
}
