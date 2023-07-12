FROM maven AS build
WORKDIR /app
COPY . .
RUN ["mvn", "package", "-Dmaven.test.skip=true"]

FROM eclipse-temurin:17
MAINTAINER vcity
RUN mkdir /opt/app
COPY --from=build /app/target/sparql-to-sql-0.0.1-SNAPSHOT.jar /opt/app/sparql-to-sql.jar
CMD [ "java", "-jar", "/opt/app/sparql-to-sql.jar" ]