import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { consultarEmpresas, type EmpresaAdministrativa } from '../../api/empresas'
import { useTituloPagina } from '../../titulo'

type Carga = { estado: 'cargando' } | { estado: 'error'; mensaje: string }
  | { estado: 'lista'; empresas: EmpresaAdministrativa[]; filtro: string }

export function EmpresasFuncionario() {
  const [parametros] = useSearchParams()
  const filtro = (parametros.get('nombre') ?? '').trim()
  return <ListadoEmpresas key={filtro} filtro={filtro} />
}

function ListadoEmpresas({ filtro }: { filtro: string }) {
  useTituloPagina('Empresas · Área del funcionario')
  const [, setParametros] = useSearchParams()
  const [nombre, setNombre] = useState(filtro)
  const [carga, setCarga] = useState<Carga>({ estado: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const controlador = new AbortController()
    let vigente = true
    setNombre(filtro); setCarga({ estado: 'cargando' })
    consultarEmpresas(filtro, controlador.signal)
      .then((empresas) => { if (vigente) setCarga({ estado: 'lista', empresas, filtro }) })
      .catch((causa: unknown) => {
        if (vigente) setCarga({ estado: 'error', mensaje: causa instanceof Error ? causa.message : 'No se pudieron consultar las empresas.' })
      })
    return () => { vigente = false; controlador.abort() }
  }, [filtro, version])

  function buscar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    setCarga({ estado: 'cargando' })
    setParametros(nombre.trim() ? { nombre: nombre.trim() } : {})
    setVersion((valor) => valor + 1)
  }

  const empresas = carga.estado === 'lista' && carga.filtro === filtro ? carga.empresas : null
  return (
    <>
      <header className="seccion-encabezado"><div><h1>Empresas</h1>
        <p>Consulta y administración de empresas existentes, incluidas las inactivas.</p></div></header>
      <form className="formulario-grilla" onSubmit={buscar}>
        <label className="campo"><span>Buscar por nombre</span>
          <input name="nombre" value={nombre} onChange={(evento) => setNombre(evento.target.value)} /></label>
        <div className="formulario-botones"><button type="submit">Buscar</button>
          <button type="button" className="boton-secundario" onClick={() => {
            setNombre(''); setCarga({ estado: 'cargando' }); setParametros({}); setVersion((valor) => valor + 1)
          }}>Mostrar todas</button></div>
      </form>
      <p>El servicio actual devuelve el listado completo, sin paginación. El alta empresarial no se realiza desde esta pantalla.</p>
      {carga.estado === 'error' ? <div><p className="mensaje-error" role="alert">{carga.mensaje}</p>
        <button type="button" className="boton-secundario" onClick={() => {
          setCarga({ estado: 'cargando' }); setVersion((valor) => valor + 1)
        }}>Volver a consultar</button></div>
        : !empresas ? <p role="status">Consultando empresas…</p>
          : empresas.length === 0 ? <p role="status">No hay empresas para esta búsqueda.</p>
            : <div className="tabla-contenedor"><table className="tabla">
              <caption>{empresas.length} empresas encontradas</caption>
              <thead><tr><th scope="col">Empresa</th><th scope="col">Número</th><th scope="col">Estado</th><th scope="col">Acciones</th></tr></thead>
              <tbody>{empresas.map((empresa) => <tr key={empresa.id}>
                <td data-etiqueta="Empresa">{empresa.nombre}</td>
                <td data-etiqueta="Número">{Number.isSafeInteger(empresa.numeroEmpresa) ? empresa.numeroEmpresa : 'Fuera del rango seguro del portal'}</td>
                <td data-etiqueta="Estado">{empresa.activa ? 'Activa' : 'Inactiva'}</td>
                <td data-etiqueta="Acciones"><Link to={`/funcionario/empresas/${empresa.id}`} aria-label={`Ver detalle de ${empresa.nombre}`}>Ver detalle</Link></td>
              </tr>)}</tbody>
            </table></div>}
    </>
  )
}
