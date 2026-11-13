# Solicitud de coordinación: guardar el trazado de las guías

**Fecha:** 2026-11-13  
**Destinatarios:** referente de P2 (guías y persistencia) y compañero responsable
del acceso a la base de datos y despliegue. Coordinar la consulta del chofer con P3.
La distribución de responsabilidades está en [frentes](frentes.md).

## Qué necesitamos

Se solicita acordar y preparar una migración de base de datos para guardar el
trazado planificado que el responsable de empresa visualiza al registrar una
guía. Después, el chofer debe poder seleccionar una guía en **Mis guías de carga**
y consultar **esa misma ruta**, sin volver a calcularla.

La persona que solicita esta funcionalidad **no tiene acceso a la base de datos**.
Por eso necesita coordinar la validación y aplicación del cambio con quien
administra ese entorno; no es necesario compartir credenciales.

**Estado:** guardar el trazado es la preferencia confirmada por el usuario.
El diseño siguiente es una propuesta para coordinación, no un acuerdo técnico
del equipo. No se ha creado esta migración ni implementado la funcionalidad del
mapa del chofer, y no se ha modificado la base de datos para este cambio.

## Situación actual verificada

- La guía conserva origen, destino y sus coordenadas, pero no el trazado de ORS.
- El mapa del responsable es una vista previa orientativa; su resultado no se
  envía actualmente al registrar la guía.
- [ADR-0024](decisiones/0024-vista-previa-guias-ide-ors.md) excluyó expresamente
  persistir el trazado. La ampliación necesita documentarse en un nuevo ADR y
  actualizar el contrato correspondiente antes de implementarse.

Guardar solo origen y destino no permite recuperar exactamente la ruta anterior:
un nuevo cálculo puede devolver un recorrido diferente.

## Propuesta de cambio mínimo

Agregar almacenamiento **opcional y separado** de los datos actuales de la guía,
por ejemplo una tabla `guia_trazado` vinculada por `guia_id`. El nombre, formato
de geometría, columnas, restricciones y límites deben acordarse con P2 antes
de escribir la migración; este documento no es un script SQL para ejecutar.

El registro debería permitir conservar:

- La asociación inequívoca con la guía y la secuencia ordenada de coordenadas.
- Los datos usados para obtener la ruta: origen, destino y peso total considerado.
- La fecha de cálculo y el proveedor; distancia, duración y advertencias que
  correspondan al mismo resultado mostrado al responsable.

El servidor debe vincular el resultado validado a la guía, sin confiar ciegamente
en una geometría enviada por el navegador y sin recalcular otro trazado al guardar.
Si cambian los datos usados para la vista previa, no se debe asociar el resultado
anterior a una guía diferente. El mecanismo concreto queda por diseñar.

## Condiciones para conservar las funcionalidades actuales

| Aspecto | Condición propuesta |
| --- | --- |
| Datos existentes | No borrar, recrear ni modificar guías históricas para inventar rutas |
| Guías sin trazado | Seguir siendo válidas; mostrar «Sin ruta guardada» |
| Fallo de ORS | Conservar la posibilidad de registrar sin trazado, con aviso explícito |
| Listados y CSV | Mantener contratos y resultados actuales; cargar la geometría solo al consultar la ruta seleccionada |
| Acceso del chofer | Validar en servidor que la guía está asignada al chofer autenticado; un ID no concede permiso |
| Tracking y fiscalización | No sustituir el recorrido real ni usar automáticamente esta ruta para detectar desvíos; ese alcance requiere coordinación con P4/P5 |

No basta con cambiar la base: también se necesitan cambios coordinados en el
registro de la guía, la API de consulta y la pantalla del chofer.

## Qué necesitamos del compañero

- [ ] Confirmar quién prepara la migración y quién valida/aplica el despliegue.
- [ ] Acordar con P2 el diseño y con P3 el contrato de consulta del chofer.
- [ ] Preparar una **migración Flyway nueva y aditiva**, con número disponible
      en la rama integrada. No editar migraciones ya aplicadas.
- [ ] Comprobar la migración desde cero y sobre una copia con datos existentes;
      verificar que conserva las guías anteriores y que puede repetirse sin cambios pendientes.
- [ ] Verificar creación de guías con/sin trazado, listado, CSV y rechazo de
      consultas a rutas de otros choferes, junto con el cambio de aplicación.
- [ ] Preparar respaldo y procedimiento de recuperación antes del despliegue;
      acordar ventana y orden de despliegue de aplicación y esquema.
- [ ] Confirmar resultados, entorno y versión de migración aplicada al equipo.

### Precaución al desplegar

Según [ADR-0001](decisiones/0001-flyway-para-el-esquema.md), Flyway gobierna el esquema.
El central llama a `Flyway.migrate()` al arrancar, desde
[`Migraciones.java`](../carga-ejb/src/main/java/uy/edu/fing/tse/carga/ejb/arranque/Migraciones.java).
Desplegar y arrancar una versión que incluya la migración puede aplicarla:
**validar y coordinar antes de iniciar esa versión en el entorno compartido**.

No se solicita ejecutar SQL manual, habilitar DDL automático de Hibernate,
recrear la base ni compartir accesos. La migración y las regresiones todavía
necesitan validación real; este documento no acredita pruebas ejecutadas.

## Respuesta esperada

Confirmar responsable, diseño propuesto, migración que se preparará, entorno de
pruebas y pasos de despliegue/recuperación. Con ese acuerdo se podrá continuar la
implementación coordinada sin solicitar acceso directo a la base.
