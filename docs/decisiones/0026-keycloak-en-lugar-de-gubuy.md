# ADR-0026 — Keycloak como proveedor OpenID Connect en lugar de gub.uy

**Estado:** Aceptada (pedido del docente)
**Fecha:** 2026-11-18

## Contexto

El ingreso de ciudadanos y choferes se hace con gub.uy / ID Uruguay por OpenID Connect (Authorization
Code + PKCE, ADR-0017). El ambiente de testing de ID Uruguay rechaza el cliente del curso (`890192`)
con `OIDC_ERROR` desde antes de la entrega, sin cambios de nuestro lado. Hasta ahora, en desarrollo
se usaba un simulador (`docker/gubuy-simulador`, imagen `oidc-provider`) que aceptaba cualquier cédula
sin clave y no se podía administrar.

El docente pidió reemplazarlo, mientras gub.uy no funcione, por **Keycloak en un contenedor Docker con
usuarios configurados**, e ingresar contra él.

## Decisión

- El servicio `keycloak` del Compose (`quay.io/keycloak/keycloak:26.0.7`, `start-dev --import-realm`)
  reemplaza al simulador. Al arrancar importa `docker/keycloak/gubuy-realm.json`:
  - realm `gubuy`, cliente confidencial `carga-uy-local` con PKCE S256 obligatorio;
  - scopes con los mismos nombres y claims que ID Uruguay: `personal_info` (`nombre_completo`,
    `primer_nombre`, `primer_apellido`), `email` y `document` (`numero_documento`, `tipo_documento`);
  - usuarios de prueba con la cédula como usuario y clave `carga-solo-desarrollo`: los 5 choferes de
    `pruebas/datos-demo.sql`, una funcionaria, una ciudadana y las 3 personas del padrón del simulador
    de la PDI (11111111, 22222222, 33333333).
  El secreto del cliente y la redirect URI se toman del entorno (`GUBUY_CLIENT_SECRET`,
  `GUBUY_REDIRECT_URI`), los mismos que lee el central, así que no pueden quedar desalineados.
- **El central no cambia de protocolo:** sigue siendo el mismo cliente OIDC (valida firma, `iss`,
  `aud` y `nonce` del `id_token`, y pide UserInfo). Volver a ID Uruguay es solo configuración
  (`docker-compose.gubuy-testing.yml`).
- **Dos direcciones para el mismo emisor.** El navegador llega a Keycloak por nginx
  (`/gubuy/`, misma URL que el front: en la nube no hace falta otro puerto ni otro certificado), y el
  central le habla por la red interna (`http://keycloak:8080`). Keycloak anuncia la URL pública como
  emisor (`KC_HOSTNAME` = `KEYCLOAK_URL_PUBLICA`) y, con `KC_HOSTNAME_BACKCHANNEL_DYNAMIC`, sirve
  token, UserInfo y claves en la dirección interna. El central lee el discovery por la red interna
  (variable nueva `GUBUY_DISCOVERY_URL`) y sigue exigiendo el emisor público en el `id_token`.
- nginx no publica la consola de administración ni el realm `master` (404). La consola se usa en
  desarrollo por `http://localhost:8085/gubuy/admin/`; en la nube el puerto queda cerrado y los
  usuarios se agregan con `kcadm.sh` dentro del contenedor (docs/despliegue-elastic-cloud.md).
- nginx resuelve los nombres de los contenedores en cada pedido (DNS de Docker), así que recrear
  Keycloak o el central no lo deja apuntando a una IP vieja.

## Alternativas consideradas

- **Seguir con el simulador `oidc-provider`.** No tiene usuarios ni claves: no cumple el pedido y no
  muestra un ingreso real.
- **Keycloak publicado en otro puerto (8085) para el navegador.** En la nube obliga a abrir otro puerto
  con su propio TLS; por nginx alcanza con el dominio y el Built-in SSL que ya existen.
- **Usar solo la dirección interna como emisor.** El navegador no puede resolver `keycloak:8080`.
- **Que el central también hable por la URL pública.** En la nube saldría a Internet y volvería por el
  balanceador para algo que está en la misma red, y en desarrollo `localhost` no es el host desde el
  contenedor.

## Consecuencias

- El ingreso con gub.uy vuelve a andar en desarrollo, en el CI y en Elastic Cloud, con usuarios y
  claves reales del lado del proveedor. Lo cubren `pruebas/punta-a-punta.sh` y las pruebas de
  `ClienteOidcGubUyIntegracionTest` (discovery interno con emisor público).
- Keycloak en modo `start-dev` guarda sus datos dentro del contenedor (H2, sin volumen): el realm se
  importa al crear el contenedor y, al reiniciar, se saltea porque ya existe. Las altas hechas a mano
  sobreviven a reinicios y se pierden al recrearlo; las personas fijas van en `gubuy-realm.json`, y un
  cambio en ese archivo se aplica con `up -d --force-recreate keycloak`. Un cambio de
  `GUBUY_CLIENT_SECRET` o `GUBUY_REDIRECT_URI` recrea el contenedor solo.
- Suma unos 500 MB de memoria y de 30 s a 3 minutos de arranque (el primero, que crea su base, es el más lento).
- Las claves de desarrollo (`admin-solo-desarrollo`, `carga-solo-desarrollo`, `secreto-solo-desarrollo`)
  son públicas: en la nube, `GUBUY_CLIENT_SECRET` y `KEYCLOAK_ADMIN_PASSWORD` van en el `.env`.
- Cuando ID Uruguay habilite el cliente, se vuelve con `docker-compose.gubuy-testing.yml`; esta
  decisión no lo impide.
