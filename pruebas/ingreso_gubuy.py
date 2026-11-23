# -*- coding: utf-8 -*-
"""
Ingreso con "gub.uy" para los scripts de prueba, contra el Keycloak de desarrollo (ADR-0026).

    from ingreso_gubuy import asegurar_usuario, ingresar
    asegurar_usuario("41000019", "Chofer Demo Uno")
    token = ingresar("41000019")          # sesion de carga.uy, como la recibe el front

Hace lo mismo que una persona en el navegador: el central redirige a la pantalla de login del realm
gubuy, se envia el formulario y el central entrega su sesion en el fragmento (#token=...).
Solo biblioteca estandar.
"""
import html
import http.cookiejar
import json
import os
import re
import urllib.error
import urllib.parse
import urllib.request

FRONT = os.environ.get("FRONT", "http://localhost:3000")
API = FRONT + "/carga/api"
# Admin de Keycloak: directo al contenedor (nginx no publica la consola ni el realm master)
KEYCLOAK = os.environ.get("KEYCLOAK", "http://localhost:8085/gubuy")
KEYCLOAK_ADMIN = os.environ.get("KEYCLOAK_ADMIN_USUARIO", "admin")
KEYCLOAK_CLAVE_ADMIN = os.environ.get("KEYCLOAK_ADMIN_PASSWORD", "admin-solo-desarrollo")
CLAVE_DEMO = "carga-solo-desarrollo"


class _SinRedireccion(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *a, **k):
        return None


class _PoliticaDev(http.cookiejar.DefaultCookiePolicy):
    # Keycloak marca sus cookies Secure: por http (desarrollo) igual hay que devolverlas, como hace el navegador
    def return_ok_secure(self, cookie, request):
        return True


def _navegador():
    jar = http.cookiejar.CookieJar(_PoliticaDev())
    abrir = urllib.request.build_opener(_SinRedireccion, urllib.request.HTTPCookieProcessor(jar)).open

    def pedir(url, datos=None, encabezados=None, metodo=None):
        try:
            r = abrir(urllib.request.Request(url, data=datos, headers=encabezados or {}, method=metodo), timeout=60)
            return r.status, r.headers, r.read().decode(errors="replace")
        except urllib.error.HTTPError as e:
            return e.code, e.headers, e.read().decode(errors="replace")
    return pedir


def ingresar(cedula, clave=CLAVE_DEMO, destino="web"):
    """Ingreso completo con gub.uy (Keycloak). Devuelve el token de carga.uy o lanza un error claro."""
    return ingresar_sesion(cedula, clave, destino)["token"]


def ingresar_sesion(cedula, clave=CLAVE_DEMO, destino="web"):
    """Como ingresar(), pero devuelve todo lo que el central manda en el fragmento: token, roles, ..."""
    pedir = _navegador()
    _, h, _ = pedir(API + "/auth/gubuy/iniciar?destino=" + destino)
    estado, _, pagina = pedir(h["Location"])
    accion = re.search(r'action="([^"]*login-actions/authenticate[^"]*)"', pagina)
    if not accion:
        raise RuntimeError(f"No aparecio la pantalla de login de gub.uy (HTTP {estado})")
    datos = urllib.parse.urlencode({"username": cedula, "password": clave}).encode()
    estado, h, _ = pedir(html.unescape(accion.group(1)), datos)
    if estado != 302 or "code=" not in h.get("Location", ""):
        raise RuntimeError(f"gub.uy no acepto el ingreso de {cedula} (HTTP {estado}); revisar usuario y clave")
    _, h, _ = pedir(h["Location"])
    fragmento = urllib.parse.parse_qs(h.get("Location", "").split("#", 1)[-1])
    if "token" not in fragmento:
        raise RuntimeError("El central no entrego la sesion: " + h.get("Location", "")[:150])
    return {clave_: valores[0] for clave_, valores in fragmento.items()}


def _token_admin():
    pedir = _navegador()
    _, _, cuerpo = pedir(KEYCLOAK + "/realms/master/protocol/openid-connect/token", urllib.parse.urlencode(
        {"grant_type": "password", "client_id": "admin-cli", "username": KEYCLOAK_ADMIN,
         "password": KEYCLOAK_CLAVE_ADMIN}).encode())
    return pedir, json.loads(cuerpo)["access_token"]


def asegurar_usuario(cedula, nombre_completo, clave=CLAVE_DEMO):
    """Crea la persona en el realm gubuy si no existe (como si tuviera usuario gub.uy). Idempotente."""
    pedir, token = _token_admin()
    nombre, _, apellido = nombre_completo.partition(" ")
    usuario = {"username": cedula, "enabled": True, "emailVerified": True, "email": f"{cedula}@correo.uy",
               "firstName": nombre, "lastName": apellido or "-",
               "attributes": {"numero_documento": [cedula], "nombre_completo": [nombre_completo]},
               "credentials": [{"type": "password", "value": clave, "temporary": False}]}
    estado, _, cuerpo = pedir(KEYCLOAK + "/admin/realms/gubuy/users", json.dumps(usuario).encode(),
                              {"Authorization": "Bearer " + token, "Content-Type": "application/json"})
    if estado not in (201, 409):
        raise RuntimeError(f"No se pudo crear el usuario {cedula} en Keycloak (HTTP {estado}): {cuerpo[:200]}")


def cedula_valida(base):
    """Cedula uruguaya con su digito verificador, a partir de 7 digitos."""
    digitos = [int(d) for d in f"{base:07d}"]
    suma = sum(d * p for d, p in zip(digitos, [2, 9, 8, 7, 6, 3, 4]))
    return f"{base:07d}{(10 - suma % 10) % 10}"
