# Informe de la prueba de performance

**Fecha:** 2026-11-02 · **Responsables:** Facundo (P4) y Joaquín (generador y monitoreo)
**Letra:** sección 4.3, puntos 2 a 4, y sección 4.4, punto 10. Volumen objetivo y política de
saturación en el [ADR-0008](decisiones/0008-volumen-y-saturacion-de-tracking.md).

## 1. Resumen

| Objetivo de la letra | Resultado |
|---|---|
| p95 de las consultas del frontoffice < 500 ms | **Cumple con el central en 4 vCPU: p95 = 350 ms** bajo el volumen base de tracking. Con 2 vCPU no cumple (p95 de 3,7 s): la ingesta satura la CPU y el frontoffice comparte esa CPU. |
| Sostener el volumen estimado (100 posiciones/s) sin pérdida | **Sin pérdida en todos los escenarios**, verificado posición por posición contra la base y con la DLQ vacía. Con 4 vCPU se sostiene en régimen (99,3 guardadas/s, 0 rechazos 503); con 2 vCPU entra en saturación controlada (503 y reintento). |
| Tiempos que no se degraden durante la prueba | Con 4 vCPU, estables durante los 5 minutos: la cola se mantiene acotada (máximo 3.365) y no crece. Con 2 vCPU se degradan hasta el umbral de saturación. |
| Punto de quiebre | Con 4 vCPU, en esta máquina, **~4.500 vehículos (150 posiciones/s, 1,5 veces el volumen base)**: ahí la latencia de la ingesta salta de 0,4 a 1,6 s y el caudal aceptado deja de crecer. El pico de 3 veces (300/s) se absorbe en la cola unos 5 minutos y después entra la política de saturación (503 y reintento), **sin pérdida** (sección 6). |
| Cuellos de botella con evidencia | Cuatro, identificados con Prometheus, `pg_stat_statements` y un perfil de Java Flight Recorder. Tres se corrigieron en el código; el cuarto (CPU del servidor de aplicaciones) define el dimensionamiento (sección 5). |

## 2. Entorno y método

- **Máquina:** notebook Intel Core i5-8350U (4 núcleos, 8 hilos, 1,7 GHz), 16 GB, Windows 11 con
  Docker Desktop 29 (VM de 8 CPU y 8 GB). **El generador corre en la misma máquina** y consume hasta 3
  CPU: los resultados absolutos son pesimistas respecto de un servidor dedicado. La comparación entre
  escenarios sí es válida, porque todos comparten esa condición.
- **Central:** `docker-compose.performance.yml`, limitado a 2 vCPU y 2 GB (también se probó con 4 vCPU
  con `docker update --cpus 4`). Pool de 20 conexiones a la base.
- **Carga de tracking:** `carga-generador`, escenario `constante` del ADR-0008: 3.000 vehículos que
  reportan cada 30 s (**100 posiciones/s**) durante 5 minutos, con 2 % de reenvíos (prueba la
  idempotencia) y 1 % de pérdidas de cobertura. Incluye un **funcionario simulado** que consulta el
  panel de operación (métricas y mapa de la flota) cada 5 s.
- **Frontoffice:** `pruebas/carga-frontoffice.py`, **20 usuarios** a la vez, cada uno con su sesión
  de gub.uy (simulador), con pausas de persona real (1 s de media). Consultan validación de permisos,
  cifras públicas, su perfil y su sesión.
- **Datos:** `pruebas/datos-demo.sql` (cinco empresas, 188 guías) más las posiciones generadas.
- **Observación:** Prometheus cada 5 s y Grafana (tablero "carga.uy - Performance (P4)"),
  `pg_stat_statements`, `docker stats` y un perfil de 50 s con Java Flight Recorder.
- Se descarta el primer minuto de cada escenario (calentamiento de la JVM). Entre escenarios se borran
  las posiciones generadas y se espera a que la cola quede vacía.

## 3. Escenarios y resultados

Volumen base en todos: 100 posiciones/s más 20 usuarios del frontoffice y el panel cada 5 s.

| Escenario | vCPU | Guardadas/s (prom.) | Rechazos 503 | Ingesta p50 / p95 | Frontoffice p50 / p95 | Panel: mapa p50 | Pérdida |
|---|---|---|---|---|---|---|---|
| 1. Base (código del 02/11, JVM 512 MB + Serial GC) | 2 | 32,5 | 22.029 | 5.225 / 7.208 ms | 3.900 / 5.923 ms | 30.351 ms | ninguna |
| 2. JVM ajustada (1,5 GB + G1) | 2 | 25,4 | sí (saturada todo el tiempo) | 7.434 / 10.626 ms | 207 / 7.920 ms | sin respuesta | ninguna |
| 3. Base con más CPU | 4 | 63,1 | 0 | 1.502 / 3.053 ms | 237 / 2.624 ms | 11.950 ms | ninguna |
| 4. **Código optimizado** | 2 | 47,5 | 14.903 | 3.332 / 5.138 ms | 102 / 3.745 ms | 3.405 ms | ninguna |
| 5. **Código optimizado** | 4 | **99,3** | **0** | **116 / 1.175 ms** | **52 / 350 ms** | **663 ms** | **ninguna** |

"Ingesta" es el tiempo de respuesta del `POST` de posiciones medido por el generador. Un 503 no es
una pérdida: es la política de saturación del ADR-0008 (el dispositivo conserva la posición y
reintenta), y todas se reintentaron hasta ser aceptadas.

**Verificación de "sin pérdida".** Al final de cada escenario el generador compara las posiciones
únicas aceptadas (202) con las filas nuevas en `posicion_tracking`. En los escenarios saturados la
comparación inmediata da "faltan", porque siguen en la cola; contadas cuando la cola llega a cero,
coinciden exactamente (por ejemplo, escenario 1: 23.073 aceptadas y 23.073 guardadas) y la DLQ tiene
0 mensajes. Los reenvíos intencionales (2 %) nunca generaron filas duplicadas (`ON CONFLICT`).

## 4. Evidencia de los mecanismos de observabilidad

Escenario 1 (base) frente al 5 (optimizado, 4 vCPU), promedios de Prometheus sin el primer minuto:

| Métrica | Base, 2 vCPU | Optimizado, 4 vCPU |
|---|---|---|
| CPU del proceso (1 = todas las vCPU asignadas) | 0,9 (p95 1,0) | 0,7 (p95 0,9) |
| Posiciones pendientes en la cola | 11.854 (máx. 18.205, umbral de 503) | 1.798 (máx. 3.365) |
| p95 HTTP global (todas las rutas) | 1.209 ms | 285 ms |
| Recolector de basura | 21,4 ms por segundo (2 %) | 3,5 ms por segundo |
| Heap usado | 233 de 512 MB | 700 de 1.536 MB |
| Conexiones del pool en uso (máx.) | 20 de 20, sin esperas | 20 de 20, sin esperas |

`pg_stat_statements` del escenario 1: el `INSERT` de una posición tarda **0,59 ms** de promedio
(23.520 llamadas). La base de datos no es el cuello de botella.

## 5. Cuellos de botella identificados

### 5.1 CPU del servidor de aplicaciones (dimensionamiento)

En la base la CPU está al 100 % todo el tiempo, mientras la base de datos responde en menos de 1 ms y
el GC usa el 2 %. El perfil de JFR es plano: casi la mitad de las muestras están en los hilos HTTP
de Undertow (recepción de posiciones, frontoffice y panel) y el resto se reparte en la pila de
WildFly (REST, CDI, transacciones JTA, Artemis), sin un método dominante. Es el patrón de un servidor
**limitado por CPU por pedido**, no por una consulta lenta.

Lo confirman los escenarios 2 y 3. Más memoria y un recolector concurrente (2) no mejoran nada.
Duplicar la CPU (3) duplica el caudal (de 32,5 a 63,1 guardadas/s) y elimina los 503.

Mientras la ingesta satura la CPU, el frontoffice degrada (p95 de 3,7 a 7,9 s), porque comparte
proceso y CPU con la ingesta. Sin saturación cumple con holgura.

### 5.2 Mapa de la flota: N+1 consultas (corregido)

`MonitorBean.mapaFlota` hacía una consulta por vehículo: con 3.000 camiones, **3.001 consultas por
cada apertura del panel** (9.952 llamadas en `pg_stat_statements` en 5 minutos), y además recorría
todas las matrículas de la historia. Se reemplazó por **una sola consulta** con una función de
ventana (`row_number() OVER (PARTITION BY matricula ORDER BY instante DESC)`), limitada a las últimas
6 horas y sobre el índice `(matricula, instante)`. El mapa bajó de 30 s a 3,4 s con 2 vCPU, y a
663 ms con 4 vCPU.

### 5.3 Métricas del panel: recorridos completos de la tabla (corregido)

Cada consulta del panel hacía `count(*)`, `count(distinct matricula)` y `max(recibido_en)` sobre
toda `posicion_tracking`, sin índice en `recibido_en`. Al volumen del ADR-0008 la tabla crece
8,6 millones de filas por día, así que esto empeora con el tiempo. Cambios:
- índice nuevo en `recibido_en` (migración `V11`);
- "vehículos reportando" cuenta los de los últimos 5 minutos (que es lo que significa);
- el total sale de la estadística de Postgres cuando supera las 100.000 filas.

### 5.4 Exportador de métricas: JMX caro en cada lectura (corregido)

El perfil mostró ~7 % de la CPU en `MonitorRecursos`: por cada atributo y en cada consulta de
Prometheus llamaba a `getMBeanInfo` sobre el MBean del pool de WildFly, que reconstruye su modelo de
administración entero. Ahora el nombre del atributo se resuelve una sola vez y queda en memoria.

**Efecto conjunto de 5.2 a 5.4**, con la misma CPU (escenario 1 frente al 4): el caudal guardado
pasó de 32,5 a 47,5 posiciones/s (**+46 %**), el GC de 21,4 a 8,2 ms por segundo y la mediana del
frontoffice de 3.900 a 102 ms.

### 5.5 Lo que no resultó ser un cuello de botella

- **Recolector de basura y heap:** 2 % del tiempo con Serial GC y 512 MB; pasar a G1 con 1,5 GB no
  mejoró. Igual se deja G1 con 1,5 GB en el perfil de performance, porque con 4 vCPU el heap usado
  llega a 1,1 GB.
- **Base de datos:** inserciones de 0,59 ms, sin esperas por conexión libre.

## 6. Punto de quiebre (escenario rampa)

Escenario `rampa` con el código optimizado y 4 vCPU: arranca con 3.000 vehículos y suma 1.500 por
minuto hasta 13.500 (450 posiciones/s, más que el pico de 3 veces que pide la letra), durante 8 minutos.

| Momento | Vehículos (posiciones/s generadas) | Aceptadas/s | Ingesta p50 | Frontoffice p95 | Qué pasa |
|---|---|---|---|---|---|
| 0:30 | 3.000 (100) | 100 | 117 ms | — | régimen normal |
| 1:00 | 4.500 (150) | 100 | 437 ms | 1.912 ms | empieza a no alcanzar |
| 1:30 – 5:00 | 4.500 a 10.500 (150 a 350) | 100 a 147 | 1,5 a 1,9 s | 2,3 a 2,7 s | **quiebre**: el caudal se estanca; la cola crece y los dispositivos acumulan |
| 5:30 en adelante | 10.500 a 13.500 (350 a 450) | 0 (503) | 0,5 s | 0,8 a 1,4 s | la cola llega al umbral (18.000): **saturación controlada**, todo se rechaza con 503 y se reintenta |

- **Punto de quiebre: ~4.500 vehículos, 150 posiciones/s.** A partir de ahí el central acepta entre
  100 y 147 posiciones/s sin importar cuánto llegue, y la latencia pasa de décimas a casi 2 segundos.
- **El pico de 3 veces no se sostiene, pero se absorbe sin pérdida**, como prevé el ADR-0008: la cola
  lo amortigua y, cuando se llena, el 503 frena a los productores. Los rechazos por saturación son
  baratos, y el frontoffice mejora apenas empiezan (de 2,6 a 0,8 s de p95).
- **Sin pérdida y sin errores:** 56.116 posiciones únicas aceptadas, 56.116 guardadas, 0 errores 5xx.
- Por la relación lineal medida entre CPU y caudal (de 2 a 4 vCPU el caudal se duplica), sostener el
  pico de 300 posiciones/s en régimen requeriría del orden de 3 veces la CPU actual: unas 8 a 12 vCPU,
  repartidas en varias instancias (sección 7).

## 7. Conclusiones y recomendaciones

1. **El central necesita al menos 4 vCPU** para el volumen base con el frontoffice dentro de su
   objetivo. Con 2 vCPU sostiene el volumen sin perder datos, gracias a la política de saturación,
   pero el frontoffice no cumple.
2. **Escalar horizontalmente.** El central no guarda estado de sesión en el servidor (JWT), así que
   se pueden sumar instancias detrás de un balanceador. La recomendación concreta es **separar la
   ingesta de tracking del resto** (instancias propias, o al menos un pool de hilos HTTP propio), para
   que un pico de posiciones no degrade el frontoffice, que es lo que se vio en los escenarios
   saturados.
3. **Lotes más grandes desde el nodo Tracking.** El generador mandó ~3 posiciones por pedido; cada
   pedido paga la pila completa de WildFly. El endpoint de lote acepta hasta 500, y agrupar más
   posiciones por pedido reduce la CPU por posición.
4. **Repetir la medición final con el generador en otra máquina** (o en la nube, junto con el
   despliegue en Elastic Cloud) para separar el costo del generador del sistema medido.

## 8. Cómo reproducir

```bash
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile monitoreo up -d --build
docker compose exec -T db psql -U carga -d carga < pruebas/datos-demo.sql
docker compose stop tracking
docker compose --profile carga run --rm -e GEN_DURACION_SEG=300 -e GEN_MONITOR_PASSWORD=<clave del admin> generador
python pruebas/carga-frontoffice.py --usuarios 20 --duracion 300   # en otra terminal, al mismo tiempo
```

Para 4 vCPU, `docker update --cpus 4 cargauy-central-1`. Rampa:
`-e GEN_ESCENARIO=rampa -e GEN_RAMPA_PASO=1500 -e GEN_RAMPA_CADA_SEG=60 -e GEN_DURACION_SEG=480`.
Las cifras de Prometheus se leen en Grafana (http://localhost:3001) o en http://localhost:9090.
