# Pendientes funcionales del laboratorio 2026

**Fecha de corte:** 25 de noviembre de 2026 (reemplaza el inventario del 22 de octubre).

**Base de código:** `c159bcf` (rama `main`), más el repositorio móvil `JavaEE_Mobile` (`42d29d4`).

**Alcance:** requisitos funcionales de `Laboratorio_2026.pdf` (aportado por el equipo fuera del
repositorio), con su estado actual en el código. Las páginas citadas son las del PDF.

## 1. Cómo leer este documento

| Estado | Significado |
| --- | --- |
| **Hecho** | El flujo está implementado y se ejecutó de punta a punta (ver sección 2). |
| **Implementado** | El código está completo y tiene pruebas JUnit, pero ningún script lo recorre entero. |
| **Parcial** | Falta una parte concreta, que se indica. |
| **Pendiente** | No está implementado. |

Complementan este inventario:
- [Distribución de frentes](frentes.md);
- [contratos](contratos.md);
- [casos de uso críticos](GuiaCUCrit.md);
- [decisiones](decisiones/README.md);
- el [detalle de P2](pendientesP2.md).

## 2. Qué se ejecutó

Con el Compose completo levantado en local se corrieron dos scripts, sin errores:

- **[`pruebas/punta-a-punta.sh`](../pruebas/punta-a-punta.sh)** (también lo corre el job `punta-a-punta`
  del CI). Recorre:
  - salud del central y de la base;
  - ingreso de cuentas internas y rechazo sin token o con un token inventado;
  - roles de administrador y autoridad;
  - ingesta de Tracking, monitor y administración de nodos, y recorridos PostGIS;
  - ingreso con gub.uy (Keycloak, ADR-0026) de punta a punta y verificación PDI;
  - métricas y trazado distribuido en Jaeger.
- **[`pruebas/demo-fiscalizacion.py`](../pruebas/demo-fiscalizacion.py)**, con
  [`datos-demo.sql`](../pruebas/datos-demo.sql) cargado. Recorre:
  - el chofer ingresa con gub.uy y registra INICIO y FIN como la app;
  - el aviso de fin de viaje, la obtención de pesajes del nodo Balanzas y la detección asincrónica
    del desvío de ruta;
  - la sincronización de pesajes con detección de exceso de carga;
  - una sola traza de la app al central, a la cola y al nodo Balanzas.

El resto (pantallas del frontoffice, backoffice y área del funcionario) se probó a mano en el
navegador durante el desarrollo, sin un script que lo repita.

En Elastic Cloud (https://env-8268946.web.elasticloud.uy) el sistema está desplegado con Keycloak y
su pantalla de ingreso carga. Falta confirmar un ingreso completo con uno de sus usuarios.

## 3. Resumen

| ID | Requisito | Frente | Estado |
| --- | --- | --- | --- |
| F01 | Detección automática (4 reglas) | P5 | **Hecho**: desvío y exceso de carga ejecutados; sin habilitación y viaje sin guía implementados |
| F02 | Notificación a la empresa | P5 | **Implementado** |
| F03 | Infracciones y descargo de la empresa | P5 | **Implementado** |
| F04 | Bandeja y resolución del funcionario | P5 | **Implementado** |
| F05 | Parámetros de reglas sin redesplegar | P5 | **Implementado** |
| F06 | Tablero y reportes para la autoridad | P5 / P2 | **Parcial**: faltan volumen por rubro, habilitados y filtro geográfico |
| F07 | Recepción de eventos del viaje | P3 | **Hecho** |
| F08 | Estado e historial del viaje | P3 | **Parcial**: el funcionario no ve el historial de eventos |
| F09 | Fin de viaje → pesajes | P3 / P4 | **Hecho** |
| F10 | Perfil de la empresa | P2 | **Implementado** |
| F11 | ABM de vehículos del responsable | P2 | **Implementado** |
| F12 | Gestión de empresas por el funcionario | P2 | **Implementado** |
| F13 | Vehículos y vigencias para el funcionario | P2 | **Implementado** |
| F14 | Monitoreo de viajes y pesajes del funcionario | P2 / P3 / P4 | **Parcial**: falta el historial de eventos (ver F08) |
| F15 | Validación pública de permisos | sin asignar (hecho por P4) | **Implementado** |
| F16 | Estadísticas públicas agregadas | sin asignar (hecho por P4) | **Implementado** |
| — | Nodos periféricos en otra plataforma | P4 | **Pendiente** (ver sección 7) |

## 4. Fiscalización y reportes (P5)

El motor y el ciclo del caso los implementó Facundo (P4) a pedido del grupo; quedan a revisión de
Joaquín. Criterios y justificación: [ADR-0022](decisiones/0022-fiscalizacion-automatica-y-casos.md).

### F01. Detección automática — hecho (pp. 6–7)

- [x] **Exceso de carga:** peso pesado mayor que la tara más la capacidad más la tolerancia
  (`exceso_carga.tolerancia_kg`). Se evalúa en cada pesada nueva (`DeteccionPesadaMDB`, cola
  `pesadas`). Ejecutado por la demo.
- [x] **Sin habilitación:** permiso nacional o ITV vencidos o faltantes, o vehículo o empresa dados de
  baja, el día de la actividad. Lo evalúan la pesada y la agenda de 5 minutos
  (`AgendaDeteccionBean`).
- [x] **Viaje sin guía:** actividad sin una guía EN_CURSO o FINALIZADA que la cubra.
- [x] **Desvío de ruta**, con criterio definido y justificado:
  - corredor alrededor de la línea origen–destino, de ancho máximo entre 5 km y el 15 % de la
    distancia;
  - hay desvío si más del 20 % de las posiciones quedan afuera;
  - se evalúa al fin del viaje (`FinDeViajeMDB`).
  
  Ejecutado por la demo.
- [x] **Asincrónico:** nada se evalúa dentro de la recepción de eventos ni de posiciones.
- [x] **Sin duplicados:** cada caso lleva un `hecho` único con `ON CONFLICT DO NOTHING`.

### F02. Notificación a la empresa — implementado (p. 7)

- [x] Al crearse, el caso pasa a NOTIFICADA con plazo de descargo (`descargo.plazo_dias`, 10 días).
- [x] Queda registrado en `notificacion_empresa` y la empresa lo ve en *Infracciones*.
- El correo es simulado: se escribe en el log y no se envía. Si la letra exige un envío real, falta
  configurar SMTP.

### F03. Infracciones y descargo de la empresa — implementado (p. 5)

- [x] Pantalla [Infracciones](../carga-front/src/pages/frontoffice/Infracciones.tsx): casos de la
  empresa con su evidencia.
- [x] Un descargo por caso, dentro del plazo. Pasa a EN_DESCARGO.
- [x] El servidor controla la pertenencia a la empresa (un caso ajeno responde 403) y el plazo.

### F04. Bandeja y resolución del funcionario — implementado (pp. 6–7)

- [x] [Casos](../carga-front/src/pages/funcionario/Casos.tsx) y
  [DetalleCaso](../carga-front/src/pages/funcionario/DetalleCaso.tsx): bandeja, evidencia y descargo.
- [x] Confirmar o desestimar, con fundamento obligatorio.
- [x] Solo las transiciones de `EstadoCaso`: `CasoFiscalizacion.pasarA` rechaza las demás.
  `EstadoCasoTest` cubre todas las combinaciones.
- [x] **Plazo vencido sin descargo:** el caso queda en NOTIFICADA marcado como vencido, y lo resuelve
  una persona. No se confirma solo.
- [x] Auditoría de creación, notificación, descargo y resolución, en la tabla de solo agregado de P1.

### F05. Parámetros sin redesplegar — implementado (p. 8)

- [x] Pantalla [Reglas de fiscalización](../carga-front/src/pages/backoffice/ReglasFiscalizacion.tsx),
  solo para el administrador.
- [x] Las tolerancias de exceso de carga y de desvío, más el plazo de descargo.
- [x] Las reglas leen `parametro_regla` en cada evaluación. Cada cambio exige un motivo y queda
  auditado.

### F06. Tablero y reportes para la autoridad — parcial (pp. 7–8)

Lo que hay:
- [ReporteActividad](../carga-front/src/pages/backoffice/ReporteActividad.tsx):
  - guías por día, estado y rubro;
  - dos criterios de fecha, por creación de la guía o por viaje finalizado (evento FIN);
  - filtros de período y tipo de carga.
  
  Fuentes: [ADR-0020](decisiones/0020-tablero-autoridad-actividad.md) y
  [ADR-0023](decisiones/0023-reporte-viajes-finalizados-autoridad.md).
- [IndicadoresFiscalizacion](../carga-front/src/pages/backoffice/IndicadoresFiscalizacion.tsx):
  infracciones por tipo y estado, y la tasa de confirmadas sobre detectadas.
- Exportación en CSV y en PDF (impresión del navegador).

Lo que falta:
- [ ] **Volumen de carga por rubro:** hoy se cuentan guías, no kilos.
- [ ] **Empresas y vehículos habilitados en el tablero de la autoridad.** Las estadísticas públicas
  (F16) ya los calculan, pero con supresión.
- [ ] **Filtro por ubicación geográfica.**

## 5. Eventos del viaje (P3) y área del funcionario (P2)

### F07. Recepción de eventos — hecho (pp. 5–6, CU3)

- [x] `POST /eventos-viaje`: inicio, fin, carga y descarga parcial, e incidente con foto.
- [x] Solo el chofer de la guía puede registrar eventos. Se deduplican por identificador global y
  contenido.
- [x] Política de conflictos con la sincronización diferida:
  [ADR-0019](decisiones/0019-registro-de-eventos-de-viaje.md).
- [x] La app móvil guarda los eventos en una cola sin conexión y los reenvía (`42d29d4`, CU3).
- [ ] **Falta probar con la app sin conexión real** y forzar un conflicto. La matriz está en
  [`p2-p3-eventos-conflictos.md`](../pruebas/p2-p3-eventos-conflictos.md).

### F08. Estado e historial — parcial (p. 6)

- [x] Los eventos válidos mueven la guía entre ASIGNADA, EN_CURSO y FINALIZADA.
- [x] El chofer consulta el historial de su guía (`GET /eventos-viaje?guiaId=`).
- [ ] **C14-E:** el historial de eventos para el funcionario. Hoy `EventoViajeBean` es solo CHOFER,
  y la pantalla del funcionario lo aclara.

### F09. Fin de viaje → pesajes — hecho

- [x] El FIN llama a `AvisoFinDeViajeLocal`. `FinDeViajeMDB` trae los pesajes del nodo Balanzas y
  evalúa el desvío ([ADR-0010](decisiones/0010-integraciones-de-p4-con-p3-y-p5.md)).
- [x] La demo lo recorre entero, con una sola traza en Jaeger.

### F10–F14. Empresa y funcionario (P2)

| ID | Qué hay | Qué falta |
| --- | --- | --- |
| F10 | `/mi-empresa/perfil`, editable por el responsable activo, sin campos administrativos ([ADR-0014](decisiones/0014-perfil-propio-empresa.md)). | Nada funcional. |
| F11 | `/mi-empresa/vehiculos`: alta, modificación, baja protegida si hay un viaje EN_CURSO, y reactivación ([ADR-0012](decisiones/0012-abm-vehiculos-del-responsable.md)). | Probar la carrera entre el inicio de un viaje y la baja del vehículo. |
| F12 | `/funcionario/empresas`: búsqueda, detalle, edición y baja. | Nada funcional. |
| F13 | Vehículos de una empresa con la vigencia de permiso e ITV ([ADR-0016](decisiones/0016-consulta-vehicular-funcionario.md)). | Nada funcional. |
| F14 | Viajes, detalle y pesajes del viaje ([ADR-0018](decisiones/0018-consulta-viajes-funcionario-p2.md)). | El historial de eventos (F08, C14-E). |

Además, por pedido del grupo:
- vista previa del recorrido al crear una guía, con IDE y ORS
  ([ADR-0024](decisiones/0024-vista-previa-guias-ide-ors.md)); necesita `ORS_API_KEY` en el `.env`;
- exclusión entre chofer y responsable
  ([ADR-0025](decisiones/0025-incompatibilidad-chofer-responsable.md)).

## 6. Funciones públicas — implementado (pp. 2, 5, 10–11)

No tenían responsable asignado. Las implementó Facundo (P4) según
[ADR-0021](decisiones/0021-frontoffice-publico-y-umbral-de-agregacion.md).

- [x] **F15, [ValidarPermiso](../carga-front/src/pages/publico/ValidarPermiso.tsx):** se consulta por
  matrícula o número de empresa, sin ingresar.
  - Responde si el vehículo puede circular hoy.
  - No dice de qué empresa es ni quién lo maneja.
- [x] **F16, [EstadisticasPublicas](../carga-front/src/pages/publico/EstadisticasPublicas.tsx):**
  - Publica empresas y vehículos habilitados, y viajes y toneladas por mes, tipo de carga y rubro.
  - Umbral: una cifra sale solo si reúne al menos 5 guías de al menos 3 empresas.
  - Aplica supresión secundaria (`ProteccionEstadistica`, con su test).

## 7. Pendientes reales

| Pendiente | Frente | Qué hay que hacer |
| --- | --- | --- |
| Nodos periféricos en otra plataforma | P4 | La letra pide Balanzas y Tracking en otra PaaS o IaaS. Hoy corren en el mismo nodo que el central. Ver [despliegue, sección 11](despliegue-elastic-cloud.md). |
| Historial de eventos para el funcionario (C14-E) | P3 / P2 | Lectura autorizada para FUNCIONARIO y mostrarla en el detalle del viaje. |
| Tablero de la autoridad | P5 / P2 | Volumen por rubro, habilitados y filtro por ubicación geográfica. |
| App sin conexión y conflictos | P3 | Probar con el celular sin red y un conflicto con el frontoffice. |
| Trazado en la app | P3 | `JavaEE_Mobile` tiene `src/services/traza.ts` y `api.ts` sin commitear (traceparent por pedido). Commitear o descartar. |
| Revisión de lo hecho por P4 en otros frentes | P5 y grupo | ADR-0021 y ADR-0022 siguen en estado Propuesta, a revisión de Joaquín y del grupo. |
| ID Uruguay real | externo | Depende de que AGESIC habilite el cliente `890192`. Mientras tanto se usa Keycloak por pedido del docente ([ADR-0026](decisiones/0026-keycloak-en-lugar-de-gubuy.md)). |

## 8. Lo que no es un pendiente

- **Rubros y tipos de carga:** la letra (p. 8) los da precargados; no hace falta un ABM.
- **Opcionales elegidos:** LDAP (P1) y PostGIS (P4/P5). Los demás opcionales de la letra no se
  toman.
- **Que una guía pase su fecha prevista no la vuelve un viaje realizado:** el fin lo marca el evento
  FIN.
