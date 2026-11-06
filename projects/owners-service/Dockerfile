FROM maven:3.9.11-eclipse-temurin-17@sha256:e4a7ace3dc0d645ed97f8d9ad0b0d3f0b14fa8d150138f27f116d7105a639b82 AS build
WORKDIR /src
COPY projects/owners-service/pom.xml .
COPY projects/owners-service/src src
RUN mvn -B -ntp package

FROM eclipse-temurin:17-jre-jammy@sha256:2a9eb226abb654c9d1f91b53cf638d20cb3abb726d1b18a67909c29515545469
WORKDIR /app
COPY --from=build /src/target/owners-service-1.0.0-SNAPSHOT.jar app.jar
COPY docker/http-healthcheck /usr/local/bin/http-healthcheck
USER 10001:10001
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
