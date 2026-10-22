---
name: jpa-flyway-postgis
description: >-
  Diseña cambios de persistencia de carga.uy con JPA, Flyway y PostgreSQL/PostGIS.
  Usar al modificar entidades, consultas, migraciones, índices, restricciones,
  asignaciones temporales o recorridos. No aplica migraciones ni habilita DDL automático.
---

# Persistencia de carga.uy

## Contexto

Lee [AGENTS.md](../../../AGENTS.md), [CODEX](../../../CODEX/README.md),
[propiedad de tablas y contratos](../../../docs/contratos.md) y
[ADR-0001](../../../docs/decisiones/0001-flyway-para-el-esquema.md).
Identifica consumidores de la tabla, no solo el servicio que la escribe.

## Procedimiento

1. Contrasta entidad, migraciones, consultas y
   [persistence.xml](../../../carga-ejb/src/main/resources/META-INF/persistence.xml).
   Conserva JTA y DDL automático deshabilitado. Lee el directorio de migraciones
   antes de elegir el próximo número; no fijes en esta skill una versión final.
2. Crea una migración nueva `V<n>__descripcion.sql`; no edites una aplicada,
   borres historial ni uses `repair`/`clean` como arreglo. Considera filas existentes,
   nulos, valores por defecto, unicidad y compatibilidad con el código desplegado.
   Cambios compartidos o destructivos requieren coordinación, no ejecución implícita.
3. Modela la garantía en el nivel apropiado: validación de negocio en servicio y
   restricciones atómicas en base cuando proceda. Para concurrencia, sigue la
   transacción y orden de locks: un `exists` previo no evita una carrera.
   No cambies aislamiento ni agregues bloqueos globales como solución automática.
4. Para guías conserva intervalos `[inicio, fin)`, vehículo/chofer y límites de
   autorización actuales. Para pesajes conserva `guia_id` nulo si no existe guía
   vigente: esa fila es evidencia, no un error descartable. Consulta las advertencias
   de [contexto](../../../CODEX/contexto-proyecto.md) antes de usar ADR-0005.
5. Parametriza JPQL/SQL y revisa límites, orden estable y acceso a relaciones antes
   de serializar DTO. Propón índices según filtros/orden y volumen del caso, no
   por cada columna; no afirmes mejora de rendimiento sin medición.
6. Si hay geodatos, verifica SRID, orden longitud/latitud, unidades y tipos de
   columna en las migraciones. Mantén la geometría generada y el índice espacial
   conforme al esquema. Para distancias en metros, contrasta el uso de `geography`
   en [RecorridoDAO](../../../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/persistencia/RecorridoDAO.java).
   No midas grados como metros ni calcules el total sobre una lista truncada.
7. Conserva registros de fiscalización y auditoría de solo agregado; no introduzcas
   cascadas o limpiezas que los eliminen. No inventes una política de retención
   de Tracking aún pendiente.

## Pruebas y comprobaciones

Antes de implementar, escribe las pruebas pertinentes: migración desde cero y
actualización con datos previos, restricciones, duplicado concurrente, bordes
temporales y resultado espacial. Reutiliza
[MigracionesContraPostgresTest](../../../carga-ejb/src/test/java/uy/edu/fing/tse/carga/ejb/persistencia/MigracionesContraPostgresTest.java)
sin presentarlo como evidencia de una migración nueva que no se ejecutó.

Desde la raíz, comandos de inspección sin modificar la base:

```sh
git diff --check
find carga-ejb/src/main/resources/db/migration -maxdepth 1 -type f | sort
```

No apliques SQL, levantes contenedores ni ejecutes builds/pruebas que compilen.
Entrega impacto en datos/consumidores, comprobaciones estáticas y pruebas reales
pendientes. Un diff correcto no demuestra sintaxis SQL completa ni rollback/locks.
