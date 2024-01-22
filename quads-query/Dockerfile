FROM maven:3-eclipse-temurin-21 AS build-query
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21
LABEL maintainer="vcity"
RUN mkdir /opt/app
COPY --from=build-query /app/target/sparql-to-sql-query-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/sparql-to-sql-query.jar
CMD [ "java", "-jar", "/opt/app/sparql-to-sql-query.jar" ]