FROM maven:3-eclipse-temurin-21 AS build-query
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21

LABEL maintainer="vcity, jey.puget-gil@liris.cnrs.fr"
LABEL authors="jey.puget-gil@liris.cnrs.fr"

RUN mkdir /opt/app
COPY --from=build-query /app/target/quads-query-1.0-SNAPSHOT.jar /opt/app/quads-query.jar
CMD [ "java", "-jar", "/opt/app/quads-query.jar" ]
