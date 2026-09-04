####
# Multi-stage Dockerfile for Currency Order Service (Quarkus JVM mode)
#
# Stage 1: Build the application using Maven
# Stage 2: Package the built artifacts into a lightweight runtime image
#
# Build:
#   docker build -t currency-order-service:latest .
#
# Run:
#   docker run -i --rm -p 8080:8080 currency-order-service:latest
####

# ===== Stage 1: Build =====
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

# Copy pom.xml first to leverage Docker layer caching for dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn -B dependency:go-offline

# Copy source code and build
COPY src src
RUN mvn -B clean package -DskipTests

# ===== Stage 2: Runtime =====
FROM registry.access.redhat.com/ubi9/openjdk-21-runtime:1.24

ENV LANGUAGE='en_US:en'

# Copy the built application layers from the build stage
COPY --from=build --chown=185 /workspace/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /workspace/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /workspace/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /workspace/target/quarkus-app/quarkus/ /deployments/quarkus/

EXPOSE 8080
USER 185

ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"

ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
