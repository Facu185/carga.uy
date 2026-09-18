# ============================================================================
#  Nodo periferico Balanzas.
#  Va en su propio contenedor a proposito: asi la llamada del central es una
#  llamada de red real y se pueden probar timeouts y caidas del nodo.
#  Usa el perfil standalone (sin JMS): no necesita mensajeria.
# ============================================================================
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-24
ARG WILDFLY_IMAGE=quay.io/wildfly/wildfly:41.0.0.Final-jdk25

FROM ${MAVEN_IMAGE} AS build
WORKDIR /src
COPY pom.xml .
COPY carga-api/pom.xml     carga-api/
COPY carga-ejb/pom.xml     carga-ejb/
COPY carga-web/pom.xml     carga-web/
COPY carga-ear/pom.xml     carga-ear/
COPY simulador-pdi/pom.xml simulador-pdi/
COPY nodo-balanzas/pom.xml nodo-balanzas/
COPY nodo-tracking/pom.xml nodo-tracking/
RUN mvn -B -ntp -pl nodo-balanzas -am dependency:go-offline || true
COPY . .
RUN mvn -B -ntp -pl nodo-balanzas -am clean package -DskipTests

FROM ${WILDFLY_IMAGE}
ENV JBOSS_HOME=/opt/jboss/wildfly
COPY --from=build --chown=jboss:jboss /src/nodo-balanzas/target/balanzas.war \
     "$JBOSS_HOME/standalone/deployments/"

USER root
RUN chgrp -R 0 "$JBOSS_HOME" && chmod -R g=u "$JBOSS_HOME"
USER jboss

ENV JAVA_OPTS="-Xms32m -Xmx192m -XX:MaxMetaspaceSize=160m -XX:+UseSerialGC \
-Djava.net.preferIPv4Stack=true -Djava.awt.headless=true"

EXPOSE 8080
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
