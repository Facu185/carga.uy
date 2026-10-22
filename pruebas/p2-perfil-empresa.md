# F10 — Perfil propio de empresa: aceptación y evidencia

**Fecha:** 2026-10-22. **Acuerdo:** D0-05/C10 y ADR-0014, confirmado por el usuario.
**Estado:** pruebas Java y escenarios frontend definidos antes de la implementación;
backend/formulario preparados, ejecución y aceptación compartida pendientes.
No se ejecutan builds, JUnit, HTTP ni navegador desde esta tarea.

## Escenarios de regresión del servidor

Antes de implementar, escribir pruebas de consulta/modificación propia, empresa
ajena, vínculo ausente/revocado/no responsable, principal ausente, empresa
inactiva/inexistente, datos inválidos y número duplicado. Comprobar que solo cambian
los cuatro campos y que fechaAlta (incluso null), estado e ID se conservan.
Separar declaraciones de roles/rutas y decisiones con dobles de seguridad/JTA real.

## Escenarios de navegador definidos antes del formulario

1. Desde Mi empresa, abrir Perfil sin perder Choferes/Vehículos ni sus rutas.
   Comprobar sesión gub.uy existente y navegación por teclado, etiquetas y foco.
2. Cargar el perfil de la única empresa RESPONSABLE/ACTIVO; cero vínculos muestra
   error sin formulario. Ante más de uno, no escoger la primera empresa.
3. Guardar nombre, razón social, número y dirección, consultar de nuevo y recargar:
   deben verse datos persistidos. Estado/fechaAlta solo se muestran, no se editan.
4. Razón social/dirección nulas o vacías; nombre en blanco; número ausente,
   no positivo, fraccionario o duplicado; límites de longitud. Corregir errores
   sin perder la edición; cancelar recupera los datos de la última consulta.
5. API admite número de 64 bits. La SPA no debe redondear números fuera del rango
   entero seguro de JavaScript: debe impedir su edición y mostrar una limitación,
   no reenviar un número aproximado. No se cambia la regla global del backend.
6. Doble clic en Guardar envía una sola mutación mientras esté pendiente. Cambiar
   sesión o navegar durante GET/PUT no muestra datos/respuestas de la sesión previa.
7. Error de carga: estado propio y consulta manual. Un fallo de red/servidor al
   guardar puede tener resultado incierto: no reintentar automáticamente; bloquear
   nuevo guardado hasta consultar de nuevo. Distinguir ese caso de validación 400.
8. Sin sesión/vencida: 401 y flujo existente de reingreso. Vínculo revocado,
   empresa inactiva o ajena: 403 sin formulario operativo; comprobar REST directo,
   no solo botones. Empresa inexistente no revela datos adicionales.
9. Payload con id, fechaAlta, activa o vínculos añadidos nunca los modifica.
   Comprobar roles solo FUNCIONARIO/ADMINISTRADOR sin CIUDADANO/CHOFER: no obtienen
   este contrato; sus operaciones administrativas/JSF conservan permisos previos.
10. Con transacciones independientes verificar número duplicado concurrente y
    ausencia de parciales al revertir. Probar actualización frente a baja/revocación
    con el entorno real; las llamadas a dobles no acreditan esa carrera.

## Límites de ejecución

Pruebas Java escritas antes de la lógica:

- [PerfilEmpresaServicioBeanTest](../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/servicio/PerfilEmpresaServicioBeanTest.java):
  16 tests de decisiones/efectos con dobles, límites, campos, autorización,
  duplicado antes de mutar y error de unicidad al flush. El doble de DAO no
  ejecuta JPQL ni reproduce estados reales de vínculos; las anotaciones y la
  excepción declarada con rollback no acreditan seguridad ni atomicidad JTA.
- [EmpresaPerfilRecursoTest](../carga-web/src/test/java/uy/edu/fing/tse/carga/web/rest/EmpresaPerfilRecursoTest.java):
  cuatro tests de rutas, delegación y propagación a mappers. No ejecuta HTTP,
  JSON-B ni prueba el status 200/400/401/403 en un despliegue.

Antes del cambio no existían contrato, DTO ni métodos de perfil: las pruebas
referencian esos símbolos ausentes. No se ejecutó ese fallo ni un ciclo verde.

La SPA sigue sin script `test`; estos escenarios NO son pruebas automatizadas ni
ciclo TDD frontend verificado. JUnit necesita compilación y no se ejecuta.
El equipo valida en despliegue aislado y registra commit/cambios locales,
WildFly/PostgreSQL, alias sintéticos, entrada mínima, resultado real y evidencia
sin tokens/datos personales. No declarar cerrado F10 antes de esa aceptación.

**Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
No se modificaron P1/solicitudes, guías, tablas/migraciones, interfaces remotas,
autenticación JSF ni dependencias/POM/CI. Se conserva la entrada Choferes existente;
el perfil es una pestaña/ruta adicional y no una migración general de ADR-0004.

## Comprobaciones estáticas de esta entrega

Base de trabajo: `7844c1c`, más los cambios locales F10; no se creó commit ni
se comprobó un artefacto desplegado.

- `git diff --check`: sin errores.
- 21 archivos revisados como UTF-8/LF; 155 enlaces locales, dos anclas y 57 imports
  locales contrastados con destinos existentes.
- Comprobación textual del DTO y setters: solo nombre, razón social, número y
  dirección; 16+4 tests declarados y delimitadores Java balanceados.
- ABM administrativo, interfaces anteriores, página Choferes, guías,
  migraciones, POM, dependencias y CI sin cambios en esta tarea.

Nada de lo anterior valida tipos/compilación, serialización JSON-B, status HTTP,
seguridad EJB, rollback JTA, concurrencia o interacción visual. Esos resultados
siguen pendientes en los escenarios anteriores.
