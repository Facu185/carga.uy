# ============================================================================
#  Componente central de carga.uy
#  Etapa 1: Maven -> carga.ear + simulador-pdi.war
#  Etapa 2: WildFly (perfil full, necesario para JMS)
# ============================================================================
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-24
ARG WILDFLY_IMAGE=quay.io/wildfly/wildfly:41.0.0.Final-jdk25

FROM ${MAVEN_IMAGE} AS build
WORKDIR /src

# 1) Solo los pom: asi Docker cachea la descarga de dependencias y no la repite
#    en cada cambio de codigo.
COPY pom.xml .
COPY carga-api/pom.xml     carga-api/
COPY carga-ejb/pom.xml     carga-ejb/
COPY carga-web/pom.xml     carga-web/
COPY carga-ear/pom.xml     carga-ear/
COPY simulador-pdi/pom.xml simulador-pdi/
COPY nodo-balanzas/pom.xml nodo-balanzas/
COPY nodo-tracking/pom.xml nodo-tracking/
RUN mvn -B -ntp -pl carga-ear,simulador-pdi -am dependency:go-offline || true

# 2) Codigo y empaquetado
COPY . .
RUN mvn -B -ntp -pl carga-ear,simulador-pdi -am clean package -DskipTests

# ============================================================================
FROM ${WILDFLY_IMAGE}
ENV JBOSS_HOME=/opt/jboss/wildfly
ARG PG_DRIVER_VERSION=42.7.4

# Driver PostgreSQL + datasource CargaDS, aplicados con el CLI en modo embebido
# (no hace falta arrancar el server para configurarlo).
ADD --chown=jboss:jboss \
    https://repo1.maven.org/maven2/org/postgresql/postgresql/${PG_DRIVER_VERSION}/postgresql-${PG_DRIVER_VERSION}.jar \
    /tmp/postgresql.jar
COPY --chown=jboss:jboss docker/central/configure.cli /tmp/configure.cli
RUN "$JBOSS_HOME/bin/jboss-cli.sh" --file=/tmp/configure.cli \
 && rm -rf "$JBOSS_HOME/standalone/configuration/standalone_xml_history" /tmp/*.cli /tmp/postgresql.jar

# Usuario de aplicacion, para invocar EJBs remotos desde fuera del contenedor
# (JBOSS-LOCAL-USER no sirve porque el cliente no comparte el file system).
RUN "$JBOSS_HOME/bin/add-user.sh" -a -u carga -p carga123 -g user --silent

# Los despliegues: la aplicacion y el simulador de la PDI.
COPY --from=build --chown=jboss:jboss /src/carga-ear/target/carga.ear \
     "$JBOSS_HOME/standalone/deployments/"
COPY --from=build --chown=jboss:jboss /src/simulador-pdi/target/simulador-pdi.war \
     "$JBOSS_HOME/standalone/deployments/"

# Compatibilidad con PaaS tipo OpenShift, que corre el contenedor con un UID
# aleatorio del grupo 0: sin esto WildFly no puede escribir en standalone/.
USER root
RUN chgrp -R 0 "$JBOSS_HOME" && chmod -R g=u "$JBOSS_HOME"
USER jboss

# Memoria acotada para que entre en una maquina de desarrollo. Necesita >= 768 MB;
# con 512 MB falla por OutOfMemoryError: Metaspace.
ENV JAVA_OPTS="-Xms64m -Xmx512m -XX:MaxMetaspaceSize=256m -XX:+UseSerialGC \
-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true \
-Djava.security.egd=file:/dev/urandom"

EXPOSE 8080 9990
# ${PORT} lo inyectan algunas PaaS; en local queda 8080.
CMD ["sh", "-c", "exec /opt/jboss/wildfly/bin/standalone.sh -c standalone-full.xml -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.http.port=${PORT:-8080}"]
