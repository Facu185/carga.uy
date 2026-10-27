# F13 / C13 — Consulta vehicular de solo lectura

Escenarios definidos antes de la implementación, tras confirmación del usuario
el 2026-10-22: [ADR-0016](../docs/decisiones/0016-consulta-vehicular-funcionario.md),
[contrato C13](../docs/contratos.md#consulta-vehicular-f13--c13).
No hay runner frontend; esta matriz NO son pruebas automatizadas. No se ejecutan
builds ni tests que compilen. No se acredita ciclo rojo/verde.

## Matriz pendiente de ejecución en WildFly/PostgreSQL y navegador

Usar entorno aislado, identidad y datos sintéticos. No guardar tokens ni datos
personales en evidencias. Registrar revisión, fecha, resultado y referencia segura.

| Escenario | Resultado esperado |
| --- | --- |
| Funcionario sin vínculo empresarial | Detalle F12 → Vehículos → consulta paginada de esa empresa, solo lectura. |
| Administrador por API | C13 200 sin vínculo; ABM/JSF anteriores intactos. |
| Ciudadano/chofer/responsable/autoridad sin roles autorizados | REST C13 403 incluso para empresa inexistente; URL SPA denegada sin consulta. |
| Sin sesión / token inválido o vencido | REST 401; SPA redirige por flujo existente, sin conservar filas operativas. |
| Funcionario solo y funcionario con CIUDADANO/responsabilidad heredada | C13 funciona; GET ABM y POST/PUT/DELETE/reactivar no obtienen permisos nuevos. Reejecutar regresión F11/ADR-0015. |
| Empresa inactiva / vehículo dado de baja | Ambos consultables; baja lógica independiente de vigencia. |
| Empresa inexistente / ID no positivo, ausente, decimal o desbordado | 400 para rol autorizado, sobre error existente; no se inventa 404. |
| Matrícula parcial en distintas mayúsculas, vacía, espacios, `%`, `_`, `!` | Coincidencia parcial literal; espacios exteriores recortados, vacío sin filtro; conteo/página usan mismo filtro. |
| Varias empresas / matrículas con mismo prefijo | No filtrar datos de otra empresa. Orden matrícula/ID estable y sin duplicados entre páginas sin escrituras concurrentes. |
| Paginación omitida, límites 1/100, negativa, 0 de tamaño, 101, decimal, desbordamiento | Defaults 0/20; inválidos 400; desplazamiento dentro de entero 32 bits. |
| Página fuera de resultados / empresa sin flota | Lista vacía y total conservado; botón anterior disponible para volver si corresponde. |
| Permiso futuro, vigente, vencido | Estados del servidor y fechas visibles; inicio/fin inclusive. |
| ITV vence hoy / ayer / dato ausente | Vigente / vencida / sin datos. No inventar inicio ITV. |
| Permiso sin número/fecha o intervalo invertido | Sin datos; no borrar ni normalizar registros históricos ni tratarlos como baja. |
| Cambio de día UTC antes de medianoche en Montevideo / navegador en otra zona | Una referencia por respuesta: fecha servidor America/Montevideo; la SPA no usa su reloj para vigencia. |
| Filtro o página con respuesta anterior demorada / cambio de ruta o sesión | Nunca mostrar filas anteriores bajo encabezado nuevo; abortar/ignorar resultados tardíos. |
| Error de red/5xx/DTO no interpretable y reconsulta | Error explícito sin filas anteriores; reintento exclusivamente de lectura. |
| URL directa, recarga, atrás/adelante | Misma empresa, filtro y página según URL. ID inválido no consulta. |
| Teclado, zoom y pantalla estrecha | Etiquetas, foco visible, tabla desplazable, estados no solo por color y resumen anunciado. |
| Viajes F14 (actualizado fase 5, 2026-10-27) | Enlace a consulta C14 P2; fuentes P3/P4 pendientes explícitas. Validar por la [matriz C14](p2-consulta-viajes.md), sin simular viajes/pesajes. |

## Evidencia automatizada escrita y límites

- `ConsultaVehiculosBeanTest`: autorización explícita antes de existencia,
  filtros/paginación, DTO, inclusividad, datos ausentes y referencia horaria con
  reloj fijo. Dobles fallan ante escrituras; no prueban JPQL ni roles EJB reales.
- `ConsultaVehiculosRecursoTest`: ruta/delegación y sintaxis numérica 400 sin
  conversiones JAX-RS implícitas. No acredita HTTP/JSON-B.
- Conservar `P2ContraDespliegueTest` (F11) y las pruebas existentes de autorización
  y baja/traslado/guías del responsable y administrador.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
  No se han hecho llamadas HTTP ni validaciones funcionales de F13.
