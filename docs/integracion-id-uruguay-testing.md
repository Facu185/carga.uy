# ID Uruguay real — ambiente de testing

## Alcance y estado

El central implementa Authorization Code + PKCE y obtiene los datos personales de
UserInfo después de validar el `id_token`. Ambos `sub` deben coincidir exactamente.
Documento obligatorio; nombre/correo opcionales según la información concedida.
Los tokens de ID Uruguay no se entregan a la SPA ni al móvil: reciben la sesión
propia existente de carga.uy. No cambian roles, DTO ni destinos del callback.

**Implementación y configuración preparadas; conexión real NO validada.** No se
ejecutaron builds, JUnit, contenedores ni autenticaciones reales durante este cambio.
El endpoint público de discovery tampoco pudo verificarse desde la herramienta web.
Fuentes: [documentación oficial de Agesic](https://www.gub.uy/agencia-gobierno-electronico-sociedad-informacion-conocimiento/comunicacion/publicaciones/documentacion-tecnica-id-uruguay/documentacion-tecnica-id-uruguay/id-0),
URIs de testing aportadas por el usuario y [ADR-0017](decisiones/0017-id-uruguay-userinfo-testing.md).

## Configuración del operador

1. Solicitar un secreto vigente/rotado por canal privado. No reutilizar el secreto
   expuesto en el chat; no incluirlo en Git, comandos compartidos, capturas ni logs.
2. En `.env` local (ignorado por Git), completar una única línea
   `GUBUY_CLIENT_SECRET` con ese valor. No modificar `.env.example` con secretos.
3. Utilizar el [override de testing](../docker-compose.gubuy-testing.yml) junto al
   Compose base. Selecciona estos valores no secretos:

   | Variable | Valor |
   | --- | --- |
   | `GUBUY_ISSUER` | `https://auth-testing.iduruguay.gub.uy/oidc/v1` |
   | `GUBUY_AUTHORIZATION_ENDPOINT` | Vacío explícito: usar discovery |
   | `GUBUY_CLIENT_ID` | `890192` |
   | `GUBUY_REDIRECT_URI` | `http://localhost:8080` **sin barra final ni ruta** |
   | `GUBUY_SCOPES` | `openid personal_info email document` |

   El secreto no vacío es obligatorio para este override: evita usar por omisión
   el secreto del simulador. El operador debe verificar que el valor sea vigente.

4. Comprobar sintaxis/interpolación sin mostrar los valores resueltos:

   ```sh
   docker compose -f docker-compose.yml -f docker-compose.gubuy-testing.yml config --quiet
   ```

   No ejecutar `config` sin `--quiet` para compartir su salida: mostraría secretos.

5. Con las imágenes actualizadas por el proceso autorizado del equipo y el resto
   del entorno disponible, el operador puede recrear el central sin construir:

   ```sh
   docker compose -f docker-compose.yml -f docker-compose.gubuy-testing.yml up -d --no-build --force-recreate central
   ```

   **No ejecutado por el agente.** Este comando no incorpora código nuevo a una
   imagen antigua; no demuestra funcionamiento. No basta reiniciar un contenedor
   para cambiar su entorno: hay que recrearlo. Mantener frontend en
   `http://localhost:3000` y central en `http://localhost:8080` para este recorrido.

El cliente obtiene authorization/token/JWKS/UserInfo de
`https://auth-testing.iduruguay.gub.uy/oidc/v1/.well-known/openid-configuration`.
No establecer manualmente la autorización del simulador en testing. Alternativa:
definir todas las variables comentadas en `.env.example` sin override; es más
fácil mezclar ambientes y el Compose base conserva defaults de desarrollo.

## Redirect y cookies

Las URIs registradas informadas son `http://localhost`, `http://localhost:8080` y
el callback del sitio externo de pruebas. Este recorrido usa solo
`http://localhost:8080`; no supone que el callback interno esté registrado.

El navegador inicia desde `http://localhost:3000/ingresar`, pasando por el proxy a
`/carga/api/auth/gubuy/iniciar`. El central fija su cookie HttpOnly de transacción.
Tras el ingreso, ID Uruguay devuelve `code`/`state` a la raíz del puerto 8080.
El filtro Undertow existente reenvía al callback interno conservando la query;
la cookie se envía al callback, que es la ruta donde corresponde, no a la raíz.
Las cookies no se aíslan por puerto: usar siempre el mismo host `localhost`,
sin alternar con `127.0.0.1`. Es un recorrido local HTTP, no una receta de producción.

## Validación manual pendiente

- [ ] Discovery accesible desde el central; endpoints y firma compatibles con el
      proveedor, TLS válido y acceso de red saliente. No desactivar PKCE ni TLS si falla.
- [ ] Al iniciar desde la SPA se abre ID Uruguay **testing**, nunca el simulador.
- [ ] Ingreso con cuenta válida de testing y consentimiento para los scopes requeridos.
- [ ] Vuelta exacta a raíz, redirección al callback y cookie de transacción disponible;
      no hay bucle, pérdida de `state` ni error de nonce.
- [ ] Canje y validación del `id_token`, UserInfo con `sub` coincidente y cédula válida;
      retorno a `/ingreso` y sesión propia de carga.uy. No copiar tokens ni datos personales.
- [ ] Primer ingreso/perfil y roles existentes; un funcionario solo recibe el rol
      si está habilitado internamente. Una identidad real no otorga permisos MTOP.
- [ ] PDI caída: ingreso permitido tras verificar la identidad, con DNIC pendiente.
      La PDI sigue siendo el simulador SOAP configurado; este cambio no integra PDI real.
- [ ] Cancelación, expiración y errores de identidad no crean una sesión.
- [ ] Cierre actual borra la sesión local, **no** la sesión de ID Uruguay. Un nuevo
      ingreso puede reutilizar la sesión del proveedor; no se implementa logout federado.
- [ ] Regresión del simulador local y de destino móvil, sin modificar sus contratos.

## Regresiones automatizadas pendientes

`ClienteOidcGubUyIntegracionTest` usa solo un proveedor HTTP local y claves
sintéticas. Incluye ID token sin datos personales, Bearer en header, endpoint
descubierto, coincidencia de `sub`, documento ausente, campos opcionales,
respuesta inválida/401/503, ausencia de access token, firma/nonce/emisor/audiencia/
caducidad inválidos y rechazo de redirecciones.

**Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
La comprobación estática no acredita ejecución, JTA, compatibilidad real ni
seguridad declarativa en WildFly. La aceptación de testing requiere evidencia
del operador; producción necesita credenciales, redirect HTTPS y despliegue propios.

### Comprobaciones estáticas realizadas

- `git diff --check` sin errores y enlaces locales afectados existentes.
- Compose base + override válido usando un secreto **sintético**, sin arrancar servicios.
- Valores fusionados de issuer, autorización por discovery, client ID, redirect
  y scopes comprobados sin mostrar la configuración completa ni secretos.
- El override rechaza un secreto vacío antes de iniciar contenedores.
- El Compose base conserva la selección del simulador. No se comprobó su login.

## Volver al simulador

Retirar de `.env` y del entorno del proceso el secreto real y las variables de
testing que se hayan definido, incluso si se utilizó el override recomendado.
De otro modo el Compose base podría enviar el secreto real al simulador o conservar
endpoints reales. Después usar solo el Compose base y recrear el central con la
imagen correspondiente. No basta omitir el override si esas variables siguen activas.
