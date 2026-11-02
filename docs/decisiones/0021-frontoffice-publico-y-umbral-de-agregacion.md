# ADR-0021 — Frontoffice público: validación de permisos y umbral de agregación

**Estado:** Propuesta (requisito sin responsable asignado; la implementó Facundo, P4, y queda a
revisión del grupo)
**Fecha:** 2026-11-02

## Contexto

La letra pide, para cualquier persona y sin ingresar, **validar permisos de empresas** y **listados de
información pública**. La sección 4.2, punto 6, agrega que la información pública "debe estar
agregada de forma tal que no permita identificar a una empresa, un vehículo o un chofer", y que el
grupo debe "definir y justificar el umbral mínimo de agregación" (Ley 18.331).

Las dos cosas tiran en sentidos distintos: validar un permiso es, por definición, preguntar por *un*
vehículo o *una* empresa; publicar cifras exige que ninguna celda hable de uno solo.

## Decisión

1. **Validación por un identificador que la persona ya tiene**, nunca por búsqueda ni listado:
   - `GET /publico/permisos/vehiculo?matricula=` responde si el vehículo puede circular hoy:
     habilitado, empresa habilitada, permiso nacional e ITV vigentes, con sus fechas. **No dice a qué
     empresa pertenece ni quién lo maneja.**
   - `GET /publico/permisos/empresa?numero=` responde el nombre público y si está habilitada.
     No informa vehículos, choferes, guías ni infracciones.
   - Un identificador que no existe responde `encontrado: false`, no un error.
2. **Estadísticas solo agregadas** (`GET /publico/estadisticas`): empresas y vehículos habilitados,
   vehículos al día, y viajes y toneladas declaradas por mes, tipo de carga y rubro, de los últimos
   12 meses (por el inicio de la guía, en America/Montevideo).
3. **Umbral: una cifra se publica solo si reúne al menos 5 guías de al menos 3 empresas distintas**
   (un cero se publica siempre: no identifica a nadie).
   - **3 empresas** es la regla de los tres contribuyentes que usan las oficinas de estadística para
     datos de empresas: con 1 la cifra *es* la de esa empresa; con 2, cada una despeja la de la otra
     restando la propia.
   - **5 guías** evita que una celda describa uno o dos viajes concretos (con quién, qué y cuándo),
     que alguien del rubro podría reconocer aunque no figure el nombre. Es el valor de *k* más
     habitual en la publicación de microdatos agregados.
4. **Supresión secundaria.** Si en una tabla se reserva una sola celda, su valor sale restando del
   total, que se conoce por cualquier otra tabla. Por eso el conjunto reservado se agranda con las
   celdas publicadas más chicas hasta que tenga al menos dos celdas y, en conjunto, cumpla el umbral.
   - En tipo de carga y rubro, lo reservado se junta en una fila "Otros", que a su vez solo se
     publica si cumple.
   - En la serie mensual, los meses reservados aparecen como "Reservado".
   - "Vehículos al día" se publica solo si también su complemento (habilitados que no están al día)
     cumple el umbral; si no, se despejaría restando.
5. **Los ids de empresa de cada celda se calculan en la base y nunca salen del central**: sirven
   solo para contar empresas distintas.
6. Las páginas `/validar-permiso` y `/estadisticas` se agregan a la SPA, enlazadas desde la portada.
   Los servicios son `@PermitAll`, la única parte del central que se usa sin sesión.

## Alternativas consideradas

- **Listado público de empresas con sus vehículos y permisos:** es lo más útil para fiscalizar en la
  ruta, pero publica exactamente lo que la sección 4.2 prohíbe (identifica empresa y vehículo).
- **Solo umbral de cantidad (k = 5) sin contar empresas:** cinco viajes de una misma empresa
  identifican a esa empresa. Descartada.
- **Ruido aleatorio (privacidad diferencial):** protege mejor ante consultas repetidas, pero cada
  recarga mostraría cifras distintas y el tablero de autoridad no coincidiría con el público. Es
  desproporcionado para cifras mensuales de bajo volumen.
- **Redondear las cifras (p. ej. a múltiplos de 5):** sigue revelando celdas de una sola empresa.

## Consecuencias

- Con pocos datos (como en desarrollo) casi todo sale "Reservado". Es el comportamiento buscado, no
  un error.
- Riesgo residual aceptado: no se publican tablas cruzadas (mes × rubro), que son las que permiten
  ataques por intersección. Si se agregan, hay que extender la supresión a esas combinaciones.
- La validación de un vehículo permite saber si una matrícula conocida está al día. Es el propósito
  del requisito; no expone a la empresa ni al chofer.
- El umbral vive en `ProteccionEstadistica` (`UMBRAL_GUIAS`, `UMBRAL_EMPRESAS`) y está cubierto por
  `ProteccionEstadisticaTest`.
