# División del trabajo — Grupo 5

5 frentes, uno por persona. Cada frente es dueño de sus casos de uso, sus tablas y sus pruebas.
La base común (este repositorio) ya está: módulos, Docker, base de datos, ejemplos de cada
tecnología.

**Opcionales elegidos (carrera Tecnólogo: alcanza con 2 puntos):** PostGIS y LDAP. Son los dos que
mejor se apoyan en lo que el sistema ya necesita — la georreferenciación hace falta igual para la
regla de desvío de ruta, y LDAP se acopla al frente de identidad sin tocar el resto.

---

## P1 — Identidad, acceso e integración con la PDI

- Autenticación del frontoffice con Usuario gub.uy (OIDC). **Caso de uso crítico.**
- Consulta de datos de la persona al servicio de DNIC **por SOAP a través de la PDI**.
- Flujo alternativo obligatorio: si la PDI no responde, el ingreso se completa igual con los datos
  mínimos verificados de UserInfo, vinculado al `id_token` (ADR-0017).
- Roles y autorización (ciudadano, chofer, funcionario, autoridad, administrador).
- **Opcional: LDAP** para los usuarios internos del backoffice.
- Pista de auditoría (tabla `auditoria`, solo se agrega, nunca se modifica).

Tablas: `usuario`, `rol`, `usuario_rol`, `auditoria`. Código de arranque: `PdiBean`, `simulador-pdi`.

## P2 — Núcleo de negocio y backoffice

- Empresas, vehículos, choferes: altas, bajas, modificaciones y consultas.
- Guías de viaje de carga: creación, asignación de vehículo y chofer. **Caso de uso crítico.**
- Vista previa de nueva guía solicitada por el usuario: sugerencias IDE, ruta ORS
  por peso y mapa Leaflet, acotada por [ADR-0024](decisiones/0024-vista-previa-guias-ide-ors.md).
  No cambia CU2, portal compartido P3 ni fiscalización/georreferenciación P5; sin
  trazado persistido y aceptación integrada aún pendiente.
- Backoffice en JSF + PrimeFaces.
- Perfil propio F10 en la SPA, alcance acotado aprobado por el usuario en
  [ADR-0014](decisiones/0014-perfil-propio-empresa.md). Conserva Choferes/Vehículos
  y requiere acreditar integración del portal compartido con P1/P3.
- Área operativa del funcionario: gestión de empresas, consulta de vehículos y
  permisos/ITV, e integración del monitoreo de viajes/pesajes (F12–F14), asignada
  a P2 por el usuario el 2026-10-22. P3 conserva estado/historial y P4 sus datos e
  integraciones. La ampliación acotada de la SPA está confirmada en
  [ADR-0013](decisiones/0013-area-funcionario-spa-p2.md); se conserva el ABM JSF,
  sin aceptar globalmente ADR-0004. Contratos y navegación compartida aún por
  acordar: véanse el [plan por fases](plan-cierre-p2.md) y el
  [registro de fase 0](acuerdos-cierre-p2-fase-0.md). F04/casos sigue en P5;
  su eventual integración de interfaz requiere acuerdo separado. F15 continúa
  sin asignación confirmada.
- Validaciones de negocio: vehículo habilitado, permiso vigente, ITV al día.

Tablas: `empresa`, `vehiculo`, `chofer`, `guia`, `rubro`, `tipo_carga`.
Código de arranque: `EmpresaServicioBean`, `EmpresaRecurso`, `empresas.xhtml`.

## P3 — Frontoffice y eventos de viaje

- Portal del ciudadano y del chofer.
- Registro de eventos de viaje: inicio, fin, cargas y descargas parciales, incidentes, con foto.
  **Caso de uso crítico.**
- Consulta del estado de un viaje y de su historial.
- Funciona con conectividad intermitente: el evento lleva `idEvento` propio y se puede reenviar.

Tablas: `evento_viaje`. Patrón a seguir: el mismo de `PosicionDTO` / `PosicionMDB` (idempotencia).

## P4 — Nodos periféricos e integración

- Nodo **Balanzas**: obtención de pesadas de los peajes. Request-response iniciado por el central.
  **Caso de uso crítico.**
- Nodo **Tracking**: ingesta de posiciones vía cola JMS.
- Resiliencia: timeouts, reintentos, corte de circuito, comportamiento degradado cuando un nodo se cae.
- Pruebas de las integraciones con el nodo lento y con el nodo caído (`BALANZAS_DEMORA_MS`, `BALANZAS_FALLA`).

Tablas: `posicion_tracking`, `pesada`, `nodo_periferico`.
Pantalla propia: **panel de operación** (`/carga/panel.xhtml`) con el estado de los nodos, los
contadores de ingesta y el mapa de la flota en vivo.

Código de arranque: `PesajeBean`, `MonitorBean`, `IngestaTrackingBean`, `PosicionMDB`,
`nodo-balanzas`, `nodo-tracking`, `PanelVista` + `panel.xhtml`.

## P5 — Fiscalización y georreferenciación

- Detección automática de incumplimientos: exceso de carga, vehículo sin habilitación, viaje sin
  guía, desvío de ruta. **Caso de uso crítico.**
- Gestión del caso de fiscalización: notificación, descargo de la empresa, resolución fundada.
  **Caso de uso crítico.**
- **Opcional: PostGIS** — mapa de la flota, recorrido de un viaje, cálculo de distancia al recorrido
  esperado para la regla de desvío.
- Reportes y tableros para la autoridad.
  MVP de actividad solicitado por el usuario en [ADR-0020](decisiones/0020-tablero-autoridad-actividad.md):
  agregados por creación de guías y días America/Montevideo, en el backoffice SPA
  existente. No incluye fiscalización ni reasigna P5 a P2; código local y pruebas
  escritas, aceptación integrada y coordinación externa pendientes.
  Ampliación confirmada (2026-11-03): filtro opcional de tipo de carga solo para
  actividad; fiscalización general por período y exportaciones con ámbitos
  explícitos. No modifica motor/casos, permisos ni propiedad P5; aceptación
  pendiente en la [matriz](../pruebas/tablero-autoridad.md).
  [ADR-0023](decisiones/0023-reporte-viajes-finalizados-autoridad.md) añade modo
  Viajes finalizados por evento FIN aceptado; conserva creación por defecto y
  fiscalización general. Lectura interna de eventos P3 sin ampliar historial ni
  modificar productor. Código local/pruebas escritas, validación real pendiente.

Tablas: `caso_fiscalizacion`, `descargo`, `parametro_regla`. Las columnas `geometry` ya están
creadas en la migración V1.

---

## Lo que es de todos

- **Documentación:** SAD actualizado, decisiones en `docs/decisiones/`.
- **Pruebas:** cada uno prueba lo suyo (~80% sobre la lógica de negocio).
- **Revisión cruzada:** cada PR lo revisa alguien de otro frente.
- **Despliegue en la nube:** es un requisito no funcional del sistema, no un caso de uso. Lo arma
  quien tenga el entorno andando y lo documenta para el resto.

## Cómo evitar pisarse

1. Las interfaces y los DTO compartidos se acuerdan **antes** de implementar
   ([docs/contratos.md](contratos.md)).
2. Cada frente crea sus propias migraciones, numeradas; si dos personas tocan la misma tabla en el
   mismo día, se coordina en el grupo.
3. Nadie edita el código de otro frente sin avisar: se abre un issue o se pide el cambio.
