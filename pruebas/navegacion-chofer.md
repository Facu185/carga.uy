# Navegación superior del chofer

**Fecha:** 2026-10-23. **Alcance:** presentación de la SPA existente (P2),
conservando sesión de P1/P3, consultas y descarga CSV de guías propias.
No incorpora operaciones de eventos ni modifica permisos o contratos.

**Actualización 2026-11-13:** los escenarios 3 y 5 se ajustan a la exclusión
confirmada en [ADR-0025](../docs/decisiones/0025-incompatibilidad-chofer-responsable.md).

## Escenarios definidos antes del cambio

1. Un CHOFER sin responsabilidad empresarial ve Mi cuenta / Mis guías antes
   del saludo en Mi cuenta. No aparece la tarjeta redundante Tus guías.
   Tus datos, verificación DNIC y salida de sesión se conservan.
2. Mis guías abre `/mis-guias`; aparece la misma barra sobre el encabezado con
   Mis guías como único enlace activo. Mi cuenta vuelve mediante navegación SPA.
   Recarga directa y atrás/adelante conservan sesión y destinos existentes.
3. Un CHOFER con vínculo RESPONSABLE/ACTIVO heredado ve la barra Mi cuenta /
   Mis guías, no la empresarial ni la solicitud de responsabilidad. El servidor
   deniega sus funciones de responsable; conserva vínculos e historial y el
   acceso a sus guías propias hasta resolver la incompatibilidad administrativamente.
4. Un FUNCIONARIO que también sea CHOFER conserva únicamente su barra operativa
   en Mi cuenta. No se modifican rutas, roles acumulativos ni permisos del servidor.
5. Mientras se consultan vínculos, ante fallo, al reintentar y sin vínculo activo,
   el chofer no pierde Mis guías. No aparece la tarjeta, botón ni formulario de
   responsabilidad, aunque tenga solicitud pendiente. El acceso funcionario
   conserva sus condiciones, sin una columna vacía reservada a la tarjeta eliminada.
6. Un CIUDADANO sin CHOFER mantiene Registrar guía, solicitudes y datos. El
   responsable sin CHOFER conserva la navegación empresarial sin Mis guías.
7. Mis guías mantiene carga, tabla de datos, vacío, error y descarga de
   `mis-guias.csv` mediante los endpoints existentes. No agrega consultas de
   vínculos, datos de otros choferes, reintentos automáticos ni llamadas nuevas.
8. Los enlaces tienen foco visible, son operables con teclado y NavLink marca
   la URL actual mediante aria-current. Comprobar 320 px/zoom 200 %: la barra
   conserva desplazamiento horizontal y no pierde destinos.
9. Sin sesión o con token vencido, el ingreso/401/403 sigue el flujo existente;
   abrir directamente rutas no concede acceso a guías ajenas o empresariales.

## Estado de validación

`carga-front/package.json` no tiene script `test` ni runner configurado.
Estos escenarios son de aceptación manual, NO pruebas automatizadas. No se
ejecutan builds, `tsc`, linters, navegador ni HTTP; no se instala un runner.
**Ciclo rojo/verde no verificado.** Pendiente comprobar en el entorno del equipo
los estados, teclado, móvil y sesiones indicados, incluida la descarga real.

## Comprobaciones estáticas realizadas

- `git diff --check`: sin errores.
- Comprobación textual con Python estándar: 25 imports locales de las cuatro
  fuentes implicadas resuelven a archivos existentes; los nueve enlaces entre
  ambas barras (siete empresariales, incluido Mis guías condicional, y dos del
  chofer) tienen rutas en `App.tsx`.
- Comparación textual con HEAD: `MisGuias.tsx` solo cambia el import y la barra;
  las consultas, descarga y estados restantes conservan el texto fuente anterior.
- `package.json` parseado como JSON, sin script `test`.

Lo anterior no valida tipos, JSX, renderizado, HTTP, descarga real ni permisos.
Se preservan los cambios de navegación del responsable todavía sin commit.
