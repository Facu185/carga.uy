import { ClipboardList } from 'lucide-react'
import { useEffect, useState } from 'react'
import { api } from '../../api/cliente'
import { useAuth } from '../../auth/contexto'
import { EncabezadoSitio } from '../../componentes/EncabezadoSitio'
import { Etiqueta } from '../../componentes/Etiqueta'
import { FilasCargando } from '../../componentes/FilasCargando'
import { NavegacionEmpresa } from '../../componentes/NavegacionEmpresa'
import { PieSitio } from '../../componentes/PieSitio'
import { formatearMomento } from '../../formato'
import type { PaginaGuiasEmpresa, VinculoEmpresa } from '../../tipos'
import { useTituloPagina } from '../../titulo'

const POR_PAGINA = 20

/** El selector no autoriza: el central comprueba RESPONSABLE/ACTIVO en cada consulta. */
export function GuiasEmpresa() {
  useTituloPagina('Guías de viajes de la empresa')
  const { sesion, logout } = useAuth()
  const [empresas, setEmpresas] = useState<VinculoEmpresa[] | null>(null)
  const [empresaId, setEmpresaId] = useState('')
  const [pagina, setPagina] = useState(0)
  const [version, setVersion] = useState(0)
  const [versionEmpresas, setVersionEmpresas] = useState(0)
  const [errorEmpresas, setErrorEmpresas] = useState<string | null>(null)
  const [respuesta, setRespuesta] = useState<{ clave: string; datos: PaginaGuiasEmpresa } | null>(null)
  const [fallo, setFallo] = useState<{ clave: string; mensaje: string } | null>(null)

  useEffect(() => {
    let vigente = true
    setErrorEmpresas(null)
    api<VinculoEmpresa[]>('/solicitudes-empresa/vinculos/mios')
      .then((vinculos) => {
        if (!vigente) return
        const responsables = vinculos.filter((vinculo) => vinculo.tipo === 'RESPONSABLE' && vinculo.estado === 'ACTIVO')
        setEmpresas(responsables)
        setEmpresaId(responsables[0] ? String(responsables[0].empresaId) : '')
        setPagina(0)
      })
      .catch((causa: unknown) => {
        if (vigente) setErrorEmpresas(causa instanceof Error ? causa.message : 'No se pudieron cargar tus empresas.')
      })
    return () => { vigente = false }
  }, [versionEmpresas])

  const clave = `${empresaId}:${pagina}:${version}`
  useEffect(() => {
    if (!empresaId) return
    let vigente = true
    const parametros = new URLSearchParams({ empresaId, pagina: String(pagina), tamano: String(POR_PAGINA) })
    api<PaginaGuiasEmpresa>(`/guias/empresa?${parametros}`)
      .then((datos) => { if (vigente) setRespuesta({ clave, datos }) })
      .catch((causa: unknown) => {
        if (vigente) setFallo({ clave, mensaje: causa instanceof Error ? causa.message : 'No se pudieron cargar las guías.' })
      })
    return () => { vigente = false }
  }, [empresaId, pagina, version, clave])

  // No presentar filas de otra empresa/página mientras llega la respuesta actual.
  const datos = respuesta?.clave === clave ? respuesta.datos : null
  const errorGuias = fallo?.clave === clave ? fallo.mensaje : null
  const cargando = Boolean(empresaId) && !datos && !errorGuias
  const paginas = datos ? Math.max(1, Math.ceil(datos.total / POR_PAGINA)) : 1

  function cambiarEmpresa(id: string) {
    setEmpresaId(id)
    setPagina(0)
    setVersion((actual) => actual + 1)
  }

  function cambiarPagina(numero: number) {
    setPagina(numero)
    setVersion((actual) => actual + 1)
  }

  return (
    <div className="pagina">
      <EncabezadoSitio>
        <span className="sitio-usuario">{sesion?.nombre}</span>
        <button type="button" className="boton-secundario boton-chico" onClick={logout}>Salir</button>
      </EncabezadoSitio>
      <main className="pagina-contenido empresa-contenido">
        <NavegacionEmpresa />
        <header className="encabezado-guia">
          <span className="tarjeta-seccion-icono" aria-hidden="true"><ClipboardList size={24} /></span>
          <div>
            <h1>Guías de viajes de la empresa</h1>
            <p>Todas las guías, incluidas las finalizadas, con el chofer y el vehículo asignados.</p>
          </div>
        </header>

        {!empresas && !errorEmpresas && <p role="status">Cargando tus empresas…</p>}
        {errorEmpresas && (
          <div>
            <p className="mensaje-error" role="alert">{errorEmpresas}</p>
            <button type="button" className="boton-secundario" onClick={() => setVersionEmpresas((actual) => actual + 1)}>Reintentar</button>
          </div>
        )}
        {empresas?.length === 0 && <p role="status">No tenés un vínculo de responsable activo con ninguna empresa.</p>}
        {empresas && empresas.length > 0 && (
          <>
            <div className="barra-filtros">
              <label className="campo">
                <span>Empresa</span>
                <select value={empresaId} onChange={(evento) => cambiarEmpresa(evento.target.value)}>
                  {empresas.map((empresa) => <option key={empresa.id} value={empresa.empresaId}>{empresa.nombreEmpresa}</option>)}
                </select>
              </label>
              <button type="button" className="boton-secundario" disabled={cargando} onClick={() => setVersion((actual) => actual + 1)}>{errorGuias ? 'Reintentar' : 'Actualizar'}</button>
            </div>
            {errorGuias && <p className="mensaje-error" role="alert">{errorGuias}</p>}
            <div className="tabla-contenedor">
              <table className="tabla" aria-busy={cargando}>
                <caption>Guías de {empresas.find((empresa) => String(empresa.empresaId) === empresaId)?.nombreEmpresa}</caption>
                <thead><tr><th scope="col">Guía / vehículo</th><th scope="col">Chofer asignado</th><th scope="col">Recorrido</th><th scope="col">Período previsto</th><th scope="col">Estado</th></tr></thead>
                <tbody>
                  {cargando && <FilasCargando columnas={5} />}
                  {datos?.registros.map(({ guia, nombreChofer, matricula }) => (
                    <tr key={guia.id}>
                      <td data-etiqueta="Guía / vehículo"><strong>N.º {guia.id}</strong><span className="celda-secundaria">{matricula ?? (guia.vehiculoId === null ? 'Sin vehículo asignado' : `Vehículo #${guia.vehiculoId}`)}</span></td>
                      <td data-etiqueta="Chofer asignado"><strong>{nombreChofer ?? (guia.choferId === null ? 'Sin chofer asignado' : 'Sin nombre registrado')}</strong>{guia.choferId !== null && <span className="celda-secundaria">Chofer #{guia.choferId}</span>}</td>
                      <td data-etiqueta="Recorrido"><strong>{guia.origen}</strong><span className="celda-secundaria">{guia.destino}</span></td>
                      <td data-etiqueta="Período previsto">{formatearMomento(guia.inicio)}<span className="celda-secundaria">hasta {formatearMomento(guia.fin)}</span></td>
                      <td data-etiqueta="Estado"><Etiqueta tono={guia.estado === 'FINALIZADA' ? 'neutro' : 'aviso'}>{guia.estado}</Etiqueta></td>
                    </tr>
                  ))}
                  {datos?.registros.length === 0 && <tr><td colSpan={5} className="tabla-vacia">{datos.total === 0 ? 'Esta empresa todavía no tiene guías registradas.' : 'No hay guías en esta página. Volvé a la página anterior.'}</td></tr>}
                </tbody>
              </table>
            </div>
            {datos && datos.total > 0 && (
              <nav className="paginacion" aria-label="Páginas de guías de viajes">
                <p className="paginacion-resumen" role="status">{datos.total} guías, de la fecha de inicio más reciente a la más antigua</p>
                <div className="acciones">
                  <button type="button" className="boton-secundario boton-chico" disabled={pagina === 0} onClick={() => cambiarPagina(pagina - 1)}>Anterior</button>
                  <span className="paginacion-numero">Página {pagina + 1} de {paginas}</span>
                  <button type="button" className="boton-secundario boton-chico" disabled={pagina + 1 >= paginas} onClick={() => cambiarPagina(pagina + 1)}>Siguiente</button>
                </div>
              </nav>
            )}
          </>
        )}
      </main>
      <PieSitio />
    </div>
  )
}
