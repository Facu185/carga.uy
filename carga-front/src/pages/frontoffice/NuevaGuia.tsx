import { ClipboardPenLine, MapPin, Truck } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { PieSitio } from '../../componentes/PieSitio'
import { ROLES, type ChoferCatalogo, type CrearGuia, type Guia, type OpcionCatalogo, type VehiculoCatalogo, type VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

type FormularioGuia = Record<keyof CrearGuia, string>
const VACIO: FormularioGuia = { vehiculoId: '', choferId: '', rubroId: '', tipoCargaId: '', pesoKg: '', volumenM3: '', inicio: '', fin: '', origen: '', origenLat: '', origenLon: '', destino: '', destinoLat: '', destinoLon: '' }

function numeroOpcional(valor: string): number | undefined { return valor.trim() === '' ? undefined : Number(valor) }
function CampoNumerico({ etiqueta, valor, alCambiar, decimal = false, obligatorio = true, min = 0, max }: { etiqueta: string, valor: string, alCambiar: (valor: string) => void, decimal?: boolean, obligatorio?: boolean, min?: number, max?: number }) {
  return <label className="campo"><span>{etiqueta}</span><input type="number" min={min} max={max} step={decimal ? 'any' : '1'} value={valor} onChange={(e) => alCambiar(e.target.value)} required={obligatorio} /></label>
}

/** Registro de guías de P2. El central verifica empresa, vigencias y solapamientos. */
export function NuevaGuia() {
  useTituloPagina('Registrar guía')
  const { sesion, logout } = useAuth()
  const [formulario, setFormulario] = useState<FormularioGuia>(VACIO)
  const [vehiculos, setVehiculos] = useState<VehiculoCatalogo[]>([])
  const [choferes, setChoferes] = useState<ChoferCatalogo[]>([])
  const [rubros, setRubros] = useState<OpcionCatalogo[]>([])
  const [tiposCarga, setTiposCarga] = useState<OpcionCatalogo[]>([])
  const [cargandoCatalogos, setCargandoCatalogos] = useState(true)
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [creada, setCreada] = useState<Guia | null>(null)
  const [tokenResponsable, setTokenResponsable] = useState<string | null>(null)
  const roles = sesion?.roles ?? (sesion ? [sesion.rol] : [])
  const esFuncionario = roles.includes(ROLES.FUNCIONARIO)

  // Solo decide la navegación; nunca condiciona los catálogos ni el permiso de crear.
  useEffect(() => {
    let vigente = true
    const token = sesion?.token
    setTokenResponsable(null)
    if (!token || esFuncionario) return
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then((vinculos) => {
        if (vigente && vinculos.some((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO')) setTokenResponsable(token)
      })
      .catch(() => {
        // Sin confirmar el vínculo se conserva el retorno a Mi cuenta, no se bloquea el formulario.
        if (vigente) setTokenResponsable(null)
      })
    return () => { vigente = false }
  }, [sesion?.token, esFuncionario])

  const cambiar = (campo: keyof CrearGuia, valor: string) => setFormulario((anterior) => ({ ...anterior, [campo]: valor }))
  useEffect(() => {
    let vigente = true
    Promise.all([api<VehiculoCatalogo[]>('/guias/catalogos/vehiculos'), api<ChoferCatalogo[]>('/guias/catalogos/choferes'), api<OpcionCatalogo[]>('/guias/catalogos/rubros'), api<OpcionCatalogo[]>('/guias/catalogos/tipos-carga')])
      .then(([vehiculos, choferes, rubros, tipos]) => { if (vigente) { setVehiculos(vehiculos); setChoferes(choferes); setRubros(rubros); setTiposCarga(tipos) } })
      .catch((causa: unknown) => vigente && setError(causa instanceof Error ? causa.message : 'No se pudieron cargar los catálogos.'))
      .finally(() => vigente && setCargandoCatalogos(false))
    return () => { vigente = false }
  }, [])

  async function registrar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault(); setError(null); setCreada(null); setEnviando(true)
    try {
      const guia = await api<Guia>('/guias', { method: 'POST', body: JSON.stringify({
        vehiculoId: Number(formulario.vehiculoId), choferId: Number(formulario.choferId), rubroId: Number(formulario.rubroId), tipoCargaId: numeroOpcional(formulario.tipoCargaId),
        pesoKg: Number(formulario.pesoKg), volumenM3: Number(formulario.volumenM3), inicio: new Date(formulario.inicio).toISOString(), fin: new Date(formulario.fin).toISOString(),
        origen: formulario.origen.trim(), origenLat: numeroOpcional(formulario.origenLat), origenLon: numeroOpcional(formulario.origenLon), destino: formulario.destino.trim(), destinoLat: numeroOpcional(formulario.destinoLat), destinoLon: numeroOpcional(formulario.destinoLon),
      }) })
      setCreada(guia); setFormulario(VACIO)
    } catch (causa) { setError(causa instanceof Error ? causa.message : 'No se pudo registrar la guía.') } finally { setEnviando(false) }
  }

  return <div className="pagina"><EncabezadoSitio><span className="sitio-usuario">{sesion?.nombre}</span><button type="button" className="boton-secundario boton-chico" onClick={logout}>Salir</button></EncabezadoSitio>
    <main className="pagina-contenido contenido-guia">
      {!esFuncionario && tokenResponsable === sesion?.token && <NavegacionEmpresa />}
      <a className="volver" href="/mi-cuenta">Volver a Mi cuenta</a>
      <header className="encabezado-guia"><span className="tarjeta-seccion-icono" aria-hidden="true"><ClipboardPenLine size={24} /></span><div><h1>Registrar guía de carga</h1><p>Completá el viaje. El sistema controla la empresa, la vigencia del vehículo y que no se superponga con otra guía.</p></div></header>
      {error && <p className="mensaje-error" role="alert">{error}</p>}
      {creada && <section className="mensaje-exito" role="status"><strong>Guía N.º {creada.id} registrada.</strong> Quedó asignada para el período indicado.</section>}
      <form className="ficha ficha-ancha formulario-guia" onSubmit={(evento) => void registrar(evento)}>
        <section aria-labelledby="asignacion"><h2 id="asignacion" className="titulo-con-icono"><Truck size={22} aria-hidden="true" />Asignación</h2><p className="ficha-texto">Seleccioná los datos habilitados de tu empresa; no ingreses IDs manualmente.</p><div className="formulario-grilla">
<label className="campo"><span>Vehículo</span><select value={formulario.vehiculoId} onChange={(e) => cambiar('vehiculoId', e.target.value)} required disabled={cargandoCatalogos}><option value="">Seleccioná un vehículo</option>{vehiculos.map((vehiculo) => <option key={vehiculo.id} value={vehiculo.id}>{vehiculo.matricula}{vehiculo.marca ? ` — ${vehiculo.marca} ${vehiculo.modelo ?? ''}` : ''}</option>)}</select></label>
<label className="campo"><span>Chofer</span><select value={formulario.choferId} onChange={(e) => cambiar('choferId', e.target.value)} required disabled={cargandoCatalogos}><option value="">Seleccioná un chofer</option>{choferes.map((chofer) => <option key={chofer.id} value={chofer.id}>{chofer.nombre} — {chofer.cedula}</option>)}</select></label>
<label className="campo"><span>Rubro</span><select value={formulario.rubroId} onChange={(e) => cambiar('rubroId', e.target.value)} required disabled={cargandoCatalogos}><option value="">Seleccioná un rubro</option>{rubros.map((rubro) => <option key={rubro.id} value={rubro.id}>{rubro.nombre}</option>)}</select></label>
<label className="campo"><span>Tipo de carga (opcional)</span><select value={formulario.tipoCargaId} onChange={(e) => cambiar('tipoCargaId', e.target.value)} disabled={cargandoCatalogos}><option value="">Sin especificar</option>{tiposCarga.map((tipo) => <option key={tipo.id} value={tipo.id}>{tipo.nombre}</option>)}</select></label>
</div></section>
        <section aria-labelledby="carga"><h2 id="carga">Carga y período</h2><div className="formulario-grilla"><CampoNumerico etiqueta="Peso declarado (kg)" valor={formulario.pesoKg} alCambiar={(valor) => cambiar('pesoKg', valor)} decimal /><CampoNumerico etiqueta="Volumen declarado (m³)" valor={formulario.volumenM3} alCambiar={(valor) => cambiar('volumenM3', valor)} decimal /><label className="campo"><span>Inicio del viaje</span><input type="datetime-local" value={formulario.inicio} onChange={(e) => cambiar('inicio', e.target.value)} required /></label><label className="campo"><span>Fin del viaje</span><input type="datetime-local" value={formulario.fin} onChange={(e) => cambiar('fin', e.target.value)} required /></label></div></section>
        <section aria-labelledby="recorrido"><h2 id="recorrido" className="titulo-con-icono"><MapPin size={22} aria-hidden="true" />Recorrido</h2><div className="formulario-grilla"><label className="campo"><span>Origen</span><input value={formulario.origen} onChange={(e) => cambiar('origen', e.target.value)} maxLength={200} required /></label><label className="campo"><span>Destino</span><input value={formulario.destino} onChange={(e) => cambiar('destino', e.target.value)} maxLength={200} required /></label><CampoNumerico etiqueta="Latitud de origen (opcional)" valor={formulario.origenLat} alCambiar={(valor) => cambiar('origenLat', valor)} decimal obligatorio={false} min={-90} max={90} /><CampoNumerico etiqueta="Longitud de origen (opcional)" valor={formulario.origenLon} alCambiar={(valor) => cambiar('origenLon', valor)} decimal obligatorio={false} min={-180} max={180} /><CampoNumerico etiqueta="Latitud de destino (opcional)" valor={formulario.destinoLat} alCambiar={(valor) => cambiar('destinoLat', valor)} decimal obligatorio={false} min={-90} max={90} /><CampoNumerico etiqueta="Longitud de destino (opcional)" valor={formulario.destinoLon} alCambiar={(valor) => cambiar('destinoLon', valor)} decimal obligatorio={false} min={-180} max={180} /></div></section>
        <div className="formulario-botones"><button type="submit" disabled={enviando || cargandoCatalogos || vehiculos.length === 0 || choferes.length === 0 || rubros.length === 0}>{cargandoCatalogos ? 'Cargando catálogos…' : enviando ? 'Registrando…' : 'Registrar guía'}</button></div>
      </form>
    </main><PieSitio /></div>
}


