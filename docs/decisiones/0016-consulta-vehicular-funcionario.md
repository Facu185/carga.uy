# ADR-0016 — Consulta vehicular F13 separada del ABM

**Estado:** Aceptada para C13 por confirmación del usuario
**Fecha:** 2026-10-22

## Contexto

La fase 4 requiere fiscalizar vehículos, permiso e ITV desde el área del
funcionario. El ABM actual autoriza administrador o responsable activo; ampliar
sus roles habilitaría operaciones ajenas a esta consulta. El usuario confirmó
el contrato siguiente; esto no acredita aceptación integrada de otros frentes.

## Decisión

- Publicar C13 como `GET /carga/api/vehiculos/consulta`, con servicio local
  `ConsultaVehiculosLocal.consultar(Long empresaId, String matricula,
  Integer pagina, Integer tamano)` independiente del ABM. Solo
  FUNCIONARIO/ADMINISTRADOR, sin vínculo empresarial. Autorizar antes de consultar
  existencia. No modificar autenticación, derechos del responsable ni guías.
- `empresaId` positivo obligatorio; incluir empresas inactivas y vehículos dados
  de baja. Empresa inexistente: 400. Matrícula opcional recortada, coincidencia
  parcial literal sin distinguir mayúsculas (`%`, `_` y `!` son literales).
- Página base 0, tamaño por defecto 20, rango 1–100 y desplazamiento máximo
  `Integer.MAX_VALUE`; orden estable matrícula/ID. Página fuera de los resultados:
  registros vacíos con total conservado. Conteo y página no prometen una instantánea
  frente a modificaciones concurrentes.
- Respuesta `PaginaConsultaVehiculosDTO`: `registros`, `pagina`, `tamano`, `total`,
  `fechaReferencia` ISO-8601 y `zonaHoraria` (`America/Montevideo`). Cada
  `ConsultaVehiculoDTO` contiene `id`, `empresaId`, `matricula`, `habilitado`,
  `numeroPermiso`, `permisoDesde`, `permisoHasta`, `itvVencimiento`,
  `estadoPermiso` y `estadoItv`. No exponer entidades ni datos personales.
- Calcular en servidor con una única fecha de referencia por respuesta. Permiso:
  `FUTURO` antes del inicio, `VENCIDO` después del fin, `VIGENTE` en ambos límites
  inclusive; `SIN_DATOS` si falta número/fecha o el intervalo es inconsistente.
  ITV: `SIN_DATOS` sin vencimiento, `VENCIDO` después, `VIGENTE` hasta el día
  inclusive. No inventar un inicio de ITV ni deducir baja de una fecha vencida.
  `habilitado` representa exclusivamente la habilitación/baja lógica registrada.
- Errores `{"error":"mensaje"}`: 400 filtros, números no interpretables,
  paginación o empresa inexistente; 401 sin sesión válida; 403 sin rol.
  No cambiar los contratos ni mappers anteriores.
- Pantalla de solo lectura `/funcionario/empresas/:id/vehiculos`, accesible desde
  el detalle F12, con filtro/paginación y estados carga/vacío/error. La SPA no
  calcula vigencia con su reloj ni ofrece mutaciones. API también admite al
  administrador; sus recorridos anteriores permanecen intactos.

## Alternativas y consecuencias

- Ampliar roles del ABM: menos código, pero mezcla permisos y puede conceder
  escritura; descartado.
- Filtrar en el navegador: reutiliza un listado completo, pero no proporciona
  autorización/paginación propias ni fecha de servidor; descartado.
- Servicio y DTO de lectura separados: añade contrato y mapeo pequeños, conserva
  los consumidores existentes y hace explícita la frontera de fiscalización.

Sin cambios de esquema ni dependencias. Pruebas de roles efectivos, JPQL y
navegador pendientes; no se ejecutan builds ni se declara ciclo rojo/verde.
