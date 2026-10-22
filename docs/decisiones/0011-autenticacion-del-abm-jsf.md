# ADR-0011 — Conservar el ABM JSF con la identidad JWT existente

**Estado:** Aceptada para la corrección P2 solicitada por su responsable
**Fecha:** 2026-10-21

## Contexto

Las vistas `empresas.xhtml`, `vehiculos.xhtml` y `choferes.xhtml` invocan servicios
protegidos durante `@PostConstruct`. La navegación del navegador no lleva el
encabezado Bearer que sí agrega la SPA. Una denegación durante esa inicialización
puede impedir construir la vista; falta confirmar el 500 reportado con logs de WildFly.

El responsable P2 solicitó conservar estas vistas e integrar autenticación, sin
quitar `@RolesAllowed`. La SPA no tiene todavía reemplazo completo del ABM.
Esta decisión acotada NO acepta la migración de ADR-0004 ni cambia los roles de negocio.

## Decisión

- Reutilizar `AutenticacionLocal.login` y `CuentaLocal.cambiarMiPassword` para las
  **cuentas internas**. No crear usuarios, roles, contraseñas ni validadores JWT nuevos.
- Transportar ese JWT en una cookie de navegador `cargauy_jsf`, HttpOnly, SameSite
  Strict, sin Domain, con path del contexto. Secure depende de `request.isSecure()`;
  HTTPS y su reconocimiento por WildFly son obligatorios en un despliegue no local.
  No se confía directamente en encabezados forwarded proporcionados por el cliente.
- Consumir la cookie **solo** en las rutas P2 enumeradas en `SesionJsf`: no autentica
  REST, SOAP, recursos estáticos ni el panel P4. Mantener Bearer para los consumidores
  existentes y dar precedencia al encabezado cuando está presente.
- Validar vigencia y revocación mediante P1 en cada petición. No crear HttpSession
  para autenticar, no renovar automáticamente ni cambiar el TTL. JSF puede conservar
  su estado de vistas habitual; se descarta al ingresar, cambiar clave o salir.
- Comprobar origen exacto (esquema/host/puerto) de operaciones no seguras, incluido
  login, usando Origin y, solo si falta, Referer. Sin ambos se rechaza; SameSite no
  sustituye este control. Deshabilitar caché y enmarcado en estas rutas.
- Antes de construir una vista, redirigir al ingreso si falta sesión, al cambio si
  la clave es temporal y rechazar con 403 un rol insuficiente. Para AJAX usar la
  redirección XML de Faces. Los EJB siguen siendo la autoridad de negocio.
- Conservar los roles actuales: empresas permite lectura con los cinco roles y
  escritura solo a administrador/funcionario; vehículos y choferes requieren
  administrador/ciudadano/chofer. No ampliar permisos de funcionarios por esta corrección.
- En choferes, el no-administrador selecciona únicamente empresas con vínculo
  RESPONSABLE ACTIVO. El servicio vuelve a validar la pertenencia; filtrar la UI
  no sustituye esa comprobación.

## Alternativas consideradas

- **Retirar roles o usar RunAs:** descartado; escondería la incompatibilidad
  habilitando operaciones sin la identidad real del usuario.
- **Migrar el ABM a React:** requiere completar pantallas y acordar ADR-0004. Se
  mantiene fuera de esta corrección por elección del responsable P2.
- **Guardar el login en HttpSession:** agrega estado de autenticación y una forma
  distinta de vigencia; el adaptador conserva el JWT validado por P1.

## Consecuencias y límites

- Se agrega un adaptador web; contratos EJB, permisos, REST, SOAP y JMS no cambian.
  Coordinar su integración con P1 por tocar el mecanismo web compartido.
- El formulario nuevo permite ingresar al administrador con su cuenta interna
  existente (base o LDAP). Autoridad puede leer empresas, no gestionar vehículos/choferes.
- **No se agrega un destino OIDC JSF.** Ciudadanos y funcionarios mantienen el
  portal gub.uy existente; su acceso interactivo a JSF requeriría coordinar un
  puente/destino OIDC con P1. No introducir cuentas internas para eludir esa identidad.
- Salir borra la cookie y las vistas JSF; no revoca globalmente el JWT ni cierra
  la SPA. La expiración/revocación siguen bajo P1.
- Pruebas unitarias con dobles no demuestran roles/JASPIC/CDI, serialización de
  vistas, AJAX ni seguridad real del navegador. Validación en WildFly pendiente,
  sin builds ejecutados por el agente. Ver [guía de comprobación](../pruebas-abm-jsf.md).

## Referencia técnica

La cookie usa la [API Cookie de Servlet 6.1](https://jakarta.ee/specifications/servlet/6.1/apidocs/jakarta.servlet/jakarta/servlet/http/Cookie.html),
incluido `setAttribute` para SameSite. No se añade una biblioteca de autenticación.
