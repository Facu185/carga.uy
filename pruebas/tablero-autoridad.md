# Aceptación pendiente — Tablero de autoridad (ADR-0020)

**Estado:** escenarios definidos antes del código; no ejecutados. No hay runner
frontend en package.json; no se instalaron herramientas ni se ejecutaron builds.
Las pruebas Java tampoco acreditan ciclo rojo/verde, roles EJB ni HTTP reales.

## Backend y contrato

1. AUTORIDAD obtiene 200 sin vínculo empresarial. Sin sesión 401; ciudadano,
   chofer, funcionario y administrador 403, antes de leer guías. Token vencido y
   clave temporal conservan comportamiento P1. Verificar atravesando WildFly.
2. Sin límites, 30 días incluido hoy local; reloj fijo en 2026-11-02T01:00Z
   corresponde aún al 1 de noviembre en Montevideo.
3. Rango de un día válido y de 366 días válido; 367, invertido, imposible,
   espacios, año cero, fechas con hora y par incompleto: 400 seguro.
4. Inclusión exacta a medianoche local, exclusión de medianoche siguiente,
   incluyendo instantes que en UTC caen en el día siguiente. La agrupación no
   depende de la zona del navegador, JVM o sesión PostgreSQL.
5. Guías creadas dentro con inicio previsto fuera se incluyen; guías creadas
   fuera con inicio previsto dentro se excluyen. No usar columna heredada fecha.
6. Misma empresa con varias guías cuenta una vez; incluir empresa inactiva,
   guía sin asignaciones y sin rubro. Estados actuales, no reconstruidos.
7. Totales, estados, rubros y evolución coinciden; faltantes diarios/estados son
   cero. Vacío real devuelve 200 y cero; error de base NO devuelve cero.
8. Catálogo con rubro llamado «Sin rubro» y guía sin rubro son grupos distintos.
   Orden estable y sin truncado de guías. Verificar SQL/JDBC/Hibernate reales.
9. Única sentencia de agregación; sin escrituras ni locks explícitos. Altas o
   cambios de estado concurrentes no mezclan instantáneas entre distribuciones.
10. JSON-B mantiene nombres/enteros/arreglos y no serializa entidades; errores
    `{error:mensaje}`; respuesta no almacenada (`Cache-Control: no-store`).
11. Medir plan y latencia con volumen representativo antes de decidir índices.
    El límite temporal limita la serie, no garantiza costo constante de consulta.

## Navegador

1. Autoridad entra a `/backoffice` y ve reporte; navegación a auditoría y cambio
   de clave intactos. Administrador conserva inicio/usuarios/nodos existentes;
   otros roles no acceden al reporte ni por URL directa.
2. Carga, éxito, vacío real, 400, 403, 401, 500, red caída y recuperación manual:
   fallos sin tarjetas numéricas engañosas ni datos anteriores bajo filtros nuevos.
3. Cambiar/aplicar rápidamente filtros y retrasar respuestas: solo gana la última
   consulta; desmontar pantalla cancela/ignora respuestas. Reintento vuelve a
   consultar el período aplicado, no un borrador sin enviar.
4. Mostrar período aplicado distinto del borrador y advertencia de estado actual.
   30 días iniciales vienen del servidor; aplicar otro rango y recargar lo conserva
   en URL. Atrás/adelante restaura consulta y campos.
5. Cada gráfico tiene tabla con los mismos datos, caption y encabezados; teclado,
   foco en error de formulario, lector de pantalla y móvil sin depender del color.
6. JSON inválido, conteo negativo/no seguro, grupos repetidos, días omitidos,
   totales incoherentes, período/zona/criterio incorrectos: mostrar error, no gráfico.
7. Sin llamadas a monitor/nodos, sincronización, eventos individuales ni nuevos
   permisos para consultar detalle de empresa/guía.

## Evidencia por registrar

Resultados de pruebas Java, PostgreSQL/Hibernate y contenedor; capturas y recorrido
de teclado/navegador; aceptación de P1/P5. No marcar cerrado por presencia de código.

## Entrega local y comprobaciones realizadas (2026-11-02)

- 20 pruebas Java escritas: 12 de servicio (autorización explícita, calendario,
  límites, vacío/fallo y completado), 3 de DAO (delegación/mapeo), 3 de recurso
  (delegación/cabecera/errores) y 2 de SQL/JDBC con Testcontainers. Ninguna fue
  compilada ni ejecutada; fixture SQL mínimo no sustituye migraciones/Hibernate.
- `git diff --check` sin incidencias. Revisión estática adicional de archivos
  nuevos/modificados: espacios finales/LF, existencia de enlaces locales Markdown
  e imports Java del proyecto. Ejemplo JSON del contrato parseado y sus sumas
  contrastadas. Esto NO valida sintaxis Java/TypeScript/SQL ni comportamiento.
- Contraste de fechas históricas de fixtures con tzdata del entorno: referencia
  auxiliar, no ejecución de las pruebas Java ni verificación del runtime destino.
- No hay `node_modules` ni ejecutable `oxlint` disponible; no se instaló nada.
  No se ejecutaron linters, `tsc`, Maven, Docker ni navegador.
- **Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
