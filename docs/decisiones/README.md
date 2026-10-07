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
| [0005](0005-autorizacion-y-concurrencia-de-guias.md) | Autorización y concurrencia al crear guías | Aceptada |
