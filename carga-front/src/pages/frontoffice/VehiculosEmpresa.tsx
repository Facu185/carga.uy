import { LogOut } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { useAviso } from '../../componentes/avisos'
import { useConfirmar, type OpcionesConfirmacion } from '../../componentes/confirmaciones'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FormularioVehiculo } from '../../componentes/FormularioVehiculo'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { PieSitio } from '../../componentes/PieSitio'
import { ProveedorAvisos } from '../../componentes/ProveedorAvisos'
import { ProveedorConfirmaciones } from '../../componentes/ProveedorConfirmaciones'
import { formatearFecha } from '../../formato'
import type { DatosVehiculo, Vehiculo, VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; empresa: VinculoEmpresa; vehiculos: Vehiculo[] }

export function VehiculosEmpresa() {
  return <ProveedorAvisos><ProveedorConfirmaciones><GestionVehiculos /></ProveedorConfirmaciones></ProveedorAvisos>
}

function GestionVehiculos() {
  useTituloPagina('Vehículos de mi empresa')
  const { sesion, logout } = useAuth()
  const avisar = useAviso()
  const confirmar = useConfirmar()
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)
  const [editando, setEditando] = useState<Vehiculo | null>(null)
  const [mostrarFormulario, setMostrarFormulario] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const enviando = useRef(false)
  const generacion = useRef(0)
  const titulo = useRef<HTMLHeadingElement>(null)
  const origenEdicion = useRef<HTMLButtonElement | null>(null)

  useEffect(() => {
    let vigente = true
    const controlador = new AbortController()
    generacion.current++
    setCarga({ estado: 'cargando' })
    setMostrarFormulario(false)
    setEditando(null)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios', { signal: controlador.signal })
      .then(async (vinculos) => {
        if (!vigente) return
        const responsables = vinculos.filter((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO')
        if (responsables.length === 0) throw new Error('No tienes una empresa asignada como responsable activo.')
        if (responsables.length !== 1) throw new Error('Hay más de una empresa responsable activa. Solicita que se revisen tus vínculos antes de gestionar vehículos.')
        const empresa = responsables[0]
        const vehiculos = await api<Vehiculo[]>(`/vehiculos?empresaId=${empresa.empresaId}`, { signal: controlador.signal })
        if (vigente) setCarga({ estado: 'lista', empresa, vehiculos })
      })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudieron cargar los vehículos.' })
      })
    return () => { vigente = false; generacion.current++; controlador.abort() }
  }, [version, sesion?.token])

  useEffect(() => {
    if (!mostrarFormulario && origenEdicion.current) {
      if (origenEdicion.current.isConnected) origenEdicion.current.focus()
      else titulo.current?.focus()
      origenEdicion.current = null
    }
  }, [mostrarFormulario])

  function comenzarEdicion(vehiculo: Vehiculo | null, boton: HTMLButtonElement) {
    origenEdicion.current = boton
    setError(null)
    setEditando(vehiculo)
    setMostrarFormulario(true)
  }

  // Exclusión inmediata para doble clic; la referencia de generación descarta resultados de otra sesión/vista.
  async function mutar(operacion: () => Promise<unknown>, mensaje: string, opciones?: OpcionesConfirmacion) {
    if (enviando.current || carga.estado !== 'lista') return
    enviando.current = true
    setGuardando(true)
    const actual = generacion.current
    try {
      if (opciones && !(await confirmar(opciones))) return
      if (generacion.current !== actual) return
      setError(null)
      await operacion()
      if (generacion.current !== actual) return
      avisar(mensaje)
      setMostrarFormulario(false)
      // No permitir otra accion sobre filas anteriores mientras se recarga el resultado.
      setCarga({ estado: 'cargando' })
      setVersion((valor) => valor + 1)
      titulo.current?.focus()
    } catch (causa: unknown) {
      if (generacion.current === actual) setError(causa instanceof Error ? causa.message : 'No se pudo completar la operación.')
    } finally {
      enviando.current = false
      setGuardando(false)
    }
  }

  async function guardar(datos: DatosVehiculo) {
    if (carga.estado !== 'lista') return
    const cuerpo = { ...datos, empresaId: carga.empresa.empresaId, habilitado: editando?.habilitado ?? true }
    await mutar(
      () => api<Vehiculo>(editando ? `/vehiculos/${editando.id}` : '/vehiculos', { method: editando ? 'PUT' : 'POST', body: JSON.stringify(cuerpo) }),
      editando ? 'Vehículo actualizado' : 'Vehículo registrado',
    )
  }

  async function cambiarEstado(vehiculo: Vehiculo) {
    await mutar(
      () => api<void>(`/vehiculos/${vehiculo.id}${vehiculo.habilitado ? '' : '/reactivar'}`, { method: vehiculo.habilitado ? 'DELETE' : 'PUT' }),
      vehiculo.habilitado ? 'Vehículo dado de baja' : 'Vehículo reactivado',
      vehiculo.habilitado ? {
        titulo: `¿Dar de baja a ${vehiculo.matricula}?`,
        mensaje: 'Dejará de estar disponible para nuevas guías. Si tiene una guía en curso, el servidor rechazará la baja. Podrás reactivarlo después.',
        accion: 'Dar de baja', peligro: true,
      } : undefined,
    )
  }

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}><LogOut size={15} aria-hidden="true" /> Salir</button>
      </EncabezadoSitio>
      <main className="pagina-contenido empresa-contenido">
        <NavegacionEmpresa />
        <header className="seccion-encabezado">
          <div><h1 ref={titulo} tabIndex={-1}>Vehículos</h1><p>{carga.estado === 'lista' ? `Vehículos de ${carga.empresa.nombreEmpresa}.` : 'Gestiona los vehículos de tu empresa.'}</p></div>
          {carga.estado === 'lista' && <button type="button" disabled={guardando || mostrarFormulario} onClick={(evento) => comenzarEdicion(null, evento.currentTarget)}>Agregar vehículo</button>}
        </header>
        {error && <p id="error-vehiculos" className="mensaje-error" role="alert">{error}</p>}
        {carga.estado === 'cargando' && <p role="status">Cargando vehículos…</p>}
        {carga.estado === 'error' && <div><p className="mensaje-error" role="alert">{carga.mensaje}</p><button type="button" className="boton-secundario" onClick={() => { setError(null); setVersion((valor) => valor + 1) }}>Reintentar</button></div>}
        {carga.estado === 'lista' && (
          <>
            {mostrarFormulario && <FormularioVehiculo key={editando?.id ?? 'nuevo'} vehiculo={editando} ocupado={guardando} error={error} onGuardar={guardar} onCancelar={() => { setMostrarFormulario(false); setError(null) }} />}
            <p>La baja conserva el historial y puede revertirse. Reactivar no renueva el permiso ni la ITV.</p>
            <div className="tabla-contenedor">
              <table className="tabla" aria-busy={guardando}>
                <caption>Vehículos de {carga.empresa.nombreEmpresa}, incluidos los dados de baja</caption>
                <thead><tr><th scope="col">Vehículo</th><th scope="col">Peso y capacidades</th><th scope="col">Permiso</th><th scope="col">ITV</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
                <tbody>
                  {carga.vehiculos.map((vehiculo) => (
                    <tr key={vehiculo.id}>
                      <td data-etiqueta="Vehículo"><strong>{vehiculo.matricula}</strong><span className="celda-secundaria">{[vehiculo.marca, vehiculo.modelo].filter(Boolean).join(' ') || 'Sin marca ni modelo'}</span></td>
                      <td data-etiqueta="Peso y capacidades">Vacío: {vehiculo.pesoKg} kg<span className="celda-secundaria">Carga: {vehiculo.capacidadCargaKg} kg · {vehiculo.capacidadVolumenM3 ?? 'Sin configurar'} m³</span></td>
                      <td data-etiqueta="Permiso"><strong>{vehiculo.numeroPermiso ?? 'Sin permiso'}</strong><span className="celda-secundaria">{formatearFecha(vehiculo.permisoDesde)} — {formatearFecha(vehiculo.permisoHasta)}</span></td>
                      <td data-etiqueta="ITV">{formatearFecha(vehiculo.itvVencimiento)}</td>
                      <td data-etiqueta="Estado"><Etiqueta tono={vehiculo.habilitado ? 'ok' : 'neutro'}>{vehiculo.habilitado ? 'Activo' : 'Dado de baja'}</Etiqueta></td>
                      <td data-etiqueta="Acciones" className="celda-acciones"><div className="acciones">
                        <button type="button" className="boton-chico boton-secundario" disabled={guardando || mostrarFormulario} aria-label={`Editar ${vehiculo.matricula}`} onClick={(evento) => comenzarEdicion(vehiculo, evento.currentTarget)}>Editar</button>
                        <button type="button" className="boton-chico boton-secundario" disabled={guardando || mostrarFormulario} aria-label={`${vehiculo.habilitado ? 'Dar de baja' : 'Reactivar'} ${vehiculo.matricula}`} onClick={() => void cambiarEstado(vehiculo)}>{vehiculo.habilitado ? 'Dar de baja' : 'Reactivar'}</button>
                      </div></td>
                    </tr>
                  ))}
                  {carga.vehiculos.length === 0 && <tr><td colSpan={6} className="tabla-vacia">Todavía no hay vehículos registrados.</td></tr>}
                </tbody>
              </table>
            </div>
          </>
        )}
      </main>
      <PieSitio />
    </div>
  )
}
