FROM maven:3-eclipse-temurin-21 AS build-loader
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21

LABEL maintainer="vcity, jey.puget-gil@liris.cnrs.fr"
LABEL authors="jey.puget-gil@liris.cnrs.fr"

RUN mkdir /opt/app
COPY --from=build-loader /app/target/quads-loader-0.0.1-SNAPSHOT.jar /opt/app/quads-loader.jar
CMD [ "java", "-jar", "/opt/app/quads-loader.jar" ]