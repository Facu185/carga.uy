# Calidad de código Java

## Hacer

- Usar Java 24 y Jakarta EE 11, conforme al `pom.xml` raíz. Preferir las APIs estándar de Jakarta EE antes de incorporar una dependencia.
- Respetar las responsabilidades de los módulos: `carga-api` contiene interfaces y DTOs compartidos; `carga-ejb`, dominio, servicios, persistencia, mensajería y migraciones; `carga-web`, JSF, JAX-RS y SOAP; `carga-ear`, el empaquetado. Los nodos periféricos y el simulador no dependen de implementaciones internas del central.
- Mantener paquetes con responsabilidad explícita, por ejemplo `api.dto`, `ejb.modelo`, `ejb.servicio`, `ejb.jms`, `web.rest` y `web.jsf`. Nombrar tipos en `PascalCase`, métodos y campos en `camelCase`, constantes en `UPPER_SNAKE_CASE` y tablas/columnas en `snake_case` singular.
- Separar presentación/transporte, negocio, persistencia e integración. Un recurso JAX-RS o backing bean JSF recibe datos, invoca al servicio y presenta el resultado; no contiene reglas de negocio.
- Usar DTOs serializables, con constructor vacío y getters/setters, cuando crucen REST, SOAP, JMS o EJB remoto. Las fechas en JSON/JMS se intercambian como ISO-8601 conforme a `contratos.md`.
- Aplicar validaciones sintácticas en el borde y reglas de negocio/autorización dentro del servicio. Modelar errores esperados con excepciones de negocio específicas, por ejemplo `NegocioException` con `@ApplicationException(rollback = true)` cuando deba revertirse la transacción.
- Inyectar dependencias con CDI/EJB (`@Inject`, `@EJB`) y mantener a los servicios sin estado local. Usar transacciones declarativas de EJB o límites transaccionales explícitos cuando una operación afecte varias entidades.
- Usar JPA para entidades y Flyway para todos los cambios de esquema. Agregar una nueva migración `V<n>__descripcion.sql`; nunca editar una migración aplicada ni habilitar la creación automática del esquema.
- Mantener la configuración fuera del código mediante variables de entorno o configuración del servidor. Documentar valores por defecto seguros en `.env.example` o README, sin contraseñas reales.
- Formatear el código con la configuración Java acordada e incorporar al pipeline una verificación estática compatible con Maven (por ejemplo Checkstyle, PMD o SonarQube) antes de introducirla como regla obligatoria.
- Escribir pruebas JUnit 5 para la lógica de negocio. Toda corrección de un defecto empieza por una prueba que lo reproduzca; las pruebas de integración deben cubrir contratos REST/SOAP/JMS y fallas de nodos cuando aplique.

## No hacer

- No introducir herramientas de otro ecosistema en este repositorio. La implementación y sus herramientas son Java, Maven y Jakarta EE.
- No acoplar un consumidor a una clase concreta de otro módulo; depender de las interfaces públicas en `carga-api`.
- No usar estado global mutable, `System.out.println`, credenciales, URLs o umbrales mágicos en el código.
- No devolver ni persistir entidades JPA como si fueran DTOs de frontera.
- No silenciar excepciones, desactivar verificaciones estáticas sin justificación, dejar código comentado ni agregar librerías para una capacidad que provee el JDK o Jakarta EE.
- No implementar cambios de contrato, tabla compartida o flujo crítico sin actualizar primero la documentación afectada y acordarlo con el equipo.
