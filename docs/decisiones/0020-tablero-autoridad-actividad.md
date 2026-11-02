# ADR-0020 — Tablero de autoridad por registro de guías

**Estado:** Aceptada para el MVP solicitado y confirmado por el usuario; aceptación integrada pendiente.
**Fecha:** 2026-11-02

## Contexto

La autoridad consulta reportes gerenciales agregados (GuiaCUCrit), no administra
usuarios ni resuelve casos. El backoffice SPA ya tiene sesión y auditoría, pero
su inicio no ofrece esos reportes. El usuario pidió aplicar la recomendación de
tablero de solo lectura y confirmó explícitamente **fecha de creación de la guía
y días calendario de America/Montevideo** para todos sus resultados.

## Decisión y alcance

- Ampliar la SPA existente solo con este tablero, reutilizando sesión, cliente
  REST y presentación. No aceptar globalmente ADR-0004 ni eliminar JSF.
- Acceso del nuevo reporte exclusivamente a AUTORIDAD, sin ampliar por herencia
  los permisos del administrador, funcionario, ciudadano o chofer. Conservar
  auditoría, cambio de contraseña y el inicio administrativo existentes.
- Consultar conteo de guías registradas, empresas distintas con esas guías y
  distribución por **estado actual persistido**, más evolución diaria y
  distribución por rubro de la guía. No usar el rubro actual de la empresa.
- Todos los resultados usan `guia.creada_en`: desde inclusivo a las 00:00 locales
  hasta el día posterior a hasta, exclusivo. No usar `guia.fecha` (derivada UTC)
  ni inicio/fin previstos. Incluir empresas inactivas y asignaciones ausentes.
- Ofrecer 30 días por defecto, incluido hoy según reloj del servidor; límites
  explícitos inclusivos en formato YYYY-MM-DD. Limitar cada consulta a 366 días
  como protección técnica del tamaño de la serie, sin truncar guías.
- Agregar en PostgreSQL mediante una única sentencia parametrizada para que
  totales y distribuciones compartan la misma instantánea de lectura. Completar
  días y estados sin registros con cero en el central; conservar guías sin rubro.
  No descargar guías individuales ni nombres/documentos de empresas o personas.
- Mostrar tablas equivalentes a los gráficos, error distinto de vacío y evitar
  que respuestas tardías sustituyan una consulta más reciente. No añadir librerías.

## Límites y responsabilidad

Los reportes siguen siendo responsabilidad funcional de P5 según `frentes.md`;
este bloque solicitado no amplía el cierre P2 ni acredita coordinación externa.
El contrato aditivo se publica en [contratos](../contratos.md#reporte-de-actividad-de-autoridad--adr-0020).
No incluye motor/casos/descargos, indicadores de cumplimiento, mapa en vivo,
exportaciones, histórico de estados ni volumen/peso efectivamente transportado.
Los estados son actuales al consultar, NO los estados al cierre del período.

## Alternativas y consecuencias

- JSF: reutiliza la tecnología existente, pero requiere otro recorrido de interfaz
  y sesión; se prefiere continuidad con el backoffice autenticado de P1.
- Agregar en el navegador: simplifica una primera maqueta, pero exige descargar
  registros, expone detalle innecesario y puede producir totales parciales.
- Varias consultas independientes: más simples aisladamente, pero READ COMMITTED
  puede devolver totales y distribuciones inconsistentes ante altas concurrentes.
- SQL específico de PostgreSQL: permite agregación temporal explícita y coherente;
  requiere validación real de SQL/JDBC/Hibernate. No se cambia esquema ni aislamiento.

## Validación

Pruebas escritas primero, sin ejecutar compilación ni ciclo rojo/verde.
Roles reales, JSON-B, SQL/JDBC/Hibernate y navegador quedan pendientes según
[matriz de aceptación](../../pruebas/tablero-autoridad.md).
