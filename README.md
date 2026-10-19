# carga.uy

Sistema de gestión del transporte de carga terrestre — Taller de Sistemas Empresariales / Taller de
Sistemas de Información (FIng, UdelaR), **Grupo 5**, 2026.

Este repositorio es la **base común** del equipo: la estructura de módulos, la base de datos, el
entorno de ejecución en Docker y un ejemplo completo de punta a punta (JSF + REST + SOAP + EJB +
JPA + JMS + nodo periférico). Sobre esta base cada frente de trabajo agrega sus casos de uso.

---

## 1. Requisitos

| Herramienta | Versión | Notas |
|---|---|---|
| JDK | 24 | `java -version` debe decir 24 |
| Maven | 3.9+ | o el Maven que trae IntelliJ |
| Docker Desktop | reciente | con `docker compose` (v2) |
| RAM libre | ≥ 3 GB | son dos WildFly + PostgreSQL |
| Git | cualquiera | |

No hace falta instalar WildFly ni PostgreSQL: los levanta Docker.

---

## 2. Arrancar todo (3 comandos)

```bash
git clone https://github.com/Facu185/carga.uy.git
cd carga.uy
cp .env.example .env
docker compose up -d --build
```

La primera vez tarda varios minutos (descarga Maven, WildFly y las dependencias). Después arranca
en ~30 segundos.

Cuando termina:

| Qué | URL |
|---|---|
| Frontend React (frontoffice y backoffice) | http://localhost:3000/ |
| Backoffice — ingreso | http://localhost:3000/backoffice/login (usuario `admin` / `admin-solo-desarrollo`) |
| Jaeger (trazas distribuidas) | http://localhost:16686 |
| Simulador gub.uy | http://localhost:8085/gubuy/.well-known/openid-configuration |
| Aplicación (JSF) — `http://localhost:8080/` redirige acá | http://localhost:8080/carga/ |
| Panel de operación de nodos (P4) | http://localhost:8080/carga/panel.xhtml |
| REST — salud | http://localhost:8080/carga/api/salud |
| REST — empresas | http://localhost:8080/carga/api/empresas |
| REST — consulta a la PDI | http://localhost:8080/carga/api/pdi/personas/22222222 |
| REST — estado de los nodos | http://localhost:8080/carga/api/monitor/nodos |
| SOAP — WSDL | http://localhost:8080/carga/EmpresaService?wsdl |
| Simulador PDI (SOAP) | http://localhost:8080/simulador-pdi/ServicioDnicImpl?wsdl |
| Nodo Balanzas (REST) | http://localhost:8081/balanzas/api/pesadas?matricula=STZ1234 |
| Consola WildFly | http://localhost:9990 (usuario `carga` / `carga123`) |
| PostgreSQL | `localhost:5432`, base/usuario/clave `carga` |

Comandos útiles:

```bash
docker compose logs -f central
```

```bash
docker compose down
```

```bash
docker compose down -v
```

(`down -v` borra además el volumen de la base: útil cuando querés empezar de cero.)

---

## 3. Compilar sin Docker

```bash
mvn clean package
```

Genera `carga-ear/target/carga.ear`. Docker hace esto mismo adentro del contenedor, así que **no
hace falta compilar antes de `docker compose up --build`**.

---

## 4. Estructura

```
carga.uy
├── carga-api          Contratos: interfaces de negocio y DTO. No depende de nadie.
├── carga-ejb          Dominio (JPA), servicios (EJB), MDB, migraciones de la base.
├── carga-web          REST + SOAP + JSF/PrimeFaces del componente central.
├── carga-ear          Empaquetado desplegable (carga.ear).
├── carga-front        Frontend React (fuera del EAR, servido por nginx; ADR 0004).
├── simulador-pdi      Simulador SOAP de la PDI (servicio de DNIC).
├── nodo-balanzas      Nodo periférico: pesadas de los peajes (REST, request-response).
├── nodo-tracking      Nodo periférico: emite posiciones GPS (proceso autónomo).
├── carga-generador    Generador de carga de posiciones para la prueba de performance (P4).
├── docker/            Dockerfile de cada contenedor + configuración de WildFly.
└── docs/              Contratos entre frentes, división del trabajo, decisiones.
```

Regla de dependencias: **`carga-web` y los nodos nunca dependen de `carga-ejb`**, solo de
`carga-api`. Así se puede cambiar una implementación sin tocar a quien la consume.

---

## 5. Qué ya está resuelto (y sirve de ejemplo)

Cada cosa de la lista tiene una implementación funcionando que se puede copiar:

| Tema | Dónde mirar |
|---|---|
| EJB Stateless + reglas de negocio | `carga-ejb/.../servicio/EmpresaServicioBean.java` |
| Entidad JPA + mapeo a DTO | `carga-ejb/.../modelo/Empresa.java` |
| Recurso REST | `carga-web/.../rest/EmpresaRecurso.java` |
| Servicio SOAP | `carga-web/.../soap/EmpresaSoap.java` |
| Pantalla JSF + PrimeFaces | `carga-web/src/main/webapp/empresas.xhtml` |
| Pantalla con auto-refresco y mapa | `carga-web/src/main/webapp/panel.xhtml` |
| Cola JMS + MDB (procesamiento asincrónico) | `carga-ejb/.../jms/PosicionMDB.java` |
| Cliente REST a un nodo periférico (con timeout) | `carga-ejb/.../servicio/PesajeBean.java` |
| Cliente SOAP a la PDI | `carga-ejb/.../servicio/PdiBean.java` (probalo con `/carga/api/pdi/personas/11111111`) |
| Migraciones de la base | `carga-ejb/src/main/resources/db/migration/` |
| Prueba unitaria | `carga-ejb/src/test/java/.../VehiculoTest.java` |

---

## 6. La base de datos

El esquema lo crean las **migraciones de Flyway**, no Hibernate (`hbm2ddl.auto=none`). Al arrancar,
`Migraciones.java` aplica todo lo que haya en `carga-ejb/src/main/resources/db/migration/`.

Para agregar una tabla o una columna:

1. Creá un archivo nuevo: `V3__descripcion_corta.sql` (el número siguiente al último).
2. **Nunca edites una migración que ya se aplicó** — Flyway guarda un hash y va a fallar.
3. Reiniciá: `docker compose restart central`.

Si te quedó la base en un estado raro: `docker compose down -v && docker compose up -d`.

---

## 7. Probar rápido que anda

```bash
curl http://localhost:8080/carga/api/salud
```

```bash
curl -X POST http://localhost:8080/carga/api/empresas -H "Content-Type: application/json" -d "{\"nombre\":\"Prueba SRL\",\"razonSocial\":\"Prueba SRL\",\"numeroEmpresa\":9001}"
```

```bash
docker compose logs -f tracking
```

El nodo Tracking postea posiciones cada 5 segundos; se ven llegar en los logs de `central` y
quedan en la tabla `posicion_tracking`.

Ingreso al backoffice y una llamada autenticada (el token va en `Authorization: Bearer`):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/carga/api/auth/login -H "Content-Type: application/json" -d '{"usuario":"admin","password":"admin-solo-desarrollo"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')
curl -s http://localhost:8080/carga/api/auth/yo -H "Authorization: Bearer $TOKEN"
```

Ingreso con gub.uy: en http://localhost:3000/ingresar, con una cédula del padrón del simulador PDI
(`1.111.111-1` es válida) para que además se complete la fecha de nacimiento desde DNIC.

En **Windows PowerShell** usá `curl.exe` (no `curl`, que ahí es un alias de `Invoke-WebRequest`).

---

### Pruebas de integración

Además de `mvn verify` (pruebas unitarias, sin Docker) hay dos niveles más. Los dos corren en el
job `integracion` del CI:

```bash
# Migraciones contra un PostGIS real (Testcontainers; necesita Docker)
mvn verify -Pintegracion -pl carga-ejb -am

# El sistema completo: login, roles, gub.uy, PDI, auditoría, métricas
docker compose up -d --build
./pruebas/punta-a-punta.sh
```
### Generador de carga

Simula miles de dispositivos GPS contra la ingesta de posiciones, para la prueba de performance
(volumen y escenarios en el [ADR-0008](docs/decisiones/0008-volumen-y-saturacion-de-tracking.md)).
No arranca con `docker compose up`: está en el perfil `carga`.

Antes de correrlo, bajá el nodo Tracking de demostración (si no, sus posiciones se mezclan con las del
generador y la verificación "sin pérdida" no cierra):

```bash
docker compose stop tracking
```

Con los valores por defecto (escenario `constante`: 3.000 vehículos cada 30 s = 100 posiciones/s,
durante 10 minutos):

```bash
docker compose --profile carga run --build --rm generador
```

Otros escenarios, cambiando parámetros con `-e`:

```bash
docker compose --profile carga run --build --rm -e GEN_ESCENARIO=pico generador
docker compose --profile carga run --build --rm -e GEN_ESCENARIO=rampa -e GEN_DURACION_SEG=1200 generador
```

| Variable | Default | Qué es |
|---|---|---|
| `GEN_ESCENARIO` | `constante` | `constante`, `pico` (a un tercio de la prueba una parte de la flota pierde cobertura y la recupera casi junta) o `rampa` (suma vehículos hasta el final, para buscar el punto de quiebre) |
| `GEN_VEHICULOS`, `GEN_INTERVALO_SEG`, `GEN_DURACION_SEG` | `3000`, `30`, `600` | Flota inicial, cada cuánto reporta cada vehículo y cuánto dura la generación |
| `GEN_PROB_DUPLICADO` | `0.02` | Probabilidad de reenviar una posición ya aceptada (prueba la idempotencia) |
| `GEN_PROB_SIN_COBERTURA`, `GEN_SIN_COBERTURA_MAX_SEG` | `0.01`, `600` | Probabilidad, en cada reporte, de entrar a una zona sin señal, y su duración máxima |
| `GEN_PICO_FRACCION`, `GEN_PICO_CORTE_SEG`, `GEN_PICO_VENTANA_SEG` | `0.5`, `300`, `60` | Escenario pico: qué parte de la flota pierde cobertura, por cuánto, y en cuántos segundos la recuperan |
| `GEN_RAMPA_PASO`, `GEN_RAMPA_CADA_SEG` | `500`, `120` | Escenario rampa: cuántos vehículos se suman y cada cuánto |
| `GEN_CONCURRENCIA`, `GEN_LOTE_MAX` | `200`, `500` | Pedidos en vuelo como máximo y tamaño máximo de un lote |
| `GEN_REPORTE_SEG`, `GEN_DRENAJE_SEG` | `10`, `180` | Cada cuánto informa y cuánto espera al final a que se vacíen los buffers |

Cada 10 segundos muestra pedidos y posiciones por segundo, respuestas (202, 400, 503, errores), la
latencia de la ingesta (p50, p95, p99) y lo que quedó en los buffers. Al final compara las posiciones
únicas aceptadas con lo que el central guardó como nuevo e informa **SIN PERDIDA** o cuántas faltan.
El detalle queda en `resultados-carga/` (CSV, no se versiona).

Después de la prueba, volvé a levantar el nodo y, si querés, borrá las posiciones generadas
(sus matrículas empiezan con `GEN`, así que tampoco aparecen en los recorridos de vehículos reales):

```bash
docker compose start tracking
docker compose exec db psql -U carga -d carga -c "DELETE FROM posicion_tracking WHERE matricula LIKE 'GEN%';"
```

### Monitoreo y modo performance (P4)

Prometheus guarda las métricas del central cada 5 segundos y Grafana las grafica en un tablero ya
armado: ingesta, cola, tiempos por endpoint, memoria, recolector de basura, CPU, pool de conexiones y
circuito de Balanzas. Están en el perfil `monitoreo`, así que no arrancan con `docker compose up`.

```bash
docker compose --profile monitoreo up -d
```

- Grafana: http://localhost:3001 (sin login; abre directo el tablero "carga.uy - Performance (P4)").
- Prometheus: http://localhost:9090 (en *Status → Targets* el central tiene que figurar *UP*).

Para las pruebas de performance se suma `docker-compose.performance.yml`, que limita el central a
2 CPU y 2 GB (los resultados no dependen de la máquina) y activa `pg_stat_statements` en Postgres:

```bash
docker compose -f docker-compose.yml -f docker-compose.performance.yml --profile monitoreo up -d
docker compose exec db psql -U carga -d carga -c "CREATE EXTENSION IF NOT EXISTS pg_stat_statements;"
```

Las 10 consultas SQL que más tiempo consumieron:

```bash
docker compose exec db psql -U carga -d carga -c "SELECT round(total_exec_time) AS ms_total, calls, round(mean_exec_time::numeric, 2) AS ms_promedio, left(query, 80) AS consulta FROM pg_stat_statements ORDER BY total_exec_time DESC LIMIT 10;"
```

El consumo de CPU y memoria de cada contenedor se ve con `docker stats`.

## 8. Cómo sigue cada uno

- **División del trabajo:** [docs/frentes.md](docs/frentes.md)
- **Contratos entre frentes (lo que no se toca sin avisar):** [docs/contratos.md](docs/contratos.md)
- **Cómo trabajar en el repo (ramas, commits, PR):** [CONTRIBUTING.md](CONTRIBUTING.md)
- **Decisiones técnicas y por qué:** [docs/decisiones/](docs/decisiones/)

---

## 9. Problemas frecuentes

| Síntoma | Causa | Solución |
|---|---|---|
| `failed to connect to the docker API` | Docker Desktop no está corriendo | Abrilo y esperá a que diga "Engine running" |
| El central arranca y se cae con `OutOfMemoryError: Metaspace` | poca RAM asignada a Docker | Subí la memoria de Docker Desktop a ≥ 4 GB |
| `port is already allocated` (8080) | tenés otro WildFly/Tomcat levantado | Bajalo, o cambiá el puerto en `docker-compose.yml` |
| La app no ve cambios del código | la imagen está cacheada | `docker compose up -d --build` |
| `Migration checksum mismatch` | editaste una migración ya aplicada | Revertí el archivo, o `docker compose down -v` |
| El WSDL muestra una IP `172.x` | falta `wsdl-host` | Ya está configurado en `docker/central/configure.cli`; reconstruí la imagen |
