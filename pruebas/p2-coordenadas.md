# P2 — Regresión del formulario de coordenadas

## Alcance y estado

Corrección puntual autorizada de `NuevaGuia.tsx`, formulario de creación P2 en el
frontoffice P3. No cambia sesión, navegación, eventos, contratos ni validaciones
de negocio del central. La validación HTML es una ayuda al usuario, NO sustituye
la validación del servidor.

Escenarios definidos antes del parche. Estado de todos: **PENDIENTE de ejecución
por el equipo** en navegador sobre un frontend y central desplegados con estos
cambios. No hay runner de pruebas frontend en `carga-front/package.json`; no se
agregan dependencias. No se ejecutaron builds, navegador ni ciclo rojo/verde;
estos escenarios no constituyen pruebas automatizadas ni TDD verificado.

## Preparación

- Ingresar por el mecanismo P1 existente como responsable activo de una empresa
  sintética con vehículo y chofer habilitados y catálogos disponibles.
- Completar los demás campos con una guía válida: carga dentro de capacidad,
  permiso/ITV vigentes, origen y destino distintos y período libre.
- Usar otro período libre para cada creación confirmada; no reutilizar uno que
  produzca un rechazo de solapamiento ajeno a esta regresión.
- Observar las solicitudes de red sin publicar tokens, cookies ni datos reales.

## Escenarios de aceptación

| Caso | Acción | Resultado esperado |
| --- | --- | --- |
| COORD-01 — Uruguay | Origen `-34.9011`, `-56.1645`; destino `-34.4533`, `-56.3900`; enviar | Ningún campo presenta `rangeUnderflow`, `rangeOverflow` ni `stepMismatch`; se permite enviar con los cuatro números negativos. Antes del parche `min=0` rechazaba esas coordenadas. |
| COORD-02 — Límites incluidos | Probar `-90` y `90` en ambas latitudes, y `-180` y `180` en ambas longitudes | `checkValidity()` de cada campo es verdadero con datos restantes válidos. Esta comprobación de límites del formulario no exige crear viajes con destinos geográficos extremos. |
| COORD-03 — Fuera de rango | Probar por separado `-90.0001` y `90.0001` en cada latitud; `-180.0001` y `180.0001` en cada longitud; intentar enviar | Campo inválido por límite inferior/superior; el envío normal del formulario no produce POST mientras ese valor permanezca. |
| COORD-04 — Coordenadas vacías | Dejar los cuatro campos vacíos; enviar una guía válida | No hay error `valueMissing` en coordenadas. Las cuatro propiedades siguen omitidas del JSON, no se convierten a cero ni a cadena vacía. |
| COORD-05 — Decimal y cero | Probar `0` y valores decimales dentro del rango en los cuatro campos | Campo válido; se conserva `step="any"`. Cero se serializa como número `0`, no se omite. |
| COORD-06 — Peso/volumen | Probar `-1` por separado en peso y volumen | Continúan inválidos por `min=0`; el cambio de coordenadas no permite carga negativa. Ambos mantienen su obligatoriedad y paso decimal. |
| COORD-07 — Contrato | En COORD-01 y COORD-04 inspeccionar `POST /carga/api/guias` | Mismos nombres y tipos de campos y mismo endpoint. Las coordenadas presentes son números con signo conservado; las vacías se omiten. No aparecen campos nuevos ni cambia el resto del payload. |
| COORD-08 — Persistencia | Crear COORD-01 y consultar luego la guía desde el listado empresarial y, como chofer asignado, desde `GET /carga/api/guias/mis-guias` | El ID creado aparece en las consultas autorizadas; verificar en el `GuiaDTO` recibido las cuatro coordenadas con el mismo valor numérico. No exigir ceros decimales finales en JSON ni que la pantalla muestre todas las coordenadas. |

Registrar para cada escenario fecha, versión desplegada, navegador, ID sintético
de guía cuando exista, resultado real y evidencia sanitizada. Un envío aceptado
por HTML no demuestra persistencia ni autorización: COORD-08 requiere el central.

## Referencias

- [Matriz general de cierre P2](p2-cierre.md).
- [Caso crítico 2](../docs/GuiaCUCrit.md#caso-de-uso-crítico-2--gestionar-viajes-de-carga-crear-guía).
- [Contrato Guías P2](../docs/contratos.md#guías-p2).
- [ADR-0004: estado de la SPA](../docs/decisiones/0004-frontend-react-sobre-api-rest.md).
- [ADR-0011: conservar ABM JSF](../docs/decisiones/0011-autenticacion-del-abm-jsf.md).
