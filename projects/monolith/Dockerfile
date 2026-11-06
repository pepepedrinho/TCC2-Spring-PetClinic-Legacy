FROM maven:3.9.11-eclipse-temurin-8@sha256:e3c149f44c95b0e9dd131862b3df67b3f061f7f6f3898a87b170564b3a943611 AS build
WORKDIR /src
COPY projects/monolith/pom.xml .
COPY projects/monolith/src src
RUN mvn -B -ntp package -Dcobertura.skip=true -Dmaven.gitcommitid.skip=true

FROM eclipse-temurin:8-jre-jammy@sha256:48a8129eda9d0c8a81c86d1837c125b63a5ee8558ba9565b6066516ae72a8d47
WORKDIR /app
COPY --from=build /src/target/spring-petclinic-1.5.1.jar app.jar
COPY docker/http-healthcheck /usr/local/bin/http-healthcheck
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
