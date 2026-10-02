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
| **Inicio** (la raíz redirige sola) | **http://localhost:8080/** |
| **Panel de operación de nodos** | **http://localhost:8080/carga/panel.xhtml** |
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
├── simulador-pdi      Simulador SOAP de la PDI (servicio de DNIC).
├── nodo-balanzas      Nodo periférico: pesadas de los peajes (REST, request-response).
├── nodo-tracking      Nodo periférico: emite posiciones GPS (proceso autónomo).
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

En **Windows PowerShell** usá `curl.exe` (no `curl`, que ahí es un alias de `Invoke-WebRequest`).

---

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
