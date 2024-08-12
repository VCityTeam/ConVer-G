FROM maven:3-eclipse-temurin-21 AS build-query
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:21
LABEL maintainer="vcity"
ARG DATASOURCE_URL
ARG DATASOURCE_USERNAME
ARG DATASOURCE_PASSWORD
ENV DATASOURCE_URL=${DATASOURCE_URL}
ENV DATASOURCE_USERNAME=${DATASOURCE_USERNAME}
ENV DATASOURCE_PASSWORD=${DATASOURCE_PASSWORD}

RUN mkdir /opt/app
COPY --from=build-query /app/target/quads-query-1.0-SNAPSHOT-jar-with-dependencies.jar /opt/app/quads-query.jar
CMD [ "java", "-jar", "/opt/app/quads-query.jar" ]
