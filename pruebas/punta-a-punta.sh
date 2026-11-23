#!/usr/bin/env bash
# Prueba de punta a punta contra el sistema levantado con docker compose (central, front, gub.uy, PDI, base).
# No usa mocks: cada paso es un pedido HTTP real, igual que un usuario o la app movil.
#
#   docker compose up -d --build
#   ./pruebas/punta-a-punta.sh
#
# Termina con codigo 0 si todo paso; si no, con la cantidad de fallas. La usa el job "integracion" del CI.
set -uo pipefail

API=${API:-http://localhost:8080/carga/api}
FRONT=${FRONT:-http://localhost:3000}
JAEGER=${JAEGER:-http://localhost:16686}
ADMIN_USUARIO=${ADMIN_INICIAL_USUARIO:-admin}
ADMIN_PASSWORD=${ADMIN_INICIAL_PASSWORD:-admin-solo-desarrollo}
# Cedula del padron del simulador PDI: el ingreso con gub.uy la verifica con DNIC
CEDULA=${CEDULA:-22222222}
# Keycloak de desarrollo, que hace de gub.uy (ADR-0026). Admin directo al contenedor: nginx no publica la consola
KEYCLOAK=${KEYCLOAK:-http://localhost:8085/gubuy}
KEYCLOAK_ADMIN_USUARIO=${KEYCLOAK_ADMIN_USUARIO:-admin}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-admin-solo-desarrollo}
CLAVE_GUBUY=carga-solo-desarrollo

fallas=0
ok() { echo "  OK    $1"; }
mal() { echo "  FALLA $1"; fallas=$((fallas + 1)); }

# Codigo HTTP de un pedido (los argumentos van directo a curl)
codigo() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

esperar_codigo() {
  local descripcion=$1 esperado=$2; shift 2
  local obtenido
  obtenido=$(codigo "$@")
  if [ "$obtenido" = "$esperado" ]; then ok "$descripcion ($obtenido)"; else mal "$descripcion: esperaba $esperado, vino $obtenido"; fi
}

contiene() {
  local descripcion=$1 texto=$2 buscado=$3
  if printf '%s' "$texto" | grep -q -- "$buscado"; then ok "$descripcion"; else mal "$descripcion: no aparece '$buscado'"; fi
}

token_de() { sed -n 's/.*"token":"\([^"]*\)".*/\1/p'; }

# Una cedula valida al azar (que no esta en el padron): fuerza una consulta a la PDI en cada corrida
cedula_al_azar() {
  local numero suma=0 i pesos=(2 9 8 7 6 3 4)
  numero=$(printf '%07d' $((RANDOM * 32768 % 9000000 + 1000000 + RANDOM % 1000)))
  for i in 0 1 2 3 4 5 6; do suma=$((suma + ${numero:$i:1} * ${pesos[$i]})); done
  echo "$numero$(( (10 - suma % 10) % 10 ))"
}

# Da de alta la persona en el Keycloak de desarrollo, como si tuviera usuario gub.uy (si ya existe, no hace nada)
asegurar_usuario_gubuy() {
  local cedula=$1 token
  token=$(curl -s -X POST "$KEYCLOAK/realms/master/protocol/openid-connect/token" \
    -d grant_type=password -d client_id=admin-cli \
    --data-urlencode "username=$KEYCLOAK_ADMIN_USUARIO" --data-urlencode "password=$KEYCLOAK_ADMIN_PASSWORD" \
    | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
  curl -s -o /dev/null -X POST "$KEYCLOAK/admin/realms/gubuy/users" \
    -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$cedula\",\"enabled\":true,\"emailVerified\":true,\"email\":\"$cedula@correo.uy\",
        \"firstName\":\"Persona\",\"lastName\":\"de Prueba\",
        \"attributes\":{\"numero_documento\":[\"$cedula\"],\"nombre_completo\":[\"Persona de Prueba\"]},
        \"credentials\":[{\"type\":\"password\",\"value\":\"$CLAVE_GUBUY\",\"temporary\":false}]}"
}

# Ingreso completo con gub.uy (Keycloak); deja la sesion en URL_FRONT y el traceId del callback en TRAZA_CALLBACK
ingresar_con_gubuy() {
  local cedula=$1 cookies url_gubuy pagina cookies_kc accion url_vuelta encabezados
  asegurar_usuario_gubuy "$cedula"
  cookies=$(mktemp)
  url_gubuy=$(curl -s -c "$cookies" -o /dev/null -w '%{redirect_url}' "$FRONT/carga/api/auth/gubuy/iniciar?destino=web")
  URL_GUBUY=$url_gubuy
  # Pantalla de login. Keycloak marca sus cookies Secure: por http curl solo las devuelve a localhost, asi que
  # se reenvian a mano (como hace el navegador en desarrollo) y la prueba anda igual en el CI
  pagina=$(curl -s -D - "$url_gubuy" | tr -d '\r')
  cookies_kc=$(printf '%s' "$pagina" | sed -n 's/^[Ss]et-[Cc]ookie: *\([^;]*\).*/\1/p' | paste -sd ';' - | sed 's/;/; /g')
  accion=$(printf '%s' "$pagina" | grep -o 'action="[^"]*login-actions/authenticate[^"]*"' | head -1 \
    | sed 's/^action="//; s/"$//; s/&amp;/\&/g')
  url_vuelta=$(curl -s -o /dev/null -w '%{redirect_url}' -H "Cookie: $cookies_kc" -X POST "$accion" \
    --data-urlencode "username=$cedula" --data-urlencode "password=$CLAVE_GUBUY")
  URL_VUELTA=$url_vuelta
  encabezados=$(curl -s -b "$cookies" -D - -o /dev/null "$url_vuelta" | tr -d '\r')
  URL_FRONT=$(printf '%s' "$encabezados" | sed -n 's/^[Ll]ocation: *//p')
  TRAZA_CALLBACK=$(printf '%s' "$encabezados" | sed -n 's/^[Xx]-[Tt]race-[Ii]d: *//p')
  rm -f "$cookies"
}

login() {
  curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"usuario\":\"$1\",\"password\":\"$2\"}"
}

echo "== Esperando a que el central este listo"
for _ in $(seq 1 90); do
  [ "$(codigo "$API/salud/listo")" = "200" ] && break
  sleep 2
done
esperar_codigo "el central atiende (salud/listo)" 200 "$API/salud/listo"
contiene "la base de datos esta UP" "$(curl -s "$API/salud/listo")" '"baseDeDatos":"UP"'
# Keycloak crea su base e importa el realm al arrancar (de 30 s a 3 minutos, segun la maquina)
for _ in $(seq 1 90); do
  [ "$(codigo "$FRONT/gubuy/realms/gubuy/.well-known/openid-configuration")" = "200" ] && break
  sleep 2
done
esperar_codigo "gub.uy (Keycloak) publica su configuracion por nginx" 200   "$FRONT/gubuy/realms/gubuy/.well-known/openid-configuration"

echo "== Seguridad del backoffice"
esperar_codigo "sin token no se accede a usuarios internos" 401 "$API/usuarios-internos"
esperar_codigo "un token inventado se rechaza" 401 "$API/usuarios-internos" -H "Authorization: Bearer inventado"
esperar_codigo "clave incorrecta" 401 -X POST "$API/auth/login" -H 'Content-Type: application/json' \
  -d "{\"usuario\":\"$ADMIN_USUARIO\",\"password\":\"incorrecta-123456\"}"

TOKEN_ADMIN=$(login "$ADMIN_USUARIO" "$ADMIN_PASSWORD" | token_de)
if [ -n "$TOKEN_ADMIN" ]; then ok "el administrador ingresa"; else mal "el administrador no pudo ingresar"; fi
ADMIN=(-H "Authorization: Bearer $TOKEN_ADMIN")
contiene "el token identifica al administrador" "$(curl -s "$API/auth/yo" "${ADMIN[@]}")" 'ADMINISTRADOR'
esperar_codigo "el administrador lista usuarios internos" 200 "$API/usuarios-internos" "${ADMIN[@]}"

AUTORIDAD="autoridad-$(date +%s)"
CLAVE_AUTORIDAD="clave-de-prueba-$RANDOM-ok"
esperar_codigo "el administrador crea una autoridad" 201 -X POST "$API/usuarios-internos" "${ADMIN[@]}" \
  -H 'Content-Type: application/json' \
  -d "{\"usuario\":\"$AUTORIDAD\",\"nombre\":\"Autoridad de prueba\",\"password\":\"$CLAVE_AUTORIDAD\",\"rol\":\"AUTORIDAD\"}"
TOKEN_AUTORIDAD=$(login "$AUTORIDAD" "$CLAVE_AUTORIDAD" | token_de)
if [ -n "$TOKEN_AUTORIDAD" ]; then ok "la autoridad ingresa"; else mal "la autoridad no pudo ingresar"; fi
esperar_codigo "una autoridad NO gestiona usuarios (403)" 403 "$API/usuarios-internos" \
  -H "Authorization: Bearer $TOKEN_AUTORIDAD"
esperar_codigo "una autoridad SI consulta la auditoria" 200 "$API/auditoria" -H "Authorization: Bearer $TOKEN_AUTORIDAD"
contiene "el alta quedo en la auditoria" "$(curl -s "$API/auditoria?entidadId=$AUTORIDAD" "${ADMIN[@]}")" \
  'USUARIO_INTERNO_CREADO'

echo "== Seguridad de los nodos perifericos y de P4"
NODO_TRACKING_TOKEN=${NODO_TRACKING_TOKEN:-tracking-solo-desarrollo}
INSTANTE_POSICION=$(date -u +%Y-%m-%dT%H:%M:%SZ)
POSICION="{\"idEvento\":\"e2e-$(date +%s)\",\"matricula\":\"E2E0001\",\"latitud\":-34.9,\"longitud\":-56.16,\"instante\":\"$INSTANTE_POSICION\"}"
esperar_codigo "sin token de nodo no se aceptan posiciones" 401 -X POST "$API/tracking/posiciones" \
  -H 'Content-Type: application/json' -d "$POSICION"
esperar_codigo "un token de nodo inventado se rechaza" 401 -X POST "$API/tracking/posiciones" \
  -H 'Content-Type: application/json' -H 'X-Nodo-Token: inventado' -d "$POSICION"
esperar_codigo "con el token del nodo la posicion se acepta" 202 -X POST "$API/tracking/posiciones" \
  -H 'Content-Type: application/json' -H "X-Nodo-Token: $NODO_TRACKING_TOKEN" -d "$POSICION"
esperar_codigo "sin sesion no se consulta el monitor de nodos" 401 "$API/monitor/nodos"
esperar_codigo "una autoridad NO consulta el monitor de nodos (403)" 403 "$API/monitor/nodos" \
  -H "Authorization: Bearer $TOKEN_AUTORIDAD"
esperar_codigo "el administrador consulta el monitor de nodos" 200 "$API/monitor/nodos" "${ADMIN[@]}"
contiene "el central llega a Balanzas con su token" "$(curl -s "$API/monitor/nodos" "${ADMIN[@]}")" \
  'Responde a la consulta de pesadas'
esperar_codigo "sin sesion no se administran nodos" 401 "$API/nodos"
esperar_codigo "una autoridad NO administra nodos (403)" 403 "$API/nodos" -H "Authorization: Bearer $TOKEN_AUTORIDAD"
contiene "el administrador ve el nodo Balanzas registrado" "$(curl -s "$API/nodos" "${ADMIN[@]}")" '"tipo":"BALANZAS"'
RANGO="matricula=E2E0001&desde=$(date -u -d '-1 hour' +%Y-%m-%dT%H:%M:%SZ)&hasta=$(date -u -d '+1 hour' +%Y-%m-%dT%H:%M:%SZ)"
esperar_codigo "sin sesion no se consulta el recorrido de un vehiculo" 401 "$API/recorridos?$RANGO"
# la posicion se guarda de forma asincronica (cola + MDB): se espera un poco a que aparezca. Se busca
# por su instante (si la prueba se repite dentro de la hora, el recorrido trae tambien las anteriores)
for _ in $(seq 1 10); do
  RECORRIDO=$(curl -s "$API/recorridos?$RANGO" "${ADMIN[@]}")
  case "$RECORRIDO" in *"\"instante\":\"$INSTANTE_POSICION\""*) break ;; esac
  sleep 1
done
contiene "el recorrido incluye la posicion enviada por el nodo" "$RECORRIDO" "\"instante\":\"$INSTANTE_POSICION\""

echo "== Ingreso con gub.uy (Keycloak de desarrollo) y verificacion con DNIC (PDI)"
ingresar_con_gubuy "$CEDULA"
contiene "el central redirige a gub.uy con PKCE" "$URL_GUBUY" 'code_challenge='
contiene "gub.uy vuelve al central con un codigo" "$URL_VUELTA" 'code='
contiene "el central entrega la sesion al front" "$URL_FRONT" '#token='
TOKEN_CIUDADANO=$(printf '%s' "$URL_FRONT" | sed -n 's/.*[#&]token=\([^&]*\).*/\1/p')
PERFIL=$(curl -s "$API/mi-perfil" -H "Authorization: Bearer $TOKEN_CIUDADANO")
contiene "el perfil es el de la cedula de gub.uy" "$PERFIL" "\"cedula\":\"$CEDULA\""
contiene "la persona quedo verificada con DNIC" "$PERFIL" '"verificacionDnic":"VERIFICADO"'
esperar_codigo "un ciudadano NO entra al backoffice (403)" 403 "$API/usuarios-internos" \
  -H "Authorization: Bearer $TOKEN_CIUDADANO"

echo "== Observabilidad"
RESPUESTA=$(curl -s -i "$API/salud" -H 'X-Correlation-Id: prueba-punta-a-punta')
contiene "el ID de correlacion vuelve en la respuesta" "$RESPUESTA" 'prueba-punta-a-punta'
METRICAS=$(curl -s "$API/metricas")
contiene "metricas de pedidos HTTP" "$METRICAS" 'carga_http_pedidos_total'
contiene "metricas de ingresos" "$METRICAS" 'carga_ingresos_total{mecanismo="gubuy",resultado="exitoso"}'
contiene "cada respuesta trae su traceId" "$RESPUESTA" 'X-Trace-Id'
contiene "el central continua un traceparent recibido" \
  "$(curl -s -i "$API/salud" -H 'traceparent: 00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01')" \
  '4bf92f3577b34da6a3ce929d0e0e4736'

echo "== Traza distribuida del ingreso con gub.uy (Jaeger)"
if [ "$(codigo "$JAEGER/api/services")" = "200" ]; then
  ingresar_con_gubuy "$(cedula_al_azar)"
  TRAZA=""
  for _ in $(seq 1 15); do
    TRAZA=$(curl -s "$JAEGER/api/traces/$TRAZA_CALLBACK")
    printf '%s' "$TRAZA" | grep -q 'simulador-pdi' && break
    sleep 1
  done
  contiene "la traza tiene el tramo del central" "$TRAZA" '"serviceName":"central"'
  contiene "la traza incluye el canje con gub.uy" "$TRAZA" 'gub.uy POST /token'
  contiene "la traza llega al simulador de DNIC (otro despliegue)" "$TRAZA" '"serviceName":"simulador-pdi"'
  echo "        Para verla: $JAEGER/trace/$TRAZA_CALLBACK"
else
  mal "Jaeger no responde en $JAEGER"
fi

echo
if [ "$fallas" -eq 0 ]; then echo "Todo OK"; else echo "$fallas falla(s)"; fi
exit "$fallas"
