# Aceptación: sugerencias IDE, ruta ORS y mapa de nueva guía

Escenarios definidos ANTES del código. No hay runner SPA instalado; esta matriz
no es una suite ejecutable ni evidencia de ejecución. No ejecutar builds.

## Configuración y uso local

- Guardar `ORS_API_KEY` en `.env` raíz sin compartir su valor. No usar prefijo
  `VITE_`; no copiarlo a `.env.example` ni a capturas/logs.
- Compose entrega esa variable SOLO a `central`. URLs predeterminadas:
  `IDE_DIRECCIONES_URL=https://direcciones.ide.uy/api/v1/geocode` y
  `ORS_RUTAS_URL=https://api.heigit.org/openrouteservice/v2/directions/driving-hgv/geojson`.
  La URL ORS debe terminar en `/driving-hgv/geojson`; nunca cambiar a driving-car
  como fallback. Sin clave, IDE sigue disponible y solo falla el cálculo ORS.
- Abrir `/guias/nueva` con ciudadano responsable activo. Escribir calle/número,
  localidad y departamento; seleccionar en ambos campos. El cálculo espera 700 ms
  con extremos, vehículo y carga válidos; sugerencias esperan 350 ms desde 3 letras.
- La entrega es código fuente: para verlo el equipo debe desplegar versiones
  actualizadas de central y frontend. Agregar una variable o reiniciar un
  contenedor viejo no actualiza código ni incorpora nuevas variables de Compose.
  No se ejecutó despliegue ni build desde el agente.
- Para producción, gestionar el secreto con el mecanismo del entorno, controlar
  cuota ORS y revisar condiciones de CDN/tiles. La configuración por variable de
  entorno sigue la convención local existente; no se instaló gestor de secretos.
- Las sugerencias actuales cubren calles, portales y esquinas. Direcciones rurales
  u otros tipos pueden ingresarse como texto/coordenadas manuales, sin afirmar que
  IDE los haya resuelto.

Fuentes externas consultadas: [IDE](https://direcciones.ide.uy/swagger-ui.html),
[opciones ORS](https://giscience.github.io/openrouteservice/api-reference/endpoints/directions/routing-options),
[migración a HeiGIT](https://ask.openrouteservice.org/t/deprecating-api-openrouteservice-org-in-favour-of-api-heigit-org/7912),
[política de tiles OSM](https://operations.osmfoundation.org/policies/tiles/).

## Java (JUnit, ejecución pendiente)

- IDE: normalizar candidates, limitar resultados; excluir localidades/POI; nunca
  usar sus 0,0. Resolver por identificadores/portal/esquina, conservar orden lat/lon,
  aproximación de calle y mensajes; rechazo de punto ausente/0,0/respuesta inválida.
- ORS: dos puntos en orden lon/lat, driving-hgv, vehicle_type=hgv, peso total en
  toneladas; SIN dimensiones/ejes. Geometría/summary válidos; sin ruta, cuota,
  token ausente, timeout, redirección y JSON incorrecto con errores seguros.
- Servicio: principal, vínculo responsable activo, empresa activa y vehículo
  propio antes de llamada externa. Tara desde DB, no navegador; carga positiva y
  no mayor a capacidad. Ninguna escritura ni nueva autorización de CU2.
- REST: rutas nuevas, delegación y Cache-Control:no-store, excepciones intactas.
- Transporte: timeout, límite de respuesta y no seguimiento de redirección;
  clave solo en Authorization al destino ORS configurado; nunca en query/log.

## Navegador / integración (TODO pendiente)

1. Responsable activo: abrir `/guias/nueva`, mapa Uruguay y campos etiquetados.
2. Escribir menos de 3 caracteres: no pedir sugerencias. Escribir una calle con
   localidad/departamento: mostrar carga, lista, vacío o error diferenciados.
3. Elegir con ratón y con flechas/Enter; Escape cierra sin enviar formulario.
   Resolver calle/número antes del marcador; ubicación aproximada identificada.
4. Una selección que termina tarde no sobrescribe texto nuevo ni punto manual.
   Ningún resultado de una consulta anterior se ofrece como actual.
5. Seleccionar extremos, vehículo y carga positiva: cálculo tras espera breve,
   línea vial, distancia, duración estimada y peso total que el servidor consideró.
   Sin línea recta ficticia. Atribuciones OSM/IDE/ORS visibles.
6. Cambiar cualquier extremo/texto/vehículo/peso: retirar ruta anterior de inmediato;
   respuestas tardías no la restauran. Coordenadas 0,0/inválidas no se consultan.
7. Falta clave ORS, cuota, caída o respuesta sin ruta: mostrar aviso y reintento
   explícito, conservar formulario y marcadores; registro CU2 no agrega bloqueo.
8. CDN/mapa caído: aviso y alternativa textual; coordenadas y formulario utilizables.
9. Crear guía: mismo POST anterior con textos/coordenadas seleccionados; reglas
   de pertenencia, fechas, capacidad y solapamiento intactas. Limpiar mapa al éxito.
10. Sin sesión 401; otro rol/vínculo/empresa/vehículo ajeno 403 antes del proveedor.
    Probar en WildFly; dobles JUnit NO acreditan interceptores ni seguridad real.
11. DevTools/red/logs: ninguna API key ORS, ni retorno crudo del proveedor; solo
    llamadas autenticadas al central y tiles/CDN públicos. Secretos sin versionar.
12. Verificar recorridos con límites de peso conocidos en Uruguay. Datos ausentes
    NO equivalen a vía apta. No certificar cumplimiento ni modificar fiscalización.

## Regresión R1 — Referer de tiles OSM (2026-11-09)

Escenario añadido ANTES de corregir la capa. Captura del usuario: fondo con
`Access blocked`/403 y ruta/marcadores visibles. Configuración observada:
`nginx.conf` fija `Referrer-Policy: same-origin`; la capa no configura política
propia, por lo que suprime Referer al solicitar imágenes a otro origen. No se
capturaron todavía encabezados del navegador ni se descartan otros bloqueos OSM.

1. Conservar la cabecera global `same-origin`, URL HTTPS de tiles, atribución,
   caché normal del navegador y código de cálculo de ruta. No falsificar User-Agent,
   cambiar de host para eludir bloqueos ni desactivar caché.
2. Esperado tras el cambio: las imágenes `.leaflet-tile` tienen
   `referrerpolicy="strict-origin-when-cross-origin"`. El pedido HTTPS a
   `tile.openstreetmap.org` lleva Referer con SOLO el origen del sitio, sin
   `/guias/nueva`, parámetros, tokens ni clave ORS.
3. En el navegador con el frontend actualizado, revisar una solicitud en Network:
   encabezado Referer de origen y respuesta de imagen cartográfica válida; comprobar
   fondo visible, atribución, ruta y marcadores conservados al desplazar/zoom.
   No dar por resuelto el bloqueo externo solo por presencia de la opción.
4. Si continúa 403 con Referer correcto, investigar respuesta/política, caché y
   extensiones de privacidad sin relajar controles globales ni prometer desbloqueo.

Estado: pendiente de ejecución real. No hay runner frontend; esta regresión es
un escenario manual, no un test automatizado ni ciclo rojo/verde verificado.
Referencia: [opción Leaflet 1.9.4](https://leafletjs.com/reference-1.9.4.html#tilelayer-referrerpolicy).

## Regresión R2 — Portales ambiguos o sin ubicación (2026-11-13)

Escenarios añadidos antes del cambio. Las consultas públicas observadas no son
fixtures estables: `candidates` ofrece ambos portales, pero `find` devuelve cuatro
puntos para Camino Carrasco 5286 (idCalle 8071) y ninguno para Avenida 30 Metros 18,
Marindia (idCalle 57693). JUnit usa respuestas sintéticas, sin red ni credenciales.

1. Elegir una sugerencia cuya resolución devuelve varios puntos: mostrar todas
   las alternativas con dirección, coordenadas y advertencia de aproximación.
   No seleccionar ninguna ni calcular la ruta hasta elegir explícitamente.
   Elegir una alternativa con ratón o Tab/Enter: guardar SOLO ese punto y mostrarlo.
2. Una única ubicación conserva la selección habitual. Una calle sin portal sigue
   identificada como aproximada. No retirar las advertencias de IDE.
3. Portal sin resultados: mantener un aviso específico y el texto ingresado,
   sin marcador inventado. Usar el control existente de coordenadas manuales.
   Un centro de calle devuelto para un portal no se ofrece como ese portal.
4. Un fallo técnico mantiene el mensaje y el reintento explícito: esperar más de
   350 ms y sacar el foco NO debe sustituirlo por sugerencias o ayuda genérica.
5. Editar texto o coordenadas manuales elimina alternativas anteriores. Si la
   respuesta llega después de editar, no restaura opciones ni sobrescribe el punto.
6. El endpoint anterior `/direccion` conserva su DTO único y el rechazo de cero o
   múltiples puntos; `/direccion/alternativas` devuelve una lista sin elegir.
   Permisos y `Cache-Control: no-store` se conservan; errores técnicos no son `[]`.

Estado: pruebas Java escritas y escenarios SPA definidos; ejecución pendiente.
El runner Java compila y no hay runner SPA: ciclo rojo/verde no ejecutado por
restricción de builds. El cambio de fuente requiere desplegar central y frontend
actualizados; reiniciar las imágenes anteriores no incorpora esta corrección.
