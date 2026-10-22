# ADR-0005 — Autorización y concurrencia al crear guías

**Estado:** Aceptada  
**Fecha:** 2026-10-06

**Alineación documental:** 2026-10-22, fase 0 del cierre P2. Se corrige la
referencia histórica a `usuario.empresa_id` para reflejar el contrato y código
actuales. No se cambia la autorización ejecutable ni se acuerda aquí unificar
tipos de vínculo; véase [D0-04](../acuerdos-cierre-p2-fase-0.md#3-d0-04-decisión-de-permisos-todavía-necesaria).

## Contexto

P2 permite crear guías para una empresa vinculada al principal y a un chofer
consultar únicamente sus propias guías. P1 autentica el principal y administra
`VinculoEmpresaUsuario` (`vinculo_empresa_usuario`); responsable es un tipo de
vínculo, no un nuevo rol global. El código actual no utiliza `usuario.empresa_id`
como pertenencia ni autorización.

La creación puede competir entre pedidos simultáneos para el mismo vehículo o chofer. Una simple consulta de conflictos seguida por un `INSERT` no es atómica.

## Decisión

La autorización del responsable se resuelve exclusivamente como:

`principal autenticado -> usuario.cedula -> VinculoEmpresaUsuario de la empresa`.

La creación conserva el rol `CIUDADANO` y exige un vínculo `ACTIVO` con la empresa
del vehículo, comprobado en `empresaParaOperacion`; además verifica empresa
activa y que vehículo y chofer pertenezcan a ella. Un principal sin vínculo se
rechaza; un ID del cliente selecciona el recurso, no lo autoriza.

**Diferencia observada, pendiente de acuerdo P1/P2:** creación y catálogos
empresariales de vehículos/choferes filtran `ACTIVO` **sin tipo de vínculo**. La
consulta empresarial `guiasDeLaEmpresaResponsable` exige `RESPONSABLE/ACTIVO`
y roles `CIUDADANO` o `CHOFER`; el ABM vehicular exige también responsable activo
salvo su excepción administrativa. La alineación documental no endurece ni amplía
ninguno de esos permisos. El criterio definitivo y sus regresiones se acuerdan
antes de cambiar lógica. Los catálogos de rubros/tipos no comprueban vínculo.

El chofer se resuelve por su cédula desde el principal autenticado; el cliente no envía un identificador para consultar `/guias/mis-guias`.

La creación bloquea pesimistamente vehículo y chofer antes de consultar solapamientos, dentro de la transacción EJB. Los períodos son semiabiertos `[inicio, fin)` y la vigencia del extremo final se evalúa en `fin.minusNanos(1)`, que respeta la precisión de `Instant`.

## Alternativas consideradas

- Crear un rol `RESPONSABLE_EMPRESA`: descartado porque cambia el alcance y el modelo de roles entregado por P1.
- Confiar sólo en una consulta de conflictos: descartado por la condición de carrera entre consulta e inserción.
- Probar concurrencia EJB real con el Testcontainers actual: no es posible sin desplegar WildFly/EJB; la infraestructura actual sólo levanta PostgreSQL para migraciones. Se prueban unitariamente la estrategia de bloqueo/consulta y se deja la concurrencia del contenedor para una prueba de integración futura con servidor Java EE.

## Consecuencias

P1 debe administrar los vínculos de forma controlada: activar/revocar una relación
afecta las operaciones empresariales conforme al criterio de cada método. No se
restaura `usuario.empresa_id` ni se infiere pertenencia desde un rol global.
La garantía de concurrencia depende de invocar el bean mediante EJB, para que
exista transacción y `PESSIMISTIC_WRITE`; una invocación directa en una prueba
unitaria no reproduce los locks de base.

Fuentes de la alineación: [contrato Guías P2](../contratos.md#guías-p2),
[GuiaServicioBean](../../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/servicio/GuiaServicioBean.java)
y [VinculoEmpresaUsuarioDAO](../../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/persistencia/VinculoEmpresaUsuarioDAO.java).
