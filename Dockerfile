FROM maven:3.9.12-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src src
RUN mvn -B verify

FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app
COPY --from=build --chown=spring:spring /workspace/target/purchase-order-connector-*.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

