# Comprobación de la corrección del ABM JSF P2

## Estado de la evidencia

Se escribieron regresiones de coordinación HTTP, autenticación y filtrado de la vista.
**No se ejecutaron:** el runner Maven compila y el agente tiene prohibido ejecutar
builds. Las verificaciones estáticas no acreditan funcionamiento del contenedor.

Antes de validar, el equipo debe desplegar los cambios con su procedimiento habitual.
No atribuir resultados de una versión previa del EAR a esta corrección.

## Entrada y alcance

Abrir `/carga/ingreso-jsf.xhtml` en el origen del central (por defecto local,
`http://localhost:8080/carga/ingreso-jsf.xhtml`; confirmar el puerto real del equipo).
Usar una **cuenta interna existente de administrador**, no credenciales inventadas.
El formulario admite base de datos o LDAP según la configuración de P1. Si la clave
es temporal, primero pasa por `/carga/cambiar-clave-jsf.xhtml`.

Los enlaces de `/carga/index.xhtml` siguen mostrando las tres pantallas; sin sesión
redirigen al ingreso, sin iniciar los servicios del backing bean.

La SPA y JSF no comparten automáticamente el login. El flujo OIDC no se modifica:
funcionarios/ciudadanos siguen entrando al portal gub.uy; incorporar un destino JSF
para ellos requiere coordinación con P1, no cambiar roles ni crear cuentas internas.

## Regresiones escritas

- `MecanismoJwtTest`: petición sin sesión, cookie válida/vencida, Bearer y precedencia,
  cookie ignorada fuera de JSF P2, clave temporal, rol insuficiente, origen de POST
  y respuesta de redirección AJAX. No ejecuta el contenedor.
- `SesionJsfServletTest`: login delegado a P1, atributos de cookie, HTTPS, errores
  esperados, cambio de clave, cierre y ausencia de HttpSession de autenticación.
- `ChoferVistaTest`: administrador consulta global; responsable consulta su empresa;
  vínculos de chofer/revocados no habilitan gestión; sin vínculo no consulta;
  un filtro ajeno limpia el listado. No acredita autorización EJB real.

## Escenarios pendientes en WildFly y navegador

1. Sin cookie ni Bearer, abrir empresas, vehículos y choferes: redirección al ingreso,
   sin `EJBAccessException` durante `@PostConstruct`. Confirmar en logs el origen
   del 500 anterior, sin copiar credenciales, cookies ni datos personales.
2. Login inválido/bloqueado/LDAP no disponible: mensaje seguro, sin nueva cookie de
   acceso y sin posibilidad de ejecutar el ABM.
3. Login de administrador: cargar las tres vistas, agregar/modificar/dar de baja;
   chofer permite reactivar. Guardar una fila no debe procesar los campos de otra
   ni los del formulario de alta. Confirmar cambios persistidos recargando.
4. Clave temporal: no se ejecuta ABM; clave nueva inválida no reemplaza token;
   cambio válido permite el ABM e invalida sesiones previas conforme a P1.
5. Sesión vencida/revocada: rechazar tanto navegación como postback/AJAX antes del
   bean. AJAX debe navegar al ingreso, no intentar interpretar una página HTML
   como XML. Probar también un Bearer inválido junto a cookie válida: no rescatarlo.
6. Rol autoridad: lectura de empresas, sin formulario de alta ni acciones de escritura;
   acceso directo a vehículos/choferes devuelve 403. Verificar también llamadas EJB.
7. Mediante identidad de prueba autorizada del contenedor, verificar ciudadano/chofer
   con vínculo RESPONSABLE ACTIVO: filtro por su empresa, nunca consulta global;
   empresa ajena, vínculo revocado y empresa inactiva se rechazan en el servicio.
   Este escenario no afirma que se haya agregado ingreso OIDC a JSF.
8. POST de origen ajeno o sin Origin/Referer: 403, incluidos login, cambio, salida y
   acciones AJAX. Probar que falsificar IDs en el cliente no altera permisos del EJB.
9. Cookie en `/api/empresas` sin Bearer: continúa sin autenticar (401 por el mapper
   REST existente), no autorizar por cookie. REST autenticado con Bearer conserva
   comportamiento y códigos. SOAP y panel P4 no consumen esta cookie.
10. Cerrar sesión JSF: cookie borrada con el mismo path, vistas descartadas; recargar
    una página protegida requiere ingreso. No se promete revocación global.

## Despliegue

- HTTPS fuera del entorno local. Si hay proxy TLS, WildFly debe reconocer correctamente
  esquema/host/puerto externos y `isSecure`; comprobar cookie Secure y comparación de
  origen, sin confiar en encabezados forwarded arbitrarios.
- Navegadores/proxies que eliminen tanto Origin como Referer no pueden realizar
  operaciones JSF con esta protección; no desactivarla para ocultar un problema de proxy.
- No se modifican Docker/Compose, TTL del JWT, permisos de servicios ni destino OIDC.
