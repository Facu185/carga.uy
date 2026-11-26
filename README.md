# carga.uy

Gestión del transporte de carga terrestre. Taller de Sistemas Empresariales, FIng (UdelaR), **Grupo 5**, 2026.

## Levantar todo

```bash
docker compose up -d --build
```

Requiere Docker (`compose` v2, 4 GB de memoria). La primera vez tarda varios minutos: compila con Maven dentro de
Docker. El `.env` es opcional (ver `.env.example`). `docker compose down -v` baja todo y borra la base.

| Qué | Dónde |
|---|---|
| Frontoffice y backoffice | http://localhost:3000 (backoffice en `/backoffice/login`) |
| API REST del central | http://localhost:8080/carga/api/salud |
| Keycloak (gub.uy simulado) / Jaeger | http://localhost:8085/gubuy/admin/ / http://localhost:16686 |
| En la nube (Elastic Cloud) | https://env-8268946.web.elasticloud.uy |

## Usuarios de desarrollo

- **Backoffice y consola de Keycloak:** `admin` / `admin-solo-desarrollo`.
- **Ingreso con gub.uy** ([ADR-0026](docs/decisiones/0026-keycloak-en-lugar-de-gubuy.md)): la cédula es el usuario y la clave
  es `carga-solo-desarrollo`. Choferes demo: `41000019`, `41000025`, `41000031`, `41000047` y `41000053`.
  En el padrón PDI: `22222222`. Ciudadano sin otro rol: `12345672`.

## Datos y pruebas

```bash
docker compose exec -T db psql -U carga -d carga < pruebas/datos-demo.sql
./pruebas/punta-a-punta.sh
python pruebas/demo-fiscalizacion.py
```

Cargan el juego de datos (5 empresas, choferes y un año de guías), recorren el sistema completo y simulan
un viaje con desvío y exceso de carga que abre casos de fiscalización. `mvn verify` corre las pruebas unitarias.

## Parámetros (`.env`)

| Variable | Para qué |
|---|---|
| `ADMIN_INICIAL_PASSWORD`, `JWT_SECRET` | Clave del admin inicial y firma de tokens. Cambiarlos en la nube. |
| `KEYCLOAK_URL_PUBLICA`, `GUBUY_*` | Ingreso con gub.uy. En la nube llevan el dominio. |
| `AUTENTICACION_INTERNA` | Backoffice contra la base (`bd`, default) o contra LDAP (`ldap`). |
| `COMPOSE_PROFILES` | `monitoreo` (Grafana en `:3001`) o `ldap`. Generador: `docker compose --profile carga run --rm generador`. |
| `ORS_API_KEY` | OpenRouteService (vista previa del recorrido de una guía). |

**Más:** [guía de desarrollo](docs/guia-desarrollo.md), [nube](docs/despliegue-elastic-cloud.md),
[performance](docs/informe-performance.md), [decisiones](docs/decisiones/README.md), [requisitos](docs/pendientes-funcionales-laboratorio-2026.md).
