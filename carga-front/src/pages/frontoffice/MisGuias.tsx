import { ClipboardList, MapPin, Truck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { api, descargarArchivo } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FilasCargando } from '../../componentes/FilasCargando'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearMomento } from '../../formato'
import type { Guia } from '../../tipos'
import { useTituloPagina } from '../../titulo'

/** Consulta P2: el central deriva el chofer desde la sesión, no desde el cliente. */
export function MisGuias() {
  useTituloPagina('Mis guías')
  const { sesion, logout } = useAuth(); const [guias, setGuias] = useState<Guia[]>([]); const [cargando, setCargando] = useState(true); const [error, setError] = useState<string | null>(null)
  useEffect(() => { let vigente = true; api<Guia[]>('/guias/mis-guias').then((respuesta) => vigente && setGuias(respuesta)).catch((causa: unknown) => vigente && setError(causa instanceof Error ? causa.message : 'No se pudieron cargar tus guías.')).finally(() => vigente && setCargando(false)); return () => { vigente = false } }, [])

  async function descargar() {
    try {
      setError(null)
      const archivo = await descargarArchivo('/guias/mis-guias/descarga')
      const url = URL.createObjectURL(archivo)
      const enlace = document.createElement('a')
      enlace.href = url; enlace.download = 'mis-guias.csv'; enlace.click(); URL.revokeObjectURL(url)
    } catch (causa) { setError(causa instanceof Error ? causa.message : 'No se pudieron descargar tus guías.') }
  }
  return <div className="pagina"><EncabezadoSitio><span className="sitio-usuario">{sesion?.nombre}</span><button type="button" className="boton-secundario boton-chico" onClick={logout}>Salir</button></EncabezadoSitio><main className="pagina-contenido contenido-guia"><a className="volver" href="/mi-cuenta">Volver a Mi cuenta</a><header className="encabezado-guia"><span className="tarjeta-seccion-icono" aria-hidden="true"><ClipboardList size={24} /></span><div><h1>Mis guías de carga</h1><p>Estas son las guías asignadas a tu cédula. Nadie puede consultar las de otro chofer desde esta pantalla.</p></div><button type="button" className="boton-secundario" onClick={() => void descargar()} disabled={cargando}>Descargar CSV</button></header>{error && <p className="mensaje-error" role="alert">{error}</p>}<div className="tabla-contenedor"><table className="tabla" aria-busy={cargando}><thead><tr><th>Guía</th><th>Recorrido</th><th>Período</th><th>Estado</th></tr></thead><tbody>{cargando && <FilasCargando columnas={4} />}{guias.map((guia) => <tr key={guia.id}><td data-etiqueta="Guía"><strong>N.º {guia.id}</strong><span className="celda-secundaria"><Truck size={15} aria-hidden="true" /> Vehículo #{guia.vehiculoId}</span></td><td data-etiqueta="Recorrido"><strong>{guia.origen}</strong><span className="celda-secundaria"><MapPin size={15} aria-hidden="true" /> {guia.destino}</span></td><td data-etiqueta="Período">{formatearMomento(guia.inicio)}<span className="celda-secundaria">hasta {formatearMomento(guia.fin)}</span></td><td data-etiqueta="Estado"><Etiqueta tono="ok">{guia.estado}</Etiqueta></td></tr>)}{!cargando && guias.length === 0 && <tr><td colSpan={4} className="tabla-vacia">Todavía no tenés guías asignadas.</td></tr>}</tbody></table></div></main><PieSitio /></div>
}



