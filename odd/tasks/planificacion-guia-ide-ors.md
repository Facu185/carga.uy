# Vista previa de guía con IDE Uruguay y OpenRouteService

## Specs

- S1. Solicitud textual: «quiero implementar ese flujo, recuerda que quiero hacerlo
  tambien con ide uy para la sugerencia de calles, puedes copiar el mapa que ya se
  esta usando en el proyecto para que el responsable tenga guia visual de las guias
  que esta creando». Sugerencias de calles/portales de IDE, resolución de la
  selección, marcadores de origen/destino y recorrido ORS dentro de Registrar guía.
- S2. Alcance confirmado previamente: peso total del vehículo y carga declarada;
  SIN altura, ancho, largo ni carga por eje. El servidor obtiene la tara del
  vehículo autorizado y convierte kg a toneladas para driving-hgv. No inventar
  remolques ni certificar transitabilidad; límites cartográficos desconocidos.
- S3. Backend Jakarta como intermediario; ORS_API_KEY solo en entorno del central,
  nunca VITE_ ni navegador/logs. Endpoints nuevos de vista previa para ciudadano
  responsable activo de empresa activa; vehículo de esa empresa. No modificar
  permisos ni validaciones existentes de creación/catálogos, SOAP, tracking o P5.
- S4. Reutilizar Leaflet 1.9.4 y fondo OSM del panel existente sin copiar sus
  permisos ni flota. Resolver candidates antes de usar coordenadas (trae 0,0).
  Texto editado invalida punto; cambio de extremos/vehículo/peso invalida ruta.
  Autocompletado con espera y teclado, sin resultados obsoletos, errores distintos
  de vacío y sin HTML externo. Atribuciones visibles y alternativa textual.
- S5. Vista previa informativa: no persistir trazado ni cambiar contrato de guía,
  creación o fiscalización. Conservar texto y coordenadas opcionales/manuales;
  caída de IDE/ORS/mapa no agrega un bloqueo al registro ya existente. Revalidación
  de CU2 al registrar intacta. Sin alternativas/paradas ni navegación en tiempo real.
- S6. Límites de entrada/respuesta, timeouts, redirecciones desactivadas y circuitos
  separados; sin reintentos automáticos de proveedor que gasten cuota. Errores
  seguros 400/401/403/503 y formato previo. Endpoint ORS actual de HeiGIT.
- S7. Pruebas Java primero y escenarios SPA antes del código. No compilar,
  instalar, ejecutar runners que compilen, leer .env, hacer commits ni auditorías.
  Acreditar solo comprobaciones observadas; ejecución y aceptación real pendientes.

## Tasks

| ID | Specs | Ruta / responsabilidad | Estado | Evidencia |
| --- | --- | --- | --- | --- |
| T1 | S1–S7 | ADR-0024, contrato, matriz pruebas y tests Java | completada (escrita, no ejecutada) | 22 tests en 5 suites; matriz SPA previa; ciclo rojo/verde pendiente |
| T2 | S1–S3,S6 | carga-api, integración/servicio EJB y recurso REST | completada (sin ejecución) | 5 DTO, interfaz nueva, clientes separados, peso/autorización EJB y 3 endpoints aditivos |
| T3 | S1,S4,S5 | cliente SPA, selector IDE, mapa Leaflet y NuevaGuia | completada (sin ejecución) | Combobox/resolución, mapa y ruta con invalidación; escenarios de navegador pendientes |
| T4 | S3,S6,S7 | Compose/.env.example, documentación y validación estática | completada (estática) | YAML, 52 imports, 122 enlaces, UTF-8/LF/delimitadores y diff --check correctos; sin .env real |
| T5 | S4,S7 | MapaGuia.tsx y regresión R1 | completada (estática; navegador pendiente) | Política por TileLayer añadida; nginx global intacto por hash; R1 no ejecutada |

## Log

- Solicitud original transcrita en S1; autorización explícita para implementar el
  flujo explicado: IDE obtiene extremos, backend llama ORS con peso y clave,
  frontend presenta resultado. No acepta globalmente ADR-0004/0022.
- Base Git limpia. Leídas guías CODEX, contratos, CU2, frentes, estándares y skills.
- Panel JSF usa Leaflet 1.9.4/CDN y OSM. Se conserva intacto; solo se adapta mapa.
- Consulta pública IDE: candidates de Sucre y Av. 18 de Julio devuelve lat/lng=0;
  find de calle 8096 devuelve punto real. No se usa la clave ORS ni se lee .env.
- Endpoint actual verificado en anuncio oficial: api.heigit.org/openrouteservice/v2/directions.
- Estado inicial sin integración. Fallo esperado de tests: contratos/clases de
  planificación ausentes. Compilación y ciclo rojo/verde prohibidos.
- T1: contrato y ADR publicados antes del código; tests de proveedores, servicio,
  REST y transporte HTTP local escritos. No ejecutados; no acredita rojo/verde.
- T2: transporte con lectura limitada y sin redirecciones; circuitos separados.
  Selección IDE se resuelve por IDs; ORS recibe tara desde DB más carga en toneladas.
  EJB sin transacción remota, sin escrituras; seguridad específica de responsable,
  contrato anterior intacto. Primera comprobación `git diff --check` correcta.
- T3: cliente JSON validado, sugerencias con debounce/teclado/resolución y
  cancelación al editar incluso coordenadas manuales. Leaflet por CDN fijado y
  SRI, atribuciones y alternativa textual. ORS se calcula con espera; revisión de
  entradas impide restaurar rutas viejas aun al deshacer cambios. Sin runner SPA.
- Configuración: añadidas variables ORS_API_KEY/ORS_RUTAS_URL/IDE_DIRECCIONES_URL
  solo a central y ejemplo sin secreto. .env real intacto. Sin instalaciones.
- T4: 34 archivos de la tarea revisados estáticamente. 22 métodos @Test en 5
  suites, NINGUNO ejecutado. Se añadieron casos de JSON nulo/malformado, avisos ORS
  y perfil HGV antes de los ajustes correspondientes. Peso vacío/carga y orden
  lon/lat verificados solo por lectura y pruebas escritas; sin afirmación runtime.
- La lectura JPA del vehículo se materializa antes de llamar al proveedor, sin
  dejar un stream JDBC abierto durante HTTP. No hay transacción remota ni locks.
- Comprobaciones observadas: `git diff --check`, YAML sin expandir secretos,
  codificación/LF/espacios de nuevos y modificados, 52 imports y 122 enlaces
  locales existentes, delimitadores balanceados. Esto NO valida tipos ni ejecución.
  Creación CU2, panel JSF y package/lock de SPA comparados byte a byte con HEAD,
  intactos. Sin referencias a clave ORS en fuentes frontend.
- Entrega de código local concluida. Pendientes: compilación y ciclo rojo/verde,
  roles/JSON-B/HTTP reales en WildFly, navegador/teclado/respuestas tardías, cuota
  y clave ORS real, disponibilidad CDN/tiles y corredores uruguayos con límites
  conocidos; aceptación integrada P1/P3/P5. No se guarda ruta ni se cambia P5.
- T5 (2026-11-09): usuario pregunta por Access blocked y luego autoriza «lo puedes
  solucionar?». Se contrasta política OSM con nginx: same-origin elimina Referer
  cruzado, capa Leaflet sin override. Se conserva cabecera global y se limita la
  corrección al atributo de imágenes de tiles. R1 definido antes de modificar
  producción; no hay runner SPA ni encabezados de navegador capturados aún.
- T5: opción `referrerPolicy: 'strict-origin-when-cross-origin'` y su tipo DOM
  añadidos solo al TileLayer. `git diff --check`, lectura del diff puntual,
  UTF-8/LF/espacios/enlaces y opciones correctos estáticamente. Huellas previas
  verifican nginx, package/lock, cliente de API y Compose intactos. Sin builds,
  instalaciones, lectura de .env, tests automatizados ni prueba real del navegador.
  Para confirmar el desbloqueo falta desplegar frontend actualizado y ejecutar R1:
  Referer solo origen y fondo válido. No se garantiza resolución de otro bloqueo
  de proveedor, caché o extensión de privacidad ajeno a esta incompatibilidad.
