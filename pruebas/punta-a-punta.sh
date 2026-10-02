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
ADMIN_USUARIO=${ADMIN_INICIAL_USUARIO:-admin}
ADMIN_PASSWORD=${ADMIN_INICIAL_PASSWORD:-admin-solo-desarrollo}
# Cedula del padron del simulador PDI: el ingreso con gub.uy la verifica con DNIC
CEDULA=${CEDULA:-22222222}

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

echo "== Ingreso con gub.uy (simulador) y verificacion con DNIC (PDI)"
COOKIES=$(mktemp)
trap 'rm -f "$COOKIES"' EXIT
# 1. El central arma la redireccion a gub.uy y guarda la transaccion en una cookie
URL_GUBUY=$(curl -s -c "$COOKIES" -o /dev/null -w '%{redirect_url}' "$FRONT/carga/api/auth/gubuy/iniciar?destino=web")
contiene "el central redirige a gub.uy con PKCE" "$URL_GUBUY" 'code_challenge='
# 2. La persona se identifica en gub.uy (el simulador recibe el formulario en la misma URL)
URL_VUELTA=$(curl -s -o /dev/null -w '%{redirect_url}' -X POST "$URL_GUBUY" \
  --data-urlencode "username=$CEDULA" \
  --data-urlencode 'claims={"nombre_completo":"Persona de Prueba","email":"prueba@example.com"}')
contiene "gub.uy vuelve al central con un codigo" "$URL_VUELTA" 'code='
# 3. El central canjea el codigo, valida el id_token y redirige al front con la sesion en el fragmento
URL_FRONT=$(curl -s -b "$COOKIES" -o /dev/null -w '%{redirect_url}' "$URL_VUELTA")
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

echo
if [ "$fallas" -eq 0 ]; then echo "Todo OK"; else echo "$fallas falla(s)"; fi
exit "$fallas"
