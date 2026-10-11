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
