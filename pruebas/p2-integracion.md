# P2: pruebas contra despliegue aislado

**Estado: preparadas, NO ejecutadas.** No certifican aún seguridad, JTA, concurrencia ni operación.
Las pruebas unitarias añadidas tampoco sustituyen la ejecución en WildFly/PostgreSQL.

## Aceptación y límites

- Dueño: P2, CU2 y consumo de los contratos REST ya existentes de vehículos/guías.
- No modifica P1/P3/P4/P5, contratos, DTO, esquema, CI, Docker, POM ni el E2E general.
- El equipo prepara y despliega los artefactos; el agente NO compila, instala, inicia contenedores ni ejecuta HTTP.
- Clase: `P2ContraDespliegueTest` en las pruebas del paquete `servicio` de `carga-ejb`.
- Usa JUnit 5, `java.net.http.HttpClient` y utilidades JSON de Nimbus, dependencia ya existente.
  No usa `JsonbBuilder`: la API Jakarta presente en el POM no aporta por sí sola un proveedor JSON-B
  para el JVM de JUnit. No se agregan dependencias ni un parser artesanal.
- La etiqueta `integracion` mantiene la exclusión predeterminada del POM. Aun seleccionando esa etiqueta,
  sin `P2_PRUEBAS_HABILITADAS=true` se aborta por suposición antes de cualquier HTTP.
  Con opt-in, configuración obligatoria ausente/inválida FALLA; no se convierte en un skip verde.
- Ejecutar exclusivamente esta clase contra un entorno sintético y desechable; el perfil global también
  contiene pruebas ajenas que pueden iniciar contenedores. El equipo selecciona clase y etiqueta con su runner.

## Provisión previa por el equipo

Usar los flujos existentes de P1 para ingresar usuarios, aprobar vínculos y obtener tokens válidos;
consultar [contratos](../docs/contratos.md) y [pruebas JSF](../docs/pruebas-abm-jsf.md).
NO fabricar JWT, modificar SQL de identidad ni copiar tokens de producción. Los tokens deben estar vigentes
para toda la suite y proceder de los usuarios descritos; no se guardan en el repositorio.

Reservar sin otros escritores/generadores dos empresas sintéticas A/B y, para A:

1. Responsable con rol `CIUDADANO`, vínculo `RESPONSABLE/ACTIVO` con A y SIN vínculo autorizado con B.
2. Dos vehículos distintos habilitados, capacidad de carga ≥1 kg y volumen ≥1 m³.
3. Dos choferes activos distintos pertenecientes a A, cada uno con identidad P1 y rol `CHOFER`;
   sus cédulas deben coincidir con los respectivos principales autenticados.
4. Empresa A activa, rubro existente, permiso e ITV de ambos vehículos vigentes durante TODA la ventana UTC.
5. Ventana libre de al menos cuatro horas para ambos vehículos y choferes. No basta con que las fechas sean
   pasadas: una guía no se vuelve `FINALIZADA` por su fecha prevista. Las pruebas rechazan fixtures ocupadas.
6. Opcionalmente, identidad `ADMINISTRADOR` para comprobar también el listado global permitido.

## Variables exactas

Todas son obligatorias salvo `P2_TOKEN_ADMIN`. No hay valores predeterminados para URL, credenciales ni IDs.

| Variable | Contenido |
| --- | --- |
| `P2_PRUEBAS_HABILITADAS` | Literal `true`: habilita las escrituras de prueba. |
| `P2_ENTORNO_AISLADO_CONFIRMADO` | Literal `true`: el equipo confirma entorno desechable y exclusivo. |
| `P2_API_URL` | Base HTTP(S) del despliegue aislado, terminada en `/carga/api`, sin credenciales/query/fragmento. |
| `P2_TOKEN_RESPONSABLE_A` | Token vigente, SIN prefijo `Bearer`, del responsable de A. |
| `P2_TOKEN_CHOFER_A1`, `P2_TOKEN_CHOFER_A2` | Tokens de los dos choferes de A. |
| `P2_TOKEN_ADMIN` | Opcional: token de administrador. Sin él queda pendiente el caso ADMIN 200. |
| `P2_EMPRESA_A_ID`, `P2_EMPRESA_B_ID` | IDs positivos distintos de A/B. |
| `P2_VEHICULO_A1_ID`, `P2_VEHICULO_A2_ID` | IDs positivos distintos de los vehículos de A. |
| `P2_CHOFER_A1_ID`, `P2_CHOFER_A2_ID` | IDs positivos distintos de los choferes de A; corresponden a los tokens. |
| `P2_RUBRO_ID` | ID positivo de un rubro existente. |
| `P2_INICIO_BASE` | Inicio ISO-8601 con zona, dentro de las vigencias de ambos vehículos. |
| `P2_FIN_VENTANA` | Fin ISO-8601, al menos cuatro horas después del inicio base. |

No desactivar TLS para pruebas remotas. Usar HTTP solo en aislamiento local controlado.
No se puede demostrar aislamiento por el nombre del host: la confirmación es responsabilidad del equipo.

## Casos automatizados preparados

| Caso | Resultado exigido |
| --- | --- |
| Responsable A consulta flota/guías de B | HTTP 403 en ambos endpoints. |
| Responsable consulta flota sin empresa | HTTP 403; administrador opcional recibe 200. |
| Crear guía válida | 201, `ASIGNADA`, empresa A, coordenada negativa conservada, una guía adicional en lectura empresarial. |
| Descarga privada del chofer | La guía nueva aparece en `mis-guias` de A1 y no en la de A2. |
| Mismo vehículo, distintos choferes concurrentes | Un 201 y un 400 con motivo de solapamiento; una sola guía adicional y asignación completa observable. |
| Mismo chofer, distintos vehículos concurrentes | Un 201 y un 400 por solapamiento; una sola guía adicional y asignación completa observable. |
| Dos intervalos consecutivos `[inicio, fin)` | Dos 201, dos IDs distintos y dos nuevas guías, comprobando cada escritura por separado. |

Las carreras liberan dos solicitudes HTTP independientes mediante una barrera de inicio, SIN `sleep`.
Cada petición tiene timeout de 20 segundos, la barrera 10 segundos y la espera de cada resultado 30 segundos.
La suite se declara `SAME_THREAD` para no mezclar sus escenarios aunque el runner habilite paralelismo.
Esto coordina clientes, no fuerza una intercalación interna particular del servidor; repetir con una nueva
ventana es útil, pero un resultado favorable no es una prueba exhaustiva de todas las intercalaciones.

El listado empresarial se recorre completo en páginas de 100: total estable, IDs únicos, empresa correcta y
cantidad acumulada igual al total; falla ante páginas vacías prematuras o cambios de otros escritores.
Cada escritura compara el total y los IDs antes/después y filtra la marca/intervalo del escenario; NO presupone
que la empresa esté vacía. Se reserva un límite defensivo de 10.000 páginas para no hacer lecturas ilimitadas.
La lectura acredita ausencia de filas parciales de guía observables por esa API, NO inspecciona por sí sola
otras tablas, locks ni todos los efectos transaccionales internos.

## Ventanas y limpieza

Desde `P2_INICIO_BASE`, cada escenario utiliza una ventana distinta:

- Guía válida: minutos 0–30.
- Carrera por vehículo: minutos 60–90.
- Carrera por chofer: minutos 120–150.
- Consecutivos: minutos 180–210 y 210–240.

La suite deja **cinco guías nuevas** si todos los casos pasan. No cambia estados ni borra entidades:
no existe una limpieza REST autorizada en este alcance. Descartar/restaurar el entorno sintético usando
los procedimientos del equipo, o reservar otra ventana con recursos libres antes de repetir. Una falla
puede haber dejado guías confirmadas; nunca reintentar ciegamente con la misma ventana.

## Cobertura complementaria y evidencia

`GuiaServicioBeanTest` cubre validación de carga, obligatorios, fechas, referencias inexistentes, empresa/vehículo/
chofer inactivos, pertenencia, principal, permiso/ITV en extremos UTC, límites de capacidades y consulta
semiabierta de conflictos. Sus dobles observan persist/flush y consulta de vínculos activos, no ejecutan JPQL.
`GuiasEmpresaTest` existente cubre permisos, paginación e históricos sin asignaciones; no se modifica.

Siguen MANUALES el ABM persistido, JSF/AJAX, clave temporal, expiración/salida y el flujo completo de alta
hasta consulta; véanse [matriz P2](p2-cierre.md), [guías empresariales](guias-empresa.md) y
[ABM JSF](../docs/pruebas-abm-jsf.md). La prueba HTTP no se presenta como E2E de navegador.

Registrar versión/commit y cambios locales incluidos, versión WildFly/PostgreSQL, fecha UTC, identidad de
fixtures mediante alias, ventana, selección de casos, informe JUnit y resultado ADMIN ejecutado/pendiente.
No adjuntar tokens, cédulas ni cuerpos completos de respuestas. La suite omite cuerpos/credenciales en
sus mensajes de aserción y no imprime datos personales.

**Pruebas escritas, ciclo rojo/verde no ejecutado por restricción de builds.**
Hasta que el equipo compile/despliegue y ejecute los casos, P2 permanece como
**implementación preparada, operación no acreditada**.
