# Auditoría de código bajo solicitud

Se aplica SOLO cuando el usuario solicita auditar/revisar código. No ejecutar por defecto al implementar. Es una revisión técnica, no la pista de auditoría inmutable del producto.

## Alcance y método

1. Identifica el alcance pedido (diff, archivos, módulo o caso) y línea base; si no puede inferirse, pregunta y espera. Lee [AGENTS.md](../AGENTS.md), [contexto](contexto-proyecto.md), [mapa](mapa-arquitectura.md) y contratos/ADR pertinentes.
2. Lee el código afectado, dependencias y pruebas relacionadas. Contrasta cada presunto defecto con su flujo y contrato; no confíes en descripciones del implementador ni en objetivos documentados como prueba.
3. NO modifiques archivos, no hagas commits, no ejecutes builds ni pruebas que compilen. No actives un implementador para corregir: entrega el informe y espera instrucciones. Las restricciones de solo lectura del rol son de comportamiento; no sustituyen permisos de sesión.
4. Prioriza defectos concretos sobre estilo. No conviertas discrepancias ya registradas en hallazgos nuevos salvo que el cambio las agrave o el alcance pida analizarlas. Separa hechos, inferencias y pruebas pendientes.

## SOLID con evidencia, no etiquetas

| Principio | Pregunta accionable | Evidencia útil / señal de problema |
| --- | --- | --- |
| SRP | ¿Cada unidad tiene un motivo coherente para cambiar? | Recursos/backing beans/componentes con reglas de negocio, SQL, transporte y autorización mezclados; indicar qué cambios quedan acoplados. No denunciar por cantidad de líneas solamente. |
| OCP | ¿Una variación necesaria puede añadirse sin romper comportamiento estable? | Condicionales duplicados en múltiples sitios ante una nueva regla; proponer extensión solo si existe esa variación real. No exigir patrones hipotéticos. |
| LSP | ¿Una implementación conserva pre/postcondiciones y resultados del contrato? | Implementación que exige más permisos/datos, omite garantías, cambia errores/rollback o devuelve valores incompatibles; contrastar consumidor y pruebas. |
| ISP | ¿El consumidor depende solo de operaciones que necesita? | Interfaces que obligan a implementar métodos ajenos o acoplan consumidores a varios frentes; mostrar quién queda afectado antes de pedir división. |
| DIP | ¿Negocio y consumidores dependen de contratos apropiados? | Importar EJB concreto desde web/nodos, construir clientes externos dentro de reglas sin punto de sustitución o filtrar tipos de transporte; verificar POM e inyección. No imponer repositorios por toda llamada JPA existente. |

## Comportamiento, seguridad e integración

- [ ] **Casos de uso:** precondiciones, alternativas y postcondiciones de [GuiaCUCrit](../docs/GuiaCUCrit.md); transiciones de fiscalización válidas y fundamento requerido. No inventar reglas pendientes.
- [ ] **Contratos:** DTO/interface/ruta/status/JSON/cola compatibles con [contratos](../docs/contratos.md) y consumidores; ISO-8601, unidades, nulos, límites y paginación. No filtrar entidades JPA.
- [ ] **Autorización:** identidad del principal del servidor, rol y pertenencia empresarial por recurso; no confiar en IDs, roles o empresa enviados por cliente. Verificar excepción P4 de roles en recursos EJB (ADR-0007) y advertencia de guías (contexto).
- [ ] **Datos/secretos:** no tokens/credenciales ni datos personales en logs/respuestas; configuración externa, comparación segura de tokens de nodo y fallo cerrado; distinguir entorno local de HTTPS desplegado.
- [ ] **Transacciones:** escrituras relacionadas y publicación JMS coherentes; rollback ante fallas; proxy/contenedor requerido para roles/JTA. No afirmar atomicidad real a partir de anotaciones o mocks solamente.
- [ ] **Concurrencia/idempotencia:** ID estable en origen y restricción/deduplicación atómica en base; carrera entre consultar/insertar; locks y solapamientos `[inicio, fin)`; reintentos/reentregas/DLQ sin efectos duplicados. La clave puede ser `idEvento` o `idPesada` según el contrato, no asumir un único campo universal.
- [ ] **Integraciones:** timeout, reintentos limitados solo apropiados, circuito compartido y degradación; ingesta acusa aceptación después de encolar; saturación/`Retry-After` y límites de lote según ADR-0008.
- [ ] **Persistencia:** migración nueva, sin editar aplicada ni DDL automático; tipos/índices/restricciones acordes a consultas y volumen. No eliminar evidencia de fiscalización ni registros de auditoría.
- [ ] **Observabilidad:** correlación HTTP/JMS/salientes, errores seguros sin ocultar causa, métricas sin cardinalidad por persona/vehículo, auditoría de negocio solo agregado.
- [ ] **Pruebas:** regresión reproducible; positivo/negativo, permisos, borde temporal, duplicados, caída de nodos y rollback cuando aplique; distinguir test unitario de integración con WildFly/PostGIS/broker. No atribuir cobertura ni éxito sin resultados.

## Formato del informe

Primero hallazgos, ordenados por severidad:

```text
[ALTA] Título específico
Ubicación: ruta/archivo:línea (o rango mínimo)
Evidencia: comportamiento observado en código y contrato/consumidor que contradice.
Impacto y escenario: qué falla, para quién y bajo qué condición.
Recomendación: cambio mínimo sugerido y alternativa con tradeoff si corresponde.
Validación: prueba existente/faltante; ejecución realizada o pendiente.
```

Severidades: **CRÍTICA** (exposición grave/pérdida de integridad o indisponibilidad general), **ALTA** (defecto relevante de seguridad/negocio), **MEDIA** (caso acotado o riesgo de mantenimiento concreto), **BAJA** (mejora no bloqueante). Justifica la prioridad con impacto y condiciones, no con preferencia estética.

Cierra con alcance revisado, dudas, riesgos residuales y pruebas pendientes. Si no encuentras defectos sustentables, escribe **“Sin hallazgos en el alcance revisado”**, sin prometer ausencia de errores ni ejecución de pruebas que no ocurrió. Las correcciones requieren una solicitud posterior.
