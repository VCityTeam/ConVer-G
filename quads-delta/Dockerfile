FROM maven:3-eclipse-temurin-21 AS build-create
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21

LABEL maintainer="vcity, jey.puget-gil@liris.cnrs.fr"
LABEL authors="jey.puget-gil@liris.cnrs.fr"

WORKDIR /opt/app

COPY --from=build-create /app/target/quads-delta.jar /opt/app/quads-delta.jar
COPY --from=build-create /app/target/quads-stream-delta.jar /opt/app/quads-stream-delta.jar

# Default program is the in-memory delta. To run the streaming variant, override the entrypoint, e.g.:
#   docker run --entrypoint java <image> -jar /opt/app/quads-stream-delta.jar <file1> <file2>
ENTRYPOINT [ "java", "-jar", "/opt/app/quads-delta.jar" ]
