import { Building2, LogOut, Truck, Users } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { useAviso } from '../../componentes/avisos'
import { useConfirmar } from '../../componentes/confirmaciones'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FilasCargando } from '../../componentes/FilasCargando'
import { PieSitio } from '../../componentes/PieSitio'
import { ProveedorAvisos } from '../../componentes/ProveedorAvisos'
import { ProveedorConfirmaciones } from '../../componentes/ProveedorConfirmaciones'
import { formatearCedula } from '../../formato'
import type { Chofer, VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const VACIO = { cedula: '', nombre: '' }

/** La empresa se deduce del vínculo de la sesión: nunca se elige ni se envía una empresa arbitraria. */
export function Empresa() {
  return (
    <ProveedorAvisos>
      <ProveedorConfirmaciones>
        <GestionEmpresa />
      </ProveedorConfirmaciones>
    </ProveedorAvisos>
  )
}

function GestionEmpresa() {
  useTituloPagina('Mi empresa')
  const { sesion, logout } = useAuth()
  const avisar = useAviso()
  const confirmar = useConfirmar()
  const [empresa, setEmpresa] = useState<VinculoEmpresa | null>(null)
  const [choferes, setChoferes] = useState<Chofer[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [version, setVersion] = useState(0)
  const [nuevo, setNuevo] = useState(VACIO)
  const [editando, setEditando] = useState<number | null>(null)
  const [edicion, setEdicion] = useState(VACIO)

  useEffect(() => {
    let vigente = true
    setCargando(true)
    setError(null)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then(async (vinculos) => {
        const responsable = vinculos.find((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO')
        if (!responsable) throw new Error('No tenés una empresa activa asignada como responsable.')
        const lista = await api<Chofer[]>(`/choferes?empresaId=${responsable.empresaId}`)
        if (!vigente) return
        setEmpresa(responsable)
        setChoferes(lista)
      })
      .catch((causa: unknown) => {
        if (vigente) setError(causa instanceof Error ? causa.message : 'No se pudo cargar la empresa.')
      })
      .finally(() => { if (vigente) setCargando(false) })
    return () => { vigente = false }
  }, [version])

  function actualizar() { setVersion((actual) => actual + 1) }

  async function crear(evento: FormEvent) {
    evento.preventDefault()
    if (!empresa) return
    setError(null)
    try {
      await api<Chofer>('/choferes', { method: 'POST', body: JSON.stringify({ ...nuevo, empresaId: empresa.empresaId }) })
      setNuevo(VACIO)
      avisar('Chofer registrado')
      actualizar()
    } catch (causa) { setError(causa instanceof Error ? causa.message : 'No se pudo registrar el chofer.') }
  }

  function comenzarEdicion(chofer: Chofer) {
    setEditando(chofer.id)
    setEdicion({ cedula: chofer.cedula, nombre: chofer.nombre })
  }

  async function guardar(chofer: Chofer) {
    setError(null)
    try {
      await api<Chofer>(`/choferes/${chofer.id}`, { method: 'PUT', body: JSON.stringify({ ...chofer, ...edicion }) })
      setEditando(null)
      avisar('Chofer actualizado')
      actualizar()
    } catch (causa) { setError(causa instanceof Error ? causa.message : 'No se pudo actualizar el chofer.') }
  }

  async function cambiarEstado(chofer: Chofer) {
    if (chofer.activo && !(await confirmar({ titulo: `¿Dar de baja a ${chofer.nombre}?`, mensaje: 'Dejará de estar disponible para nuevas guías. Podrás reactivarlo después.', accion: 'Dar de baja', peligro: true }))) return
    setError(null)
    try {
      await api<void>(`/choferes/${chofer.id}${chofer.activo ? '' : '/activo'}`, chofer.activo ? { method: 'DELETE' } : { method: 'PUT', body: JSON.stringify(true) })
      avisar(chofer.activo ? 'Chofer dado de baja' : 'Chofer reactivado')
      actualizar()
    } catch (causa) { setError(causa instanceof Error ? causa.message : 'No se pudo cambiar el estado del chofer.') }
  }

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}><LogOut size={15} aria-hidden="true" /> Salir</button>
      </EncabezadoSitio>
      <main className="pagina-contenido empresa-contenido">
        <nav className="empresa-nav" aria-label="Secciones de la empresa">
          <NavLink to="/mi-cuenta"><Building2 size={18} aria-hidden="true" /> Mi cuenta</NavLink>
          <NavLink to="/mi-empresa"><Users size={18} aria-hidden="true" /> Choferes</NavLink>
        </nav>
        <header className="seccion-encabezado">
          <div><h1>Choferes</h1><p>{empresa ? `Gestioná los choferes habilitados de ${empresa.nombreEmpresa}.` : 'Gestioná los choferes habilitados de tu empresa.'}</p></div>
        </header>
        {error && <p className="mensaje-error" role="alert">{error}</p>}
        {empresa && (
          <section className="ficha ficha-ancha" aria-labelledby="titulo-nuevo-chofer">
            <h2 id="titulo-nuevo-chofer">Nuevo chofer</h2>
            <form onSubmit={(evento) => void crear(evento)} className="formulario-grilla" autoComplete="off">
              <label className="campo"><span>Cédula</span><input value={nuevo.cedula} onChange={(evento) => setNuevo({ ...nuevo, cedula: evento.target.value })} required /></label>
              <label className="campo"><span>Nombre</span><input value={nuevo.nombre} onChange={(evento) => setNuevo({ ...nuevo, nombre: evento.target.value })} required /></label>
              <div className="formulario-botones"><button type="submit">Agregar chofer</button></div>
            </form>
          </section>
        )}
        {empresa && (
          <div className="tabla-contenedor">
            <table className="tabla" aria-busy={cargando}>
              <thead><tr><th>Chofer</th><th>Estado</th><th><span className="solo-lectores">Acciones</span></th></tr></thead>
              <tbody>
                {cargando && <FilasCargando columnas={3} />}
                {choferes.map((chofer) => {
                  const enEdicion = editando === chofer.id
                  return <tr key={chofer.id}><td data-etiqueta="Chofer">{enEdicion ? <div className="edicion-en-linea"><input aria-label={`Cédula de ${chofer.nombre}`} value={edicion.cedula} onChange={(evento) => setEdicion({ ...edicion, cedula: evento.target.value })} /><input aria-label={`Nombre de ${chofer.nombre}`} value={edicion.nombre} onChange={(evento) => setEdicion({ ...edicion, nombre: evento.target.value })} /></div> : <><strong>{chofer.nombre}</strong><span className="celda-secundaria">{formatearCedula(chofer.cedula)}</span></>}</td><td data-etiqueta="Estado">{chofer.activo ? <Etiqueta tono="ok">Activo</Etiqueta> : <Etiqueta tono="neutro">Dado de baja</Etiqueta>}</td><td data-etiqueta="" className="celda-acciones">{enEdicion ? <div className="acciones"><button type="button" className="boton-chico" onClick={() => void guardar(chofer)} disabled={!edicion.cedula.trim() || !edicion.nombre.trim()}>Guardar</button><button type="button" className="boton-chico boton-secundario" onClick={() => setEditando(null)}>Cancelar</button></div> : <div className="acciones"><button type="button" className="boton-chico boton-secundario" onClick={() => comenzarEdicion(chofer)}>Editar</button><button type="button" className="boton-chico boton-secundario" onClick={() => void cambiarEstado(chofer)}>{chofer.activo ? 'Dar de baja' : 'Reactivar'}</button></div>}</td></tr>
                })}
                {!cargando && choferes.length === 0 && <tr><td colSpan={3} className="tabla-vacia">Todavía no hay choferes registrados.</td></tr>}
              </tbody>
            </table>
          </div>
        )}
        {!cargando && !empresa && <Link className="boton-enlace" to="/mi-cuenta">Volver a Mi cuenta</Link>}
      </main>
      <PieSitio />
    </div>
  )
}
