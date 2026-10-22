# Regresión: guías de la empresa

Escenarios manuales pendientes; la SPA no tiene runner de pruebas configurado.
No se ejecutaron builds ni ciclo rojo/verde. Las pruebas JUnit con dobles no
demuestran la ejecución del JPQL, los roles ni la autorización en WildFly.

1. Sin vínculo, con solicitud pendiente, rechazada o revocada: mantener la tarjeta
   de solicitud; no mostrar el acceso a guías empresariales.
2. Con vínculo RESPONSABLE/ACTIVO: no mostrar la solicitud (tampoco durante la
   carga inicial); mostrar el acceso al listado y conservar administración de choferes.
3. Si falla la consulta de vínculos en Mi cuenta: informar el error y permitir
   reintentar, sin ofrecer una solicitud como si se supiera que no existe vínculo.
4. Con guías ASIGNADA y FINALIZADA: consultar ambas, con número, chofer, vehículo,
   recorrido y período. Un chofer inactivo debe seguir apareciendo en el historial.
5. Con más de 20 guías: avanzar y retroceder páginas, sin perder guías; el orden
   de igual fecha debe ser estable por ID descendente. Probar lista vacía.
6. Con varios vínculos responsables: cambiar empresa y regresar a página 1;
   una respuesta tardía de la empresa anterior no debe reemplazar la actual.
7. Invocar la API con otra empresa, vínculo OPERADOR/PENDIENTE/REVOCADO, sin sesión
   o con rol insuficiente: no obtener datos (403; sin sesión, 401 mediante
   AccesoDenegadoMapper). Probar empresa inválida y paginación inválida: 400.
8. Simular error de red y sesión vencida; no mostrar filas antiguas como resultados
   actuales ni un error como lista vacía. Reintentar sin recargar toda la aplicación.
9. Navegar con teclado por selector, enlaces y paginación; revisar tabla en móvil.
10. Con una guía histórica CREADA sin chofer/vehículo: incluirla en la página y el
    total; mostrar “Sin chofer asignado” / “Sin vehículo asignado”, sin error 500.

El estado mostrado es el persistido: NO se deduce finalización por la fecha prevista.
El nombre del chofer y la matrícula son los actuales de los registros relacionados,
no una instantánea histórica inmutable ni una pista de reasignaciones.
