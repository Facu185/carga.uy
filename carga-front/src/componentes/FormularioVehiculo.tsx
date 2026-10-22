import { useEffect, useRef, useState, type FormEvent, type HTMLInputTypeAttribute } from 'react'
import type { DatosVehiculo, Vehiculo } from '../tipos'

type ValoresFormulario = Record<keyof DatosVehiculo, string>

interface CampoVehiculo {
  nombre: keyof ValoresFormulario
  etiqueta: string
  tipo?: HTMLInputTypeAttribute
  obligatorio?: boolean
  longitud?: number
  minimo?: number
  maximo?: number
  paso?: number
}

const CAMPOS: CampoVehiculo[] = [
  { nombre: 'matricula', etiqueta: 'Matrícula', obligatorio: true, longitud: 20 },
  { nombre: 'marca', etiqueta: 'Marca', longitud: 60 },
  { nombre: 'modelo', etiqueta: 'Modelo', longitud: 60 },
  { nombre: 'pesoKg', etiqueta: 'Peso del vehículo vacío (kg)', tipo: 'number', obligatorio: true, minimo: 0, maximo: 2147483647, paso: 1 },
  { nombre: 'capacidadCargaKg', etiqueta: 'Capacidad de carga (kg)', tipo: 'number', obligatorio: true, minimo: 1, maximo: 2147483647, paso: 1 },
  { nombre: 'capacidadVolumenM3', etiqueta: 'Capacidad volumétrica (m³)', tipo: 'number', obligatorio: true, minimo: 0.01, maximo: 9999999999.99, paso: 0.01 },
  { nombre: 'numeroPermiso', etiqueta: 'Número de permiso de circulación', obligatorio: true, longitud: 50 },
  { nombre: 'permisoDesde', etiqueta: 'Permiso vigente desde', tipo: 'date', obligatorio: true },
  { nombre: 'permisoHasta', etiqueta: 'Permiso vigente hasta', tipo: 'date', obligatorio: true },
  { nombre: 'itvVencimiento', etiqueta: 'Vencimiento de ITV', tipo: 'date' },
]

function valoresIniciales(vehiculo: Vehiculo | null): ValoresFormulario {
  return {
    matricula: vehiculo?.matricula ?? '',
    marca: vehiculo?.marca ?? '',
    modelo: vehiculo?.modelo ?? '',
    pesoKg: vehiculo ? String(vehiculo.pesoKg) : '',
    capacidadCargaKg: vehiculo ? String(vehiculo.capacidadCargaKg) : '',
    capacidadVolumenM3: vehiculo?.capacidadVolumenM3 == null ? '' : String(vehiculo.capacidadVolumenM3),
    numeroPermiso: vehiculo?.numeroPermiso ?? '',
    permisoDesde: vehiculo?.permisoDesde ?? '',
    permisoHasta: vehiculo?.permisoHasta ?? '',
    itvVencimiento: vehiculo?.itvVencimiento ?? '',
  }
}

interface Props {
  vehiculo: Vehiculo | null
  ocupado: boolean
  error: string | null
  onGuardar: (datos: DatosVehiculo) => Promise<void>
  onCancelar: () => void
}

/** Validación HTML para orientar; las reglas y la autorización siguen en el EJB. */
export function FormularioVehiculo({ vehiculo, ocupado, error, onGuardar, onCancelar }: Props) {
  const [valores, setValores] = useState(() => valoresIniciales(vehiculo))
  const matricula = useRef<HTMLInputElement>(null)
  useEffect(() => { matricula.current?.focus() }, [])

  async function guardar(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault()
    if (ocupado) return
    await onGuardar({
      matricula: valores.matricula.trim(), marca: valores.marca.trim() || null, modelo: valores.modelo.trim() || null,
      pesoKg: Number(valores.pesoKg), capacidadCargaKg: Number(valores.capacidadCargaKg),
      capacidadVolumenM3: Number(valores.capacidadVolumenM3), numeroPermiso: valores.numeroPermiso.trim(),
      permisoDesde: valores.permisoDesde, permisoHasta: valores.permisoHasta, itvVencimiento: valores.itvVencimiento || null,
    })
  }

  return (
    <section className="ficha ficha-ancha" aria-labelledby="titulo-formulario-vehiculo">
      <h2 id="titulo-formulario-vehiculo">{vehiculo ? `Editar ${vehiculo.matricula}` : 'Nuevo vehículo'}</h2>
      <p id="ayuda-formulario-vehiculo">Los campos con * son obligatorios. Se registran datos, no archivos. Sin ITV vigente no podrá asignarse a nuevas guías.</p>
      <form onSubmit={(evento) => void guardar(evento)} className="formulario-grilla" aria-busy={ocupado} aria-describedby={`ayuda-formulario-vehiculo${error ? ' error-vehiculos' : ''}`}>
        {CAMPOS.map(({ nombre, etiqueta, tipo, obligatorio, longitud, minimo, maximo, paso }) => (
          <label className="campo" key={nombre}>
            <span>{etiqueta}{obligatorio ? ' *' : ''}</span>
            <input
              ref={nombre === 'matricula' ? matricula : undefined}
              name={nombre} type={tipo ?? 'text'} value={valores[nombre]} required={obligatorio}
              maxLength={longitud} min={nombre === 'permisoHasta' ? valores.permisoDesde || undefined : minimo}
              max={maximo} step={paso} disabled={ocupado}
              onChange={(evento) => setValores({ ...valores, [nombre]: evento.target.value })}
            />
          </label>
        ))}
        <div className="formulario-botones">
          <button type="submit" disabled={ocupado}>{ocupado ? 'Guardando…' : vehiculo ? 'Guardar cambios' : 'Registrar vehículo'}</button>
          <button type="button" className="boton-secundario" disabled={ocupado} onClick={onCancelar}>Cancelar</button>
        </div>
      </form>
    </section>
  )
}
