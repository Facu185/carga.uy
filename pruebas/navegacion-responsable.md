# Navegación superior del responsable de empresa

**Fecha:** 2026-10-22. **Alcance:** presentación de la SPA existente (P2),
reutilizando navegación empresarial y sesión de P1/P3. No cambia contratos,
permisos, solicitudes ni reglas de guías.

## Escenarios definidos antes del cambio

1. En Mi cuenta, un CIUDADANO con vínculo RESPONSABLE/ACTIVO ve la misma barra
   superior y subrayado activo del funcionario: Mi cuenta, Perfil de empresa,
   Choferes, Vehículos, Guías de viajes y Registrar guía. No aparecen las tarjetas
   redundantes de registro, administración y consulta empresarial. Tus datos,
   verificación DNIC y las solicitudes existentes se conservan.
2. Cada enlace lleva a la ruta existente; recargar directamente Perfil, Choferes,
   Vehículos, Guías de viajes o Registrar guía conserva la barra. Una sola sección
   queda activa según la URL. Atrás/adelante del navegador funciona sin cerrar
   sesión. No se crean rutas ni selecciones de empresa nuevas.
3. Un CHOFER responsable conserva Mis guías en la barra empresarial de Mi cuenta
   y acceso a todas las secciones empresariales, sin tarjeta personal redundante
   (ampliación solicitada en [navegación del chofer](navegacion-chofer.md)).
   Registrar guía solo aparece si también tiene
   CIUDADANO, igual que la protección actual de esa ruta. Mis guías no se sustituye
   por la consulta empresarial ni se cambia la asignación por cédula.
4. Sin vínculo responsable activo, un ciudadano conserva Registrar guía y un
   chofer navega mediante Mi cuenta / Mis guías. Ambos conservan sus demás
   acciones, solicitud de responsabilidad (incluido estado pendiente),
   solicitud de acceso funcionario, carga/error de vínculos y reintento. No se
   muestra la barra empresarial en Mi cuenta ni en Registrar guía.
5. En Registrar guía, la consulta adicional de vínculo no bloquea ni modifica
   catálogos, formulario, POST, error o confirmación. Si falla, se mantiene Volver
   a Mi cuenta; una respuesta tardía tras salir de la página se descarta.
6. El FUNCIONARIO conserva únicamente su navegación operativa en Mi cuenta,
   incluso si un vínculo heredado es responsable. Las rutas protegidas, Bearer,
   expiración/401/403 y autorizaciones del servidor permanecen sin cambios.
7. Con teclado se recorren enlaces normales con foco visible y el enlace actual
   expone aria-current de NavLink. A 320 px/zoom 200 %, las pestañas se desplazan
   horizontalmente sin ocultar destinos ni desbordar la página.
8. En Perfil/Choferes/Vehículos/Guías, carga, vacío, error, reintentos y operaciones
   existentes funcionan igual; abrir una ruta empresarial sin autorización no
   concede acceso a recursos del servidor.

## Estado de validación

La SPA no tiene script `test` ni runner configurado en `package.json`. Estos son
escenarios de aceptación manual, NO pruebas automatizadas. No se instala un
runner ni se ejecutan builds, `tsc`, navegador o HTTP en esta tarea.

**Ciclo rojo/verde no verificado.** Pendiente ejecutar los escenarios en el
entorno del equipo, registrar resultados y comprobar visualmente escritorio,
móvil, teclado y sesiones con los distintos roles/vínculos.

## Comprobaciones estáticas realizadas

- `git diff --check`: sin errores.
- Comprobación inicial con Python estándar: 31 imports locales de los cuatro
  TSX modificados resuelven a archivos existentes; los seis enlaces de la barra
  tienen rutas en `App.tsx`; las seis pantallas empresariales incluyen la barra.
  La ampliación posterior para chofer añade Mis guías condicional y registra
  sus comprobaciones en [navegación del chofer](navegacion-chofer.md).
- `package.json` parseado como JSON; confirmado que no tiene script `test`.

Estas comprobaciones no validan tipos, sintaxis JSX, renderizado, foco,
peticiones HTTP ni autorización en ejecución. No se modifican el router,
autenticación, cliente REST, servidores, dependencias ni CSS compartido.
