FROM maven:3.9.11-eclipse-temurin-17@sha256:e4a7ace3dc0d645ed97f8d9ad0b0d3f0b14fa8d150138f27f116d7105a639b82 AS dependencies
WORKDIR /src
COPY projects/owners-service/pom.xml .
RUN mvn -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
    -Dartifact=org.hsqldb:hsqldb:2.3.5 -DoutputDirectory=/legacy \
 && mvn -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
    -Dartifact=org.hsqldb:sqltool:2.3.5 -DoutputDirectory=/legacy \
 && mvn -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy \
    -Dartifact=com.h2database:h2:2.3.232 -DoutputDirectory=/owners

FROM eclipse-temurin:8-jre-jammy@sha256:48a8129eda9d0c8a81c86d1837c125b63a5ee8558ba9565b6066516ae72a8d47 AS legacy-db
COPY --from=dependencies /legacy /opt/db
COPY projects/monolith/src/main/resources/db/hsqldb /opt/db
COPY docker/db-entrypoint docker/legacy-check.sql /opt/db/
RUN mkdir -p /var/lib/petclinic/db && chown -R 10001:10001 /var/lib/petclinic
USER 10001:10001
EXPOSE 9001
ENTRYPOINT ["bash", "/opt/db/db-entrypoint", "legacy"]

FROM eclipse-temurin:17-jre-jammy@sha256:2a9eb226abb654c9d1f91b53cf638d20cb3abb726d1b18a67909c29515545469 AS owners-db
COPY --from=dependencies /owners /opt/db
COPY projects/owners-service/src/main/resources/schema.sql /opt/db/schema.sql
COPY docker/db-entrypoint docker/owners-check.sql /opt/db/
RUN mkdir -p /var/lib/petclinic/db && chown -R 10001:10001 /var/lib/petclinic
USER 10001:10001
EXPOSE 9092
ENTRYPOINT ["bash", "/opt/db/db-entrypoint", "owners"]
