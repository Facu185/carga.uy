# -*- coding: utf-8 -*-
"""
Demo de punta a punta de la fiscalizacion (P3 + P4 + P5), contra el sistema levantado con el
simulador de gub.uy y los datos de pruebas/datos-demo.sql:

  1. Una guia Montevideo -> Colonia para el camion DEMO11, con un recorrido de tracking que se
     desvia hacia Florida.
  2. El chofer ingresa con gub.uy y registra el inicio y el fin del viaje (como la app movil).
  3. El fin de viaje obtiene los pesajes (P4) y evalua el desvio (P5): aparece un caso DESVIO_RUTA.
  4. Pesajes de DEMO53 (capacidad declarada baja): casos de EXCESO_CARGA.

    python pruebas/demo-fiscalizacion.py

Necesita docker compose (usa psql dentro del contenedor db para preparar la guia y el recorrido).
"""
import http.cookiejar
import json
import subprocess
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime, timedelta, timezone

FRONT = "http://localhost:3000"
API = FRONT + "/carga/api"
CHOFER = "41000019"  # Chofer demo 1, empresa 7001, camion DEMO11


def sql(consulta):
    r = subprocess.run(["docker", "compose", "exec", "-T", "db", "psql", "-U", "carga", "-d", "carga", "-Atc", consulta],
                       capture_output=True, text=True, check=True)
    return r.stdout.strip()


def pedir(ruta, token=None, metodo="GET", cuerpo=None, encabezados=None):
    datos = json.dumps(cuerpo).encode() if cuerpo is not None else None
    req = urllib.request.Request(API + ruta, data=datos, method=metodo, headers=encabezados or {})
    if datos:
        req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.loads(r.read().decode() or "null")
    except urllib.error.HTTPError as e:
        texto = e.read().decode()
        try:
            return e.code, json.loads(texto)
        except ValueError:
            return e.code, texto


class SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


def ingresar_gubuy(cedula):
    abrir = urllib.request.build_opener(SinRedireccion, urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())).open

    def ir(url, datos=None):
        try:
            return abrir(urllib.request.Request(url, data=datos), timeout=30).headers
        except urllib.error.HTTPError as e:
            return e.headers

    autorizar = ir(API + "/auth/gubuy/iniciar?destino=web").get("Location")
    callback = ir(autorizar, urllib.parse.urlencode({"username": cedula, "claims": "{}"}).encode()).get("Location")
    fragmento = urllib.parse.parse_qs((ir(callback).get("Location") or "").split("#", 1)[-1])
    return fragmento["token"][0], fragmento.get("roles", [""])[0]


def evento(token, guia, tipo, instante, lat, lon, traza=None):
    # Igual que la app movil: traceparent W3C y X-Cliente (src/services/traza.ts del repo movil)
    encabezados = {"X-Cliente": "app-movil"}
    if traza:
        encabezados["traceparent"] = f"00-{traza}-{uuid.uuid4().hex[:16]}-01"
    return pedir("/eventos-viaje", token, "POST", {"idEvento": str(uuid.uuid4()), "guiaId": guia, "tipo": tipo,
                                                    "instante": instante, "latitud": lat, "longitud": lon}, encabezados)


def main():
    ahora = datetime.now(timezone.utc).replace(microsecond=0)
    inicio = ahora - timedelta(hours=2)
    print("1. Guia Montevideo -> Colonia para DEMO11 y su recorrido con desvio hacia Florida")
    guia = sql(f"""INSERT INTO guia (empresa_id, vehiculo_id, chofer_id, fecha, origen, destino, estado, creada_en, peso_kg,
                   inicio, fin, origen_lat, origen_lon, destino_lat, destino_lon)
                   SELECT v.empresa_id, v.id, c.id, current_date, 'Montevideo', 'Colonia', 'ASIGNADA', now(), 20000,
                          '{inicio.isoformat()}', '{(ahora + timedelta(hours=3)).isoformat()}', -34.9011, -56.1645, -34.4626, -57.8400
                   FROM vehiculo v JOIN chofer c ON c.empresa_id = v.empresa_id AND c.cedula = '{CHOFER}'
                   WHERE v.matricula = 'DEMO11' RETURNING id""").splitlines()[0]
    # 20 posiciones: las primeras 6 sobre la ruta y el resto subiendo hacia Florida (-34.10, -56.21)
    puntos = []
    for i in range(20):
        t = inicio + timedelta(minutes=5 * i)
        if i < 6:
            lat, lon = -34.9011 + i * 0.02, -56.1645 - i * 0.08
        else:
            lat, lon = -34.78 + (i - 6) * 0.05, -56.40 + (i - 6) * 0.01
        puntos.append(f"('demo-{guia}-{i}', 'DEMO11', {lat}, {lon}, '{t.isoformat()}', now())")
    sql("INSERT INTO posicion_tracking (id_evento, matricula, latitud, longitud, instante, recibido_en) VALUES "
        + ", ".join(puntos) + " ON CONFLICT (id_evento) DO NOTHING")
    print(f"   guia {guia}, 20 posiciones")

    print("2. El chofer ingresa con gub.uy y registra inicio y fin (como la app movil)")
    token, roles = ingresar_gubuy(CHOFER)
    print(f"   roles: {roles}")
    print("   INICIO:", evento(token, int(guia), "INICIO", (inicio + timedelta(minutes=1)).isoformat(), -34.9011, -56.1645))
    traza = uuid.uuid4().hex
    print("   FIN:   ", evento(token, int(guia), "FIN", (ahora - timedelta(minutes=1)).isoformat(), -34.10, -56.21, traza))

    _, sesion = pedir("/auth/login", metodo="POST", cuerpo={"usuario": "admin", "password": "admin-solo-desarrollo"})
    admin = sesion["token"]
    print("3. Esperando la deteccion asincronica del desvio (cola de fin de viaje)")
    caso = None
    for _ in range(30):
        _, bandeja = pedir("/fiscalizacion/casos?tipo=DESVIO_RUTA&tamano=50", admin)
        caso = next((c for c in bandeja["registros"] if str(c.get("guiaId")) == guia), None)
        if caso:
            break
        time.sleep(1)
    print("   ", (caso["tipo"] + " " + caso["estado"] + ": " + caso["evidencia"]) if caso else "NO APARECIO EL CASO")

    print(f"   Traza movil -> central -> cola -> nodo Balanzas: http://localhost:16686/trace/{traza}")

    print("4. Pesajes de DEMO53 (capacidad declarada 12.000 kg): exceso de carga")
    ayer = (datetime.now() - timedelta(days=1)).date().isoformat()
    print("   ", pedir(f"/pesajes/sincronizar?matricula=DEMO53&fecha={ayer}", admin, "POST")[1].get("mensaje"))
    time.sleep(3)
    _, bandeja = pedir("/fiscalizacion/casos?tipo=EXCESO_CARGA&tamano=50", admin)
    for c in bandeja["registros"]:
        if c["matricula"] == "DEMO53":
            print("    EXCESO_CARGA", c["estado"] + ":", c["evidencia"][:150])


if __name__ == "__main__":
    main()
