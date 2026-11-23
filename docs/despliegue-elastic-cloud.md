# Despliegue en Elastic Cloud (ANTEL)

Cómo quedó desplegado el componente central en Elastic Cloud, con HTTPS, y qué hubo que hacer para
que el sistema levante sobre el Docker 19.03 que ofrece la plataforma. Elastic Cloud es Jelastic:
la documentación de Jelastic sirve para todo lo que no está acá.

- **Dirección:** https://env-8268946.web.elasticloud.uy
- **Chequeo de salud:** https://env-8268946.web.elasticloud.uy/carga/api/salud
- **Fecha:** 2026-11-04

## 1. Por qué no sirve un entorno Tomcat

El primer intento fue un entorno con **Tomcat 11** y un nodo de build Maven. No funciona: carga.uy
es un **EAR de Jakarta EE** con EJB, JMS (Artemis), JTA y JSF, y necesita **WildFly en perfil
full**. Tomcat es solo un contenedor de servlets. La raíz mostraba la página de bienvenida de Tomcat
y `/carga` daba 404.

La solución fue usar Docker y reusar el mismo `docker-compose.yml` del repo.

## 2. Crear el entorno desde el repo de GitHub

1. En el dashboard: **MARKETPLACE → Docker Engine CE**.
2. Marcar **Deploy containers from compose.yml**.
3. **Compose Repo:** `https://github.com/Facu185/carga.uy` (el repo tiene que ser público).
4. **Docker Version:** la más nueva de la lista. En nuestra cuenta **solo existe la 19.03.9**.
5. **Install.**
6. En la configuración del entorno, subir el nodo a **32 cloudlets** (4 GB). Compila con Maven dentro
   de Docker y levanta dos WildFly; con menos memoria se corta.

El paquete clona el repo en `/root/application` y ejecuta el compose. **Con Docker 19.03 falla**: el
dashboard muestra *"Deployment failed: please check /var/log/run.log"*, y ese log solo dice
`Cloning into 'application'...`. Los pasos siguientes resuelven cada problema.

Todos los comandos van en la consola del nodo: pasar el mouse por el nodo → ícono **`>_` (Web
SSH)**. Si no abre, desactivar extensiones como uBlock para `app.elasticloud.uy`.

## 3. Problema 1: docker-compose 1.25 no entiende el archivo

**Síntoma**

```
docker-compose version 1.25.5
ERROR: Invalid interpolation format for "tracking" option in service "services": "${TRACKING_INTERVALO_MS:-5000}"
```

**Causa:** nuestro `docker-compose.yml` usa el formato actual (sin `version:`, con `profiles` y
`depends_on` con `condition`). El docker-compose 1.25 lo lee con el formato viejo y toma `services`
como si fuera el nombre de un servicio.

**Solución:** instalar docker-compose v2, que funciona con Docker 19.03.

```bash
curl -fsSL https://github.com/docker/compose/releases/download/v2.29.7/docker-compose-linux-x86_64 -o /usr/local/bin/docker-compose && chmod +x /usr/local/bin/docker-compose && hash -r && docker-compose version
```

Tiene que decir `Docker Compose version v2.29.7`. Para comprobar que ahora lee el archivo:

```bash
cd /root/application && docker-compose config --quiet && echo "compose OK"
```

## 4. Problema 2: Java no puede crear hilos dentro de los contenedores

**Síntoma:** el build se corta en el paso que configura WildFly:

```
failed to solve: ... executor failed running [/bin/sh -c "$JBOSS_HOME/bin/jboss-cli.sh" --file=/tmp/configure.cli ...]: runc did not terminate sucessfully
```

**Diagnóstico.** La misma imagen falla con el filtro de seguridad normal y anda sin él:

```bash
docker run --rm quay.io/wildfly/wildfly:41.0.0.Final-jdk25 java -version
docker run --rm --security-opt seccomp=unconfined quay.io/wildfly/wildfly:41.0.0.Final-jdk25 java -version
```

La primera da `pthread_create failed (EPERM)` y la segunda muestra la versión de Java. La memoria no
es el problema: `free -m` muestra casi todo libre.

**Causa:** las imágenes actuales (Java 25, PostGIS, Maven) crean hilos con la llamada del sistema
`clone3`. El perfil seccomp de Docker 19.03 la bloquea con EPERM y la biblioteca de C no prueba otra
forma. Docker lo corrigió en la 20.10.10, pero la plataforma no ofrece esa versión: el *Redeploy
container* del nodo solo tiene la 19.03.9.

**Solución:** que el daemon de Docker use un perfil seccomp que permita todo, y reiniciarlo.

```bash
echo '{"defaultAction":"SCMP_ACT_ALLOW"}' > /etc/docker/seccomp-sin-filtro.json && python -c "import json,os; p='/etc/docker/daemon.json'; d=json.load(open(p)) if os.path.exists(p) and os.path.getsize(p) else {}; d['seccomp-profile']='/etc/docker/seccomp-sin-filtro.json'; json.dump(d,open(p,'w'),indent=2)" && cat /etc/docker/daemon.json && systemctl restart docker
```

Para comprobarlo (ahora tiene que mostrar la versión sin el `--security-opt`):

```bash
docker run --rm quay.io/wildfly/wildfly:41.0.0.Final-jdk25 java -version
```

> **Costo de seguridad:** los contenedores quedan sin el filtro de llamadas del sistema. Es aceptable
> en un entorno académico con un solo nodo propio. En producción habría que usar un Docker 20.10.10 o
> más nuevo.

**Además, construir con el builder clásico** (`DOCKER_BUILDKIT=0`): BuildKit, en este Docker viejo,
puede aplicar su propio filtro y volver a fallar.

## 5. Problema 3: el campo `platform` del servicio de base de datos

**Síntoma:** las imágenes se construyen, pero falla al crear los contenedores:

```
"specify container image platform" requires API version 1.41, but the Docker daemon API version is 1.40
```

**Causa:** el servicio `db` tiene `platform: linux/amd64` (está para las Mac con Apple Silicon), y la
API de Docker 19.03 es la 1.40.

**Solución:** un `docker-compose.override.yml` **solo en el servidor** (docker-compose lo lee solo,
sin tocar el repo). El mismo archivo cierra los puertos que no tienen que ser públicos: la base, la
consola de administración de WildFly, el nodo Balanzas y la consola de administración de Keycloak.

```bash
cd /root/application && printf 'services:\n  db:\n    platform: !reset null\n    ports: !reset []\n  central:\n    ports: !override\n      - "8080:8080"\n  balanzas:\n    ports: !reset []\n  keycloak:\n    ports: !reset []\n' > docker-compose.override.yml && docker-compose config --quiet && echo "override OK"
```

## 6. Configuración del entorno (`.env`)

En `/root/application/.env` (no se versiona). Reemplazar `DOM` si el entorno tiene otro nombre:

```bash
cd /root/application && DOM=env-8268946.web.elasticloud.uy; cat > .env <<EOF
FRONTEND_PUERTO=80
FRONTEND_URL_WEB=https://$DOM
KEYCLOAK_URL_PUBLICA=https://$DOM/gubuy
GUBUY_REDIRECT_URI=https://$DOM/carga/api/auth/gubuy/callback
GUBUY_CLIENT_SECRET=$(openssl rand -hex 24)
KEYCLOAK_ADMIN_PASSWORD=$(openssl rand -hex 12)
JWT_SECRET=$(openssl rand -hex 32)
ADMIN_INICIAL_PASSWORD=$(openssl rand -hex 8)
EOF
grep ADMIN_INICIAL_PASSWORD .env
```

- **`FRONTEND_PUERTO=80`:** el frontend (nginx) queda en el 80, que es por donde entran el dominio y el SSL.
- **URLs con el dominio:** con los valores por defecto apuntan a `localhost`, y el ingreso con gub.uy no anda.
  `KEYCLOAK_URL_PUBLICA` es la dirección con la que el navegador llega al Keycloak (a través de nginx):
  Keycloak la pone como emisor de los tokens y el central la exige al validarlos (ADR-0026).
- **`GUBUY_CLIENT_SECRET`:** lo leen Keycloak (secreto del cliente `carga-uy-local`) y el central, así que
  con una sola línea quedan de acuerdo.
- **Claves generadas:** se reemplazan las de desarrollo, que son públicas en el repo.
- **La contraseña del admin** es la que muestra la última línea. El admin se crea **una sola vez**, en
  el primer arranque, con esa contraseña. Después se cambia desde el backoffice.

## 7. Levantar

En segundo plano, para que no se corte si se cierra la consola:

```bash
cd /root/application && DOCKER_BUILDKIT=0 nohup docker-compose up -d --build > /root/up.log 2>&1 &
```

La primera vez tarda entre 10 y 20 minutos. Para ver el avance sin quedar trabado, mirar solo las
últimas líneas (no usar `tail -f`, que ocupa la consola):

```bash
tail -20 /root/up.log
```

Cuando los contenedores estén `Started`, WildFly tarda 1 o 2 minutos más en desplegar:

```bash
cd /root/application && docker-compose ps && curl -s localhost:8080/carga/api/salud
```

## 8. HTTPS

1. **Quitar la IP pública del nodo.** Desplegar el nodo → fila de la IP pública → **Detach**. Con IP
   pública, el dominio apunta directo al nodo y no pasa por el balanceador, que es el que pone el
   certificado; además deja expuestos todos los puertos.
2. **Activar Built-in SSL.** En la fila del entorno → **Change Environment Topology** → **SSL** →
   **Built-In SSL** → **Apply**. El certificado es de Let's Encrypt para `*.web.elasticloud.uy`.
3. **Del lado de la aplicación** ya está resuelto en el repo (`carga-front/nginx.conf`):
   - nginx reenvía `/carga/` al central y `/gubuy/` al Keycloak que hace de gub.uy (ADR-0026), así que
     todo entra por el puerto 80. La consola de administración de Keycloak y el realm `master` no se
     publican por ahí (404);
   - si el balanceador avisa que el pedido llegó por http (`X-Forwarded-Proto: http`), redirige a `https://`;
   - en desarrollo ese encabezado no viene y todo sigue igual que siempre.

Para verificar:

```bash
curl -s https://env-8268946.web.elasticloud.uy/gubuy/realms/gubuy/.well-known/openid-configuration | head -c 150; echo
curl -s https://env-8268946.web.elasticloud.uy/carga/api/salud
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' http://env-8268946.web.elasticloud.uy/
```

## 9. Actualizar a una versión nueva del repo

```bash
cd /root/application && git pull && DOCKER_BUILDKIT=0 nohup docker-compose up -d --build > /root/up.log 2>&1 &
```

El override y el `.env` del servidor se conservan, porque git no los toca. Si solo cambió el frontend
o el central, se pueden nombrar para que sea más rápido:
`docker-compose up -d --build frontend central`.

### Pasar del simulador de gub.uy a Keycloak (servidor desplegado antes del 2026-11-18)

El simulador se reemplazó por Keycloak (ADR-0026). En el servidor hay que cambiar el `.env` (la línea
`GUBUY_AUTHORIZATION_ENDPOINT` apuntaba a la pantalla del simulador, que ya no existe) y cerrar el
puerto de Keycloak en el override. Se pega todo junto:

```bash
cd /root/application && git pull && DOM=env-8268946.web.elasticloud.uy
sed -i '/^GUBUY_AUTHORIZATION_ENDPOINT=/d; /^GUBUY_ISSUER=/d' .env
grep -q '^KEYCLOAK_URL_PUBLICA=' .env || printf 'KEYCLOAK_URL_PUBLICA=https://%s/gubuy\nGUBUY_CLIENT_SECRET=%s\nKEYCLOAK_ADMIN_PASSWORD=%s\n' "$DOM" "$(openssl rand -hex 24)" "$(openssl rand -hex 12)" >> .env
grep -q '  keycloak:' docker-compose.override.yml || printf '  keycloak:\n    ports: !reset []\n' >> docker-compose.override.yml
docker-compose config --quiet && echo "config OK"
DOCKER_BUILDKIT=0 nohup docker-compose up -d --build --remove-orphans > /root/up.log 2>&1 &
```

`--remove-orphans` baja el contenedor del simulador viejo. Keycloak tarda de 1 a 3 minutos en
arrancar (importa el realm `gubuy` con los usuarios de prueba) y usa unos 500 MB de memoria más; si el
nodo queda corto, subir los cloudlets.

### Usuarios de gub.uy en la nube

Los usuarios salen de `docker/keycloak/gubuy-realm.json` (la cédula es el usuario, la clave
`carga-solo-desarrollo`) y se importan cuando se crea el contenedor. Para agregar una persona sin tocar
el repo (sobrevive a reinicios, se pierde si se recrea el contenedor), cambiar `C` y `N`:

```bash
cd /root/application && C=51234561 N="Nombre Apellido"
docker-compose exec -T -e C="$C" -e N="$N" keycloak bash -c '
K=/opt/keycloak/bin/kcadm.sh
$K config credentials --server http://localhost:8080/gubuy --realm master --user admin --password "$KC_BOOTSTRAP_ADMIN_PASSWORD" &&
$K create users -r gubuy -s username="$C" -s enabled=true -s emailVerified=true -s email="$C@correo.uy" \
  -s firstName="${N%% *}" -s lastName="${N#* }" \
  -s "attributes.numero_documento=[\"$C\"]" -s "attributes.nombre_completo=[\"$N\"]" &&
$K set-password -r gubuy --username "$C" --new-password carga-solo-desarrollo && echo "usuario $C creado"'
```

Para que quede fija, agregarla a `docker/keycloak/gubuy-realm.json` (o al generador) y hacer
`git pull && docker-compose up -d --force-recreate keycloak` (sin `--force-recreate` no se importa: el
archivo está montado, Compose no ve el cambio y Keycloak, al reiniciar, saltea un realm que ya existe).

Para cargar los datos de demostración:

```bash
cd /root/application && docker-compose exec -T db psql -U carga -d carga < pruebas/datos-demo.sql
```

## 10. Problemas frecuentes

| Síntoma | Causa | Qué hacer |
|---|---|---|
| La raíz muestra "Apache Tomcat" y `/carga` da 404 | Entorno Tomcat | Usar Docker Engine CE (sección 2) |
| "Deployment failed", con `run.log` solo diciendo "Cloning into 'application'" | docker-compose 1.25 | Sección 3 |
| `Invalid interpolation format ... service "services"` | docker-compose 1.25 | Sección 3 |
| `runc did not terminate sucessfully` o `pthread_create failed (EPERM)` | seccomp de Docker 19.03 y `clone3` | Sección 4 |
| `requires API version 1.41, but the Docker daemon API version is 1.40` | `platform:` en el compose | Sección 5 |
| La página rechaza la conexión | Todavía no se levantó, o el frontend no está en el 80 | Secciones 6 y 7 |
| El ingreso con gub.uy vuelve a `localhost` | Faltan las URLs en el `.env` | Sección 6 |
| "Invalid parameter: redirect_uri", 404 en `/gubuy/authorize` u `OIDC_ERROR` | Quedó el `.env` del simulador | Sección 9, "Pasar del simulador de gub.uy a Keycloak" |
| `https://` no responde | Hay IP pública, o el SSL no está activo | Sección 8 |
| El admin no entra | Se está usando la contraseña de desarrollo; tras 5 intentos fallidos, se bloquea 15 minutos | Usar la del `.env`. Para desbloquear: `docker-compose exec -T db psql -U carga -d carga -c "UPDATE cuenta_interna SET intentos_fallidos = 0, bloqueado_hasta = NULL WHERE nombre_usuario = 'admin';"` |
| La consola web no abre | Bloqueador de anuncios en el navegador | Desactivarlo para `app.elasticloud.uy`, o usar *Duplicate Session* |

## 11. Pendiente

- La letra pide los nodos periféricos (Balanzas y Tracking) en **otra plataforma** PaaS o IaaS.
  Hoy corren en el mismo nodo de Elastic Cloud; al moverlos, el central les habla por `https://`
  (variables `NODO_BALANZAS_URL` y, en el nodo Tracking, la URL del central).
- El ingreso con **ID Uruguay real** necesita que se registre
  `https://env-8268946.web.elasticloud.uy/carga/api/auth/gubuy/callback` en el cliente de testing.
  Mientras tanto, por pedido del docente, se usa Keycloak (ADR-0026).
