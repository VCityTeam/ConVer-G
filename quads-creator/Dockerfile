FROM maven:3-eclipse-temurin-21 AS build-create
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21

LABEL maintainer="vcity, jey.puget-gil@liris.cnrs.fr"
LABEL authors="jey.puget-gil@liris.cnrs.fr"

WORKDIR /opt/app

COPY --from=build-create /app/target/quads-creator-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/quads-creator.jar
ENTRYPOINT [ "java", "-jar", "/opt/app/quads-creator.jar" ]
