# ADR-0022 — Fiscalización automática: reglas, ciclo del caso y parametrización

**Estado:** Propuesta (frente P5; lo implementó Facundo, P4, por pedido del grupo, y queda a revisión
de Joaquín)
**Fecha:** 2026-11-02

## Contexto

La letra pide detectar automáticamente cuatro situaciones (exceso de carga, vehículo sin habilitación,
viaje sin guía y desvío de ruta) y llevar cada una por un caso con ciclo de vida *detectada,
notificada, en descargo, confirmada o desestimada*. Además pide:
- a. que la detección sea asincrónica y no degrade la recepción de eventos;
- b. que no haya transiciones no previstas;
- c. que se notifique a la empresa;
- d. que toda acción quede en la auditoría;
- e. que un mismo hecho no genere casos duplicados.

También pide parametrizar los umbrales sin redesplegar, y que cada grupo defina y justifique el criterio
de desvío. Los datos ya los producen los demás frentes:
- las pesadas nuevas, por la cola `pesadas` (P4);
- el recorrido con PostGIS (P4);
- los estados de la guía (P3);
- la auditoría de solo agregado (P1).

## Decisión

### Cuándo se evalúa (asincrónico)

| Disparo | Reglas | Por qué así |
|---|---|---|
| Cada pesada nueva (`DeteccionPesadaMDB`, cola `pesadas`) | exceso de carga, sin habilitación, viaje sin guía | La sincronización de pesajes no espera a las reglas; si fallan, la pesada ya está guardada y el mensaje se reentrega. |
| Fin de un viaje (`FinDeViajeMDB`, después de traer los pesajes) | desvío de ruta | El desvío se juzga sobre el recorrido completo. |
| Agenda cada 5 minutos (`AgendaDeteccionBean`) sobre las posiciones de los últimos 10 | viaje sin guía, sin habilitación | A 100 posiciones/s, evaluar cada una cargaría la ingesta, y el hecho (un vehículo en un día) es el mismo para todas. |

### Criterios

- **Exceso de carga:** peso pesado > peso del vehículo + capacidad declarada + tolerancia
  (`exceso_carga.tolerancia_kg`, 500 kg). La tolerancia absorbe el error de la balanza.
- **Sin habilitación:** el día de la actividad (pesada o tracking, en hora de Uruguay) se da alguna de
  estas situaciones:
  - el permiso nacional no estaba vigente o falta;
  - la ITV estaba vencida o falta;
  - el vehículo o su empresa estaban dados de baja.
- **Viaje sin guía:** hay actividad del vehículo sin un viaje real que la cubra. Viaje real es una guía
  EN_CURSO o FINALIZADA, es decir, con el INICIO registrado por el chofer. Una guía solo ASIGNADA no
  alcanza: el chofer no empezó el viaje.
- **Desvío de ruta.** No hay un servicio de ruteo, así que el recorrido esperado es un **corredor**
  alrededor de la línea origen–destino de la guía:
  - **Ancho del corredor:** el mayor entre 5 km (`desvio_ruta.tolerancia_m`) y el 15 % de la distancia
    en línea recta (`desvio_ruta.tolerancia_relativa_pct`). Las rutas nacionales se apartan de la
    recta en proporción al largo del viaje: un ancho fijo marcaría como desvío cualquier viaje largo, y
    uno solo relativo sería demasiado estricto en viajes cortos.
  - **Cuándo hay desvío:** si más del 20 % de las posiciones del viaje quedan fuera del corredor
    (`desvio_ruta.porcentaje_puntos`). Se usa un porcentaje y no un solo punto, para que un error de
    GPS no se tome por un desvío.
  - Con menos de 5 posiciones no se concluye nada.
  - Las distancias las calcula PostGIS sobre `geography`, en metros sobre la Tierra.

### Ciclo del caso

- `EstadoCaso` define las únicas transiciones posibles; `CasoFiscalizacion.pasarA` rechaza cualquier
  otra. Están probadas todas las combinaciones (`EstadoCasoTest`).
- Al crearse, el caso se **notifica en el acto**: pasa de DETECTADA a NOTIFICADA, con el plazo de
  descargo `descargo.plazo_dias` (10 días corridos). La empresa lo ve en su sección "Infracciones" y
  queda un registro en `notificacion_empresa` (bandeja del frontoffice, más un correo simulado en el log).
- La empresa presenta **un** descargo dentro del plazo y el caso pasa a EN_DESCARGO.
- **Resolución del funcionario**, siempre con fundamento obligatorio:
  - **Confirmar** se puede desde EN_DESCARGO, o desde NOTIFICADA una vez vencido el plazo sin
    descargo. No se confirma sin haberle dado a la empresa la oportunidad de responder.
  - **Desestimar** se puede siempre que el caso no esté resuelto.
- Al vencer el plazo sin descargo, el caso queda en NOTIFICADA marcado como "plazo vencido", listo
  para resolver. No se confirma solo: la resolución es siempre de una persona.

### Duplicados, auditoría y autorización

- **Sin duplicados:** cada caso lleva un `hecho` único (`EXCESO_CARGA:pesada:<id>`,
  `SIN_HABILITACION:<matrícula>:<día>`, `VIAJE_SIN_GUIA:<matrícula>:<día>`, `DESVIO_RUTA:guia:<id>`) y la
  base lo inserta con `ON CONFLICT DO NOTHING`. Reprocesar cualquier evento no crea casos nuevos.
- **Auditoría:** se registran la creación y la notificación (usuario `sistema`), el descargo (la cédula
  del responsable) y la resolución (el funcionario), con su fundamento. La tabla es de solo agregado:
  la base rechaza UPDATE y DELETE.
- **Autorización en los servicios:**
  - bandeja y resolución: FUNCIONARIO y ADMINISTRADOR;
  - infracciones y descargo: el responsable activo de la empresa, comprobado en cada pedido (un caso de
    otra empresa responde 403);
  - parámetros: ADMINISTRADOR;
  - indicadores: AUTORIDAD y ADMINISTRADOR.
- **Parámetros sin redesplegar:** las reglas leen `parametro_regla` en cada evaluación. Cada cambio
  exige un motivo y queda auditado (`PARAMETRO_REGLA_MODIFICADO`).

## Alternativas consideradas

- **Evaluar en la ingesta:** detección inmediata, pero la letra pide no degradar la recepción, y la
  prueba de performance mostró que la CPU del central es el recurso escaso.
- **Desvío contra una ruta calculada** (OSRM u otro servicio de ruteo): más preciso, pero suma un
  servicio externo y otra dependencia de disponibilidad. Queda como mejora; el corredor es explicable y
  se ajusta sin redesplegar.
- **Confirmar automáticamente al vencer el plazo:** menos trabajo para el funcionario, pero confirmar
  una infracción es un acto con consecuencias, y la letra pide resolución fundada.
- **Un caso por pesada o por posición también para "viaje sin guía":** generaría cientos de casos por
  el mismo hecho. Un caso por vehículo y día representa el incumplimiento real.

## Consecuencias

- Los vehículos que no están registrados no generan casos, porque no se pueden atribuir a una empresa
  (por ejemplo, las matrículas `GEN` del generador de carga). Quedan contados en la métrica
  `carga_fiscalizacion_casos_total`.
- La agenda se apaga con `FISCALIZACION_AGENDA_ACTIVA=false`, por ejemplo durante la prueba de
  performance.
- La regla de desvío necesita coordenadas de origen y destino en la guía; sin ellas no se evalúa (queda
  en el log).
