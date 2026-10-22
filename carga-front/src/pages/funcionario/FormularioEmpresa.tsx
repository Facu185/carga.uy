import type { FormEvent } from 'react'

export type ValoresEmpresa = { nombre: string; razonSocial: string; numeroEmpresa: string; direccion: string; fechaAlta: string }

interface Props {
  valores: ValoresEmpresa
  bloqueado: boolean
  ocupado: boolean
  errorId?: string
  cambiar: (valores: ValoresEmpresa) => void
  guardar: (evento: FormEvent<HTMLFormElement>) => void
  cancelar: () => void
}

/** Cinco campos administrativos C12; no se puede cambiar ID ni reactivar desde este formulario. */
export function FormularioEmpresa({ valores, bloqueado, ocupado, errorId, cambiar, guardar, cancelar }: Props) {
  return (
    <form className="formulario-grilla" onSubmit={guardar} aria-busy={ocupado}
      aria-describedby={`ayuda-editar-empresa${errorId ? ` ${errorId}` : ''}`}>
      <p id="ayuda-editar-empresa">Los campos con * son obligatorios. Si dejas la fecha de alta vacía, el servicio administrativo la completa con la fecha actual.</p>
      <label className="campo"><span>Nombre *</span><input name="nombre" autoFocus required maxLength={200} value={valores.nombre}
        disabled={bloqueado} onChange={(evento) => cambiar({ ...valores, nombre: evento.target.value })} /></label>
      <label className="campo"><span>Razón social</span><input name="razonSocial" maxLength={200} value={valores.razonSocial}
        disabled={bloqueado} onChange={(evento) => cambiar({ ...valores, razonSocial: evento.target.value })} /></label>
      <label className="campo"><span>Número de empresa *</span><input name="numeroEmpresa" type="number" required min={1} max={Number.MAX_SAFE_INTEGER} step={1}
        value={valores.numeroEmpresa} disabled={bloqueado} onChange={(evento) => cambiar({ ...valores, numeroEmpresa: evento.target.value })} /></label>
      <label className="campo"><span>Dirección</span><input name="direccion" maxLength={300} value={valores.direccion}
        disabled={bloqueado} onChange={(evento) => cambiar({ ...valores, direccion: evento.target.value })} /></label>
      <label className="campo"><span>Fecha de alta</span><input name="fechaAlta" type="date" value={valores.fechaAlta}
        disabled={bloqueado} onChange={(evento) => cambiar({ ...valores, fechaAlta: evento.target.value })} /></label>
      <div className="formulario-botones"><button type="submit" disabled={bloqueado}>{ocupado ? 'Procesando…' : 'Guardar cambios'}</button>
        <button type="button" className="boton-secundario" disabled={bloqueado} onClick={cancelar}>Cancelar cambios</button></div>
    </form>
  )
}
