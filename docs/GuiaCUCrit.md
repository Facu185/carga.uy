## Actores primarios:

Ciudadano - Chofer: conductor de una empresa transportista que utiliza el componente móvil para registrar el inicio, fin e incidentes de sus viajes de carga, incluso sin conectividad.

Ciudadano - Responsable de empresa: representante de una empresa transportista que gestiona el perfil de la empresa, sus vehículos, los viajes contratados y el descargo de infracciones a través del frontoffice web.
Cliente: empresa que contrata servicios de transporte de carga y necesita validar las autorizaciones vigentes de las empresas transportistas a través del frontoffice web público.
Público general: cualquier persona que consulta información estadística agregada publicada por la plataforma a través del frontoffice web público.
Funcionario: personal del MTOP que gestiona las empresas de transporte, fiscaliza permisos y vehículos, y resuelve los casos de fiscalización a través del frontoffice web.
Autoridad: personal del MTOP que accede a reportes gerenciales agregados sobre la actividad de transporte de carga a través del backoffice web.
Administrador: personal del MTOP que gestiona usuarios, roles, nodos periféricos y la parametrización de las reglas de fiscalización a través del backoffice web.

Sistema: actor que representa el disparo automático de los procesos de fiscalización y de obtención de pesajes, sin intervención directa de un usuario humano. Actúa de forma asíncrona ante la llegada de eventos del nodo Tracking, o ante eventos temporales o de negocio (fin de un viaje, ciclos periódicos).1

## Actores secundarios:

Gub.uy: proveedor de identidad externo utilizado para la autenticación de ciudadanos y funcionarios.

PDI (Plataforma de interoperabilidad): sistema externo que brinda acceso a servicios de organismos públicos, como la obtención de datos del ciudadano vía DNIC.

Balanzas: nodo periférico que provee, a solicitud del componente central, la información de pesaje de los vehículos en los peajes de las rutas.

Tracking: nodo periférico que reporta la posición y ruta seguida por los vehículos durante un viaje.


1 Se clasifica como actor primario —pese a no ser una persona— porque es quien inicia los casos de uso "Obtener pesajes del nodo Balanzas" y "Detectar incumplimiento automáticamente", a diferencia de los actores secundarios, que son consultados o delegados por la plataforma pero no inician ningún caso de uso por sí mismos.



## Caso de Uso Crítico 1 — Autenticarse (frontoffice) con Usuario gub.uy
Descripción
Un ciudadano (responsable de empresa o chofer) o un funcionario del MTOP se autentica en la plataforma desde el frontoffice web o el componente móvil, delegando la verificación de su identidad en Usuario gub.uy. En el primer ingreso de un ciudadano, la plataforma registra su perfil (correo y cédula) y completa sus datos consultando a la PDI. Es crítico porque es precondición de todos los demás casos de uso autenticados; integra un proveedor de identidad externo mediante OpenID Connect desde dos clientes distintos; condiciona el manejo de la sesión, que no puede depender de estado local para permitir el escalado horizontal (Sección 4.3 de la letra); es la base del control de autorización en la capa de servicios (Sección 4.2 de la letra), y ejercita el comportamiento degradado exigido ante la indisponibilidad de la PDI (Sección 4.3 de la letra).

Actor principal: Ciudadano - Responsable de empresa, Ciudadano - Chofer, Funcionario
Actores secundarios: Gub.uy, PDI
Pre-condiciones
La plataforma está registrada como cliente de Usuario gub.uy.
El usuario posee una cuenta en Usuario gub.uy.

Flujo de Eventos
El usuario solicita ingresar desde el frontoffice web o el componente móvil.
El sistema redirige al usuario a Usuario gub.uy e inicia un flujo OpenID Connect (authorization code) con los parámetros state y nonce.
El usuario se autentica en Usuario gub.uy.
Usuario gub.uy redirige al componente central con un código de autorización.
El componente central canjea el código por los tokens y valida su firma, emisor, audiencia, vigencia, state y nonce.
El central consulta UserInfo con el access_token, exige que su sub coincida exactamente con el del id_token validado y obtiene la cédula y, cuando estén disponibles, nombre y correo. Si faltan el documento o el sub, o si no coinciden las identidades, no crea sesión. No usa los datos personales del id_token como alternativa silenciosa (ADR-0017).
Si es el primer ingreso de un ciudadano, el sistema registra su perfil y consulta a DNIC a través de la PDI (Web Service SOAP) para completar sus datos.
El sistema determina los roles del usuario y, si corresponde, la empresa a la que está vinculado.
El sistema crea la sesión sin guardar estado en el servidor de aplicación y habilita las funcionalidades según el rol.

Flujos alternativos
3a. El usuario cancela la autenticación o esta falla en Usuario gub.uy: no se crea la sesión.
5a. El token no supera la validación (firma inválida, state distinto o token vencido): se rechaza el ingreso y se registra como evento de seguridad.
6a. UserInfo no responde, rechaza el access_token o devuelve datos inválidos: se rechaza el ingreso sin crear sesión ni registrar a la persona; la degradación por PDI no se aplica a la verificación de identidad.
7a. La PDI no responde (timeout o mecanismo de corte abierto): se completa el ingreso con la cédula y el correo, y la consulta a DNIC queda pendiente de reintento.
8a. El usuario no tiene rol ni empresa asociada en carga.uy: solo accede a las funcionalidades públicas.
9a. La sesión vence: el usuario debe volver a autenticarse.
Post-condiciones
El usuario queda autenticado, con su identidad verificada por Usuario gub.uy y con sus roles internos.
En el primer ingreso de un ciudadano, queda registrado su perfil.
Observaciones: en el componente móvil, el canje del código por los tokens puede realizarlo la propia aplicación (con PKCE) o el componente central, que luego emite a la aplicación un token propio; la segunda opción evita embeber el secreto de cliente en el dispositivo (a definir como decisión de arquitectura). El backoffice utiliza un mecanismo interno de autenticación y queda fuera de este caso de uso.


## Caso de Uso Crítico 2 — Gestionar viajes de carga (crear guía)
Descripción
Dentro de la gestión de viajes de carga, el responsable de una empresa transportista ingresa la guía de un viaje contratado (rubro del cliente, volumen de carga, fecha, origen y destino) y la asigna a un vehículo y a un chofer de su empresa. Es crítico porque la guía es la base de toda la fiscalización automática (Sección 3.5 de la letra): la actividad de un vehículo sin guía configura "viaje sin guía"; el origen y el destino definen el recorrido esperado para detectar desvíos; la fecha determina la vigencia del permiso y de la ITV, y el vehículo asignado aporta la capacidad para detectar exceso de carga. Además, la asignación aplica reglas de negocio sobre varias entidades, debe mantenerse consistente ante asignaciones concurrentes y define la información que el componente móvil descarga para operar sin conectividad.
Actor principal: Ciudadano - Responsable de empresa
Pre-condiciones
El responsable está autenticado (caso de uso "Autenticarse (frontoffice) con Usuario gub.uy").
La empresa tiene al menos un vehículo y un chofer registrados.

Flujo de Eventos
El responsable ingresa los datos de la guía: rubro del cliente, volumen de carga, fecha, origen y destino.
El sistema valida que estén los datos obligatorios y que sean consistentes (p. ej. origen distinto de destino).
El responsable selecciona un vehículo y un chofer de su empresa.
El sistema verifica, en la capa de servicios, que el vehículo y el chofer pertenecen a la empresa del usuario.
El sistema valida que el Permiso Nacional de Circulación y la ITV del vehículo estén vigentes a la fecha del viaje.
El sistema valida que la carga declarada sea compatible con la capacidad de carga del vehículo.
El sistema valida que el vehículo y el chofer no tengan otro viaje asignado en el mismo
período.
El sistema registra la guía y su asignación en una única transacción, y conserva el origen y el destino como recorrido esperado para la regla de desvío de ruta.
La guía queda disponible para que la descargue el componente móvil del chofer asignado.
Flujos alternativos

4a. El vehículo o el chofer no pertenecen a la empresa del usuario: se rechaza la operación. El control se realiza en la capa de servicios (Sección 4.2 de la letra).
5a. El permiso o la ITV están vencidos a la fecha del viaje: se rechaza la asignación indicando el motivo.
6a. La carga declarada supera la capacidad del vehículo: se rechaza la asignación.
7a. El vehículo o el chofer ya tienen otro viaje asignado en un período que se superpone: se rechaza la asignación.
8a. Dos usuarios asignan a la vez el mismo vehículo o chofer para el mismo período: solo una asignación se confirma y la otra es rechazada por el control de concurrencia.
Post-condiciones
Queda registrada la guía, asignada a un vehículo y a un chofer habilitados.
La capacidad, las vigencias, el origen y el destino que usan las reglas de fiscalización quedan asociados al viaje.
La guía queda disponible para el componente móvil del chofer.
Relación con "Registrar evento de viaje": la guía creada aquí es precondición de ese caso de uso (Sección 3.5); si se modifica después de que el chofer la descargó, se aplica la política de resolución de conflictos definida allí.



## Caso de Uso Crítico 3 — Registrar evento de viaje
Descripción
El chofer de un vehículo de carga, mediante el componente móvil, registra eventos asociados a la ejecución de un viaje (inicio, fin, carga/descarga parcial, incidente). Es crítico porque debe funcionar con o sin conectividad de datos (Sección 3.3 de la letra), lo que obliga a persistencia local, generación de identificadores en el dispositivo para garantizar idempotencia en el reenvío, y una política definida de resolución de conflictos frente a modificaciones concurrentes desde el frontoffice.
Actor principal: Ciudadano - Chofer
Pre-condiciones
El chofer está autenticado en el componente móvil (caso de uso "Autenticarse (frontoffice) con Usuario gub.uy").
Existe una Guía asignada al chofer (caso de uso "Gestionar viajes de carga (crear guía)"), descargada previamente al dispositivo.

Flujo de Eventos
El chofer selecciona el viaje asignado y el tipo de evento a registrar.
El sistema genera un identificador único de evento en el dispositivo.
El sistema persiste el evento localmente, con marca de tiempo y datos asociados.
Si hay conectividad, el componente móvil intenta sincronizar el evento con el componente central; si no, lo deja en cola local.
Al recuperar la conectividad, se reenvían los eventos pendientes en orden.
El componente central recibe el evento, valida el identificador para descartar duplicados, y actualiza el estado del viaje.

Flujos alternativos
4a. Sin conectividad: el evento queda en cola local hasta el siguiente intento de sincronización.
6a. Conflicto de versión: si el viaje fue modificado desde el frontoffice mientras el dispositivo
estaba offline, se aplica la política de resolución de conflictos definida por el grupo.

Post-condiciones
El evento queda registrado sin duplicados, se haya sincronizado de inmediato o en diferido.
El estado del viaje refleja el último evento válido procesado.
Relación con "Obtener pesajes del nodo Balanzas": el registro del fin de un viaje dispara la consulta de los pesajes de ese viaje (Sección 3.6).



## Caso de Uso Crítico 4 — Obtener pesajes del nodo Balanzas
Descripción
El componente central consulta al nodo periférico Balanzas las pesadas registradas en los peajes para el vehículo de un viaje en una fecha, las persiste asociadas al viaje y las deja disponibles para la fiscalización y el monitoreo. Es crítico porque es la única integración sincrónica request-response con un nodo periférico (Secciones 3.7.2 y 4.1 de la letra); ejercita los timeouts, la política de reintentos, el mecanismo de corte ante fallas sostenidas y el comportamiento degradado exigidos para que la indisponibilidad de un nodo no afecte al componente central, incluido el escenario de prueba con el Sistema de Balanzas indisponible (Sección 4.3 de la letra), y provee la información de la regla de exceso de carga.
Actor principal: Sistema (se dispara al finalizar un viaje y periódicamente mientras los viajes están en curso)
Actores secundarios: Balanzas
Pre-condiciones
Existe un viaje con vehículo asignado, en curso o finalizado.
El nodo Balanzas está registrado como nodo periférico en la plataforma.

Flujo de Eventos
Se dispara la consulta: el chofer registra el fin del viaje, o se cumple el ciclo periódico de un viaje en curso.
El componente central envía al nodo Balanzas una solicitud sincrónica con el vehículo y la
fecha, con un timeout definido.
El nodo Balanzas devuelve las pesadas registradas en peajes para ese vehículo en esa fecha.
El sistema persiste las pesadas asociadas al viaje, descartando las que ya estaban registradas.
El sistema entrega las pesadas nuevas al proceso de fiscalización.

Flujos alternativos
2a. La solicitud falla o excede el timeout: el sistema reintenta según la política de reintentos definida.
2b. Las fallas se sostienen: se abre el mecanismo de corte y el sistema deja de consultar al nodo durante un período. La consulta queda pendiente y el monitoreo de pesajes informa que la información no está disponible, mientras el resto de la plataforma sigue operando.
3a. No hay pesadas del vehículo en esa fecha: se registra la consulta sin resultados.

Post-condiciones
Las pesadas del viaje quedan registradas sin duplicados y disponibles para la fiscalización y el monitoreo, o la consulta queda pendiente si el nodo no estuvo disponible, sin afectar la disponibilidad del componente central.
Relación con "Detectar incumplimiento automáticamente": las pesadas nuevas obtenidas en este caso de uso son uno de los disparadores de ese caso de uso (Sección 3.7).



## Caso de Uso Crítico 5 — Detectar incumplimiento automáticamente
Descripción
El sistema detecta de forma automática y asíncrona situaciones de posible incumplimiento, cruzando la información de posición recibida del nodo Tracking y las pesadas obtenidas del nodo Balanzas con los datos declarados por las empresas (capacidad de carga, vigencia de permisos e ITV, Guías). Es crítico porque constituye el objetivo de negocio central de la plataforma (Sección 3.5 de la letra) y ejercita el mecanismo de mensajería asíncrona, el motor de reglas parametrizables y la información de ambos nodos periféricos.
Actor principal: Sistema (actor que representa el procesamiento automático de eventos entrantes; no hay intervención humana directa en este caso de uso — se modela como actor separado porque su disparo es reactivo a eventos, no a una acción de un usuario del sistema)
Actores secundarios: Tracking

Pre-condiciones
Existen reglas de fiscalización parametrizadas (umbrales de tolerancia de exceso de carga y desvío de ruta).
Se recibió una posición del nodo Tracking por mensajería asíncrona, o se obtuvieron pesadas nuevas mediante el caso de uso "Obtener pesajes del nodo Balanzas".

Flujo de Eventos
El componente central recibe una posición del nodo Tracking, de forma asíncrona y desacoplada de su procesamiento, o un conjunto de pesadas nuevas.
El motor de reglas evalúa el evento contra la información declarada por la empresa.
El sistema verifica que el hecho no haya generado ya un caso ante un eventual reprocesamiento del mismo evento.
Si corresponde, el sistema crea un caso de fiscalización en estado "detectada".
El sistema notifica a la empresa transportista involucrada.
El sistema registra la creación del caso en la pista de auditoría de solo agregado.

Flujos alternativos
2a. El evento no configura ninguna de las situaciones de incumplimiento definidas: no se genera ningún caso.
3a. El hecho ya generó un caso existente: el sistema descarta el evento sin crear duplicados.
Post-condiciones
Si corresponde, queda creado un nuevo caso de fiscalización en estado "detectada", con su notificación y registro de auditoría asociados.
Relación con "Gestionar caso de fiscalización": la creación de un caso en este flujo es la precondición de entrada del caso de uso "Gestionar caso de fiscalización" (Sección 3.8). Este caso de uso se limita a la detección automática; la resolución del caso por parte de un funcionario se describe por separado.

## Caso de Uso Crítico 6 — Gestionar caso de fiscalización
Descripción
El funcionario del MTOP gestiona el ciclo de vida de un caso de fiscalización previamente detectado: lo consulta en su bandeja de trabajo, revisa la evidencia y el descargo presentado por la empresa, y lo resuelve confirmándolo o desestimándolo. Es crítico porque implementa una máquina de estados controlada por la plataforma (sin transiciones no previstas, Sección 3.5 de la letra) con registro obligatorio en pista de auditoría.
Actor principal: Funcionario
Actores secundarios: Ciudadano - Responsable de empresa (presenta el descargo, en un caso de uso separado)

Pre-condiciones
Existe un caso de fiscalización en estado "detectada" o "notificada" (generado por el caso de uso "Detectar incumplimiento automáticamente").

Flujo de Eventos
El funcionario visualiza el caso en su bandeja de trabajo.
El funcionario consulta la evidencia que originó el caso (p. ej. pesaje registrado vs. capacidad declarada).
El sistema transiciona el caso a "notificada" si aún no lo estaba.
La empresa presenta un descargo dentro del plazo establecido (caso de uso aparte); el caso pasa a "en descargo".
El funcionario revisa el descargo presentado.
El funcionario resuelve el caso (confirma o desestima), indicando el fundamento de la resolución.
El sistema valida que la transición de estado solicitada sea una transición permitida por el
ciclo de vida definido.
El sistema registra la acción en la pista de auditoría (usuario, acción, fundamento, marca de tiempo).

Flujos alternativos
4a. Vence el plazo de descargo sin respuesta de la empresa: el funcionario puede resolver el caso igual (a definir el comportamiento exacto como decisión de arquitectura).
7a. Se intenta una transición no prevista (p. ej. de "detectada" directo a "confirmada" sin pasar por "en descargo"): el sistema rechaza la operación.
Post-condiciones
El caso queda en el estado válido correspondiente ("confirmada" o "desestimada").
Queda un registro inmutable en la pista de auditoría con el fundamento de la resolución.
