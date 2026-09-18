# ============================================================================
#  Nodo periferico Tracking: proceso Java autonomo, sin servidor de aplicaciones.
#  Emite posiciones contra el endpoint de ingesta del central.
# ============================================================================
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-24
ARG JRE_IMAGE=eclipse-temurin:24-jre

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
COPY . .
RUN mvn -B -ntp -pl nodo-tracking -am clean package -DskipTests

FROM ${JRE_IMAGE}
WORKDIR /app
COPY --from=build /src/nodo-tracking/target/nodo-tracking.jar /app/nodo-tracking.jar
ENV TRACKING_INTERVALO_MS=5000
CMD ["java", "-jar", "/app/nodo-tracking.jar"]
