# Reglas de implementación de carga.uy

Este es el punto de entrada para las reglas del equipo. La fuente detallada es [Estandares/RULES.md](Estandares/RULES.md).

Antes de implementar un caso de uso crítico:

1. Consultar [GuiaCUCrit.md](GuiaCUCrit.md) y preservar sus precondiciones, flujos alternativos y postcondiciones.
2. Respetar los contratos publicados en [contratos.md](contratos.md) y coordinar cualquier cambio incompatible.
3. Aplicar los estándares de [diseño de APIs](Estandares/api-design.md), [calidad Java](Estandares/code-quality.md) y [errores/observabilidad](Estandares/error-handling.md).
4. Registrar en `docs/decisiones/` cualquier política arquitectónica aún pendiente antes de codificarla.

La implementación de los componentes del sistema es Java/Jakarta EE sobre WildFly.
