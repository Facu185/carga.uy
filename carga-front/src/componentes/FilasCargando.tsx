/** Anchos variados para que las filas de espera no se vean como una grilla perfecta. */
const ANCHOS = [70, 52, 82, 44, 63, 58, 75]

/**
 * Filas de espera con la forma de la tabla mientras llegan los datos, en vez de una tabla vacía
 * o un "todavía no hay" que después desaparece. Los lectores de pantalla las ignoran: la tabla
 * se marca con aria-busy mientras carga.
 */
export function FilasCargando({ columnas, filas = 4 }: { columnas: number; filas?: number }) {
  return (
    <>
      {Array.from({ length: filas }, (_, fila) => (
        <tr key={fila} className="fila-cargando" aria-hidden="true">
          {Array.from({ length: columnas }, (_, columna) => (
            <td key={columna}>
              <span className="esqueleto" style={{ width: `${ANCHOS[(fila * 3 + columna) % ANCHOS.length]}%` }} />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}
