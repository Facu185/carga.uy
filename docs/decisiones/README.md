# Decisiones de arquitectura (ADR)

Un archivo por decisión que sea difícil de revertir o que alguien vaya a cuestionar dentro de tres
meses. Se numeran y **no se borran**: si una decisión se reemplaza, se marca como *Reemplazada por
ADR-000X* y se escribe la nueva.

Plantilla: [plantilla.md](plantilla.md).

| # | Decisión | Estado |
|---|---|---|
| [0001](0001-flyway-para-el-esquema.md) | El esquema lo gobierna Flyway, no Hibernate | Aceptada |
| [0002](0002-ingesta-de-tracking-por-rest-mas-jms.md) | El nodo Tracking publica por REST y el central encola | Aceptada |
| [0003](0003-postgis-y-ldap-como-opcionales.md) | Opcionales elegidos: PostGIS y LDAP | Aceptada |
| [0004](0004-frontend-react-sobre-api-rest.md) | Interfaces web en React sobre la API REST | Propuesta |
| [0005](0005-autorizacion-y-concurrencia-de-guias.md) | Autorización y concurrencia al crear guías | Aceptada |
| [0006](0006-obtencion-de-pesajes.md) | Obtención de pesajes: circuito compartido, deduplicación en la base y aviso por cola | Aceptada |
| [0007](0007-seguridad-de-nodos-y-endpoints-p4.md) | Autenticación de los nodos periféricos y roles en los endpoints de P4 | Aceptada |
| [0008](0008-volumen-y-saturacion-de-tracking.md) | Volumen objetivo de Tracking y política de saturación | Aceptada |
| [0009](0009-administracion-de-nodos-perifericos.md) | Administración de nodos periféricos: la base manda | Aceptada |
| [0010](0010-integraciones-de-p4-con-p3-y-p5.md) | Integraciones de P4 con P3 (fin de viaje) y con P5 (recorrido) | Aceptada |
| [0011](0011-autenticacion-del-abm-jsf.md) | Adaptador JWT para conservar el ABM JSF P2 | Aceptada para corrección P2 |
| [0012](0012-abm-vehiculos-del-responsable.md) | Vehículos en Mi empresa, baja con control de guía en curso y reactivación | Aceptada para alcance solicitado |
| [0013](0013-area-funcionario-spa-p2.md) | Área operativa F12–F14 del funcionario en la SPA existente, conservando JSF | Aceptada para alcance P2 solicitado |
| [0014](0014-perfil-propio-empresa.md) | Perfil propio F10 con contrato y campos separados del ABM administrativo | Aceptada para alcance F10 solicitado |
| [0015](0015-incompatibilidad-funcionario-responsable.md) | Incompatibilidad entre funcionario activo y responsabilidad empresarial activa | Aceptada para cambio confirmado por usuario |
| [0016](0016-consulta-vehicular-funcionario.md) | Consulta vehicular F13 separada del ABM, con referencia temporal de servidor | Aceptada para C13 confirmado por usuario |
| [0017](0017-id-uruguay-userinfo-testing.md) | Datos personales por UserInfo y configuración explícita de ID Uruguay testing | Aceptada para alcance confirmado por usuario |
| [0018](0018-consulta-viajes-funcionario-p2.md) | Consulta C14 de guías y preparación F14 limitada a P2 | Aceptada para C14 y entrega parcial confirmados por usuario |
| [0019](0019-registro-de-eventos-de-viaje.md) | Registro de eventos de viaje: sincrónico, idempotente y el central decide | Aceptada por el usuario para política central e inicio/baja; implementación local, coordinación P3/móvil y validación real pendientes de acreditar |
| [0020](0020-tablero-autoridad-actividad.md) | Tablero de autoridad por fecha de creación de guías y días America/Montevideo | Aceptada para MVP confirmado por el usuario; validación integrada pendiente |
| [0021](0021-frontoffice-publico-y-umbral-de-agregacion.md) | Frontoffice público: validación de permisos y umbral de agregación (5 guías de 3 empresas) | Propuesta |
| [0022](0022-fiscalizacion-automatica-y-casos.md) | Fiscalización automática: reglas, criterio de desvío, ciclo del caso y parametrización | Propuesta |
| [0023](0023-reporte-viajes-finalizados-autoridad.md) | Viajes finalizados de autoridad por instante del evento FIN, separados de guías creadas | Aceptada para recomendación solicitada; validación integrada pendiente |
