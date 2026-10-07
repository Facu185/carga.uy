# Diseño de APIs e integraciones

Estas reglas aplican a los endpoints JAX-RS, los contratos JMS y las integraciones SOAP de carga.uy. Los endpoints ya publicados en [contratos.md](../contratos.md) son contratos compatibles: no se renombran ni se cambia su semántica sin coordinación del equipo.

## Hacer

- Implementar recursos REST con JAX-RS en `carga-web`; el recurso valida sintaxis y traduce HTTP, pero delega reglas de negocio, autorización y transacciones en un servicio/EJB.
- Usar `application/json`, DTOs de `carga-api` y JSON-B para REST. No exponer entidades JPA ni detalles de persistencia.
- Mantener el prefijo publicado `/carga/api`. Las rutas nuevas siguen `/carga/api/<tema>` y tienen una responsabilidad clara. Si en el futuro se requiere una versión incompatible, se publica una ruta versionada sin romper la vigente.
- Responder con códigos HTTP semánticos: `201 Created` al crear, `202 Accepted` al aceptar trabajo asíncrono, `204 No Content` cuando no hay cuerpo, `400` para solicitud inválida, `401/403` para autenticación/autorización, `404` para recurso inexistente, `409` para conflicto de concurrencia o estado y `422` para una regla de negocio válida en forma pero imposible de cumplir.
- Para errores REST, devolver una respuesta consistente y sin detalles internos:

  ```json
  {
    "error": {
      "code": "REGLA_NEGOCIO",
      "message": "La carga declarada supera la capacidad del vehículo.",
      "details": [{"field": "volumenCarga", "issue": "supera la capacidad"}],
      "reference": "correlation-id"
    }
  }
  ```

- Propagar el identificador de correlación entre HTTP, JMS y las llamadas salientes; devolverlo como referencia de soporte cuando corresponda.
- En eventos de móvil, Tracking y reintentos, exigir `idEvento` generado por el origen y hacer idempotente el consumidor. El endpoint de Tracking responde `202` solo tras encolar el evento.
- Mantener el contrato SOAP de PDI con JAX-WS/JAXB y los DTO adaptados para ese contrato. Tratar timeouts y fallas de la PDI como una condición degradada, no como una caída del central.
- Definir límites y paginación para listados que puedan crecer. Los listados públicos deben aplicar agregación y anonimización antes de serializar la respuesta.

## No hacer

- No devolver `200 OK` para toda situación ni usar excepciones o stack traces como respuesta HTTP.
- No exponer entidades JPA, claves técnicas secuenciales, credenciales, rutas internas ni datos personales innecesarios.
- No poner lógica de negocio, consultas complejas ni autorización en un recurso JAX-RS o en un backing bean JSF.
- No usar `ObjectMessage` para eventos compartidos: las colas usan `TextMessage` con JSON para evitar acoplar productores y consumidores a una versión de clase.
- No bloquear la recepción de Tracking esperando su procesamiento ni permitir que una falla de Balanzas/PDI propague indisponibilidad al central.
