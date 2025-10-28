FROM maven:3-eclipse-temurin-21 AS build-query-cli
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21

LABEL maintainer="vcity, jey.puget-gil@liris.cnrs.fr"
LABEL authors="jey.puget-gil@liris.cnrs.fr"

RUN mkdir /opt/app
COPY --from=build-query-cli /app/target/quads-query-cli-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/quads-query-cli.jar
CMD [ "java", "-jar", "/opt/app/quads-query-cli.jar" ]
