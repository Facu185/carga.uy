# ============================================================================
#  Generador de carga de posiciones (P4, ADR-0008). Proceso Java autonomo.
#  No arranca con "docker compose up": esta en el perfil "carga" del compose.
# ============================================================================
ARG MAVEN_IMAGE=maven:3.9-eclipse-temurin-24
ARG JRE_IMAGE=eclipse-temurin:24-jre

FROM ${MAVEN_IMAGE} AS build
WORKDIR /src
COPY pom.xml .
COPY carga-api/pom.xml       carga-api/
COPY carga-ejb/pom.xml       carga-ejb/
COPY carga-web/pom.xml       carga-web/
COPY carga-ear/pom.xml       carga-ear/
COPY simulador-pdi/pom.xml   simulador-pdi/
COPY nodo-balanzas/pom.xml   nodo-balanzas/
COPY nodo-tracking/pom.xml   nodo-tracking/
COPY carga-generador/pom.xml carga-generador/
COPY . .
RUN mvn -B -ntp -pl carga-generador -am clean package -DskipTests

FROM ${JRE_IMAGE}
WORKDIR /app
COPY --from=build /src/carga-generador/target/carga-generador.jar /app/carga-generador.jar
CMD ["java", "-jar", "/app/carga-generador.jar"]
