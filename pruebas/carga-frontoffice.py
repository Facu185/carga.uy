# -*- coding: utf-8 -*-
"""
Usuarios simulados del frontoffice para la prueba de performance (letra 4.3, punto 3a: p95 < 500 ms).

Mientras el generador de carga manda posiciones, N usuarios recorren las consultas del frontoffice
con pausas de persona real: las públicas (validar un permiso, cifras) y, con sesión de gub.uy
(Keycloak de desarrollo, ADR-0026), su perfil. Cada minuto imprime p50/p95/p99 y al final un resumen por consulta.
El detalle de cada pedido queda en resultados-carga/frontoffice-<fecha>.csv.

    python pruebas/carga-frontoffice.py --usuarios 20 --duracion 300

Solo usa la biblioteca estándar. Necesita el Keycloak de desarrollo (compose por defecto): las
personas de la prueba se dan de alta solas en el realm gubuy (pruebas/ingreso_gubuy.py).
"""
import argparse
import csv
import json
import os
import random
import re
import sys
import statistics
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from ingreso_gubuy import asegurar_usuario, cedula_valida, ingresar  # noqa: E402

FRONT = os.environ.get("FRONT", "http://localhost:3000")
API = FRONT + "/carga/api"
MATRICULAS = ["STZ1234", "STZ5678", "CUY9012", "DEMO11", "DEMO22", "NOEXISTE1"]


def consultas(token):
    m = random.choice(MATRICULAS)
    return random.choices([
        ("publico/permisos/vehiculo", f"/publico/permisos/vehiculo?matricula={m}", None),
        ("publico/permisos/empresa", f"/publico/permisos/empresa?numero={random.choice([1001, 1002, 7001, 7003, 4242])}", None),
        ("publico/estadisticas", "/publico/estadisticas", None),
        ("mi-perfil", "/mi-perfil", token),
        ("auth/yo", "/auth/yo", token),
    ], weights=[35, 15, 20, 20, 10])[0]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--usuarios", type=int, default=20)
    p.add_argument("--duracion", type=int, default=300, help="segundos")
    p.add_argument("--pausa", type=float, default=1.0, help="pausa media entre pedidos de un usuario, en segundos")
    a = p.parse_args()

    cedulas = [cedula_valida(4_000_000 + i) for i in range(a.usuarios)]
    for i, cedula in enumerate(cedulas):
        asegurar_usuario(cedula, f"Persona Carga{i + 1}")
    tokens = [ingresar(cedula) for cedula in cedulas]
    registros = []
    candado = threading.Lock()
    fin = time.time() + a.duracion

    def usuario(token):
        while time.time() < fin:
            nombre, ruta, auth = consultas(token)
            req = urllib.request.Request(API + ruta, headers={"Authorization": "Bearer " + auth} if auth else {})
            inicio = time.perf_counter()
            try:
                with urllib.request.urlopen(req, timeout=30) as r:
                    r.read()
                    codigo = r.status
            except urllib.error.HTTPError as e:
                codigo = e.code
            except Exception:
                codigo = 0
            ms = (time.perf_counter() - inicio) * 1000
            with candado:
                registros.append((time.time(), nombre, codigo, ms))
            time.sleep(random.expovariate(1 / a.pausa))

    hilos = [threading.Thread(target=usuario, args=(t,), daemon=True) for t in tokens]
    for h in hilos:
        h.start()
    print(f"{a.usuarios} usuarios del frontoffice durante {a.duracion} s contra {API}", flush=True)
    visto = 0
    while any(h.is_alive() for h in hilos):
        time.sleep(60 if time.time() + 60 < fin else max(1, fin - time.time() + 2))
        with candado:
            nuevos = registros[visto:]
            visto = len(registros)
        if nuevos:
            print(f"{datetime.now():%H:%M:%S} " + resumen([r[3] for r in nuevos], [r[2] for r in nuevos]), flush=True)

    os.makedirs("resultados-carga", exist_ok=True)
    archivo = f"resultados-carga/frontoffice-{datetime.now():%Y%m%d-%H%M%S}.csv"
    with open(archivo, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["epoch", "consulta", "http", "ms"])
        w.writerows(registros)
    print("\nRESUMEN POR CONSULTA")
    for nombre in sorted({r[1] for r in registros}):
        filas = [r for r in registros if r[1] == nombre]
        print(f"  {nombre:28} " + resumen([r[3] for r in filas], [r[2] for r in filas]))
    print(f"  {'TOTAL':28} " + resumen([r[3] for r in registros], [r[2] for r in registros]))
    print("Detalle:", archivo)


def resumen(ms, codigos):
    q = statistics.quantiles(ms, n=100) if len(ms) >= 2 else [ms[0]] * 99
    errores = sum(1 for c in codigos if c == 0 or c >= 500)
    return f"n={len(ms):5d}  p50={q[49]:6.1f} ms  p95={q[94]:6.1f} ms  p99={q[98]:6.1f} ms  errores={errores}"


if __name__ == "__main__":
    main()
