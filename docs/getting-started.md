# Getting started

## Installation

This project uses Java 21 JDK + Maven and
a [dockerized (make sure that Docker is installed too)](https://www.docker.com/) [PostgreSQL 17 database](https://www.postgresql.org/docs/17/index.html).
If you don't have Java 21 installed by default, I recommend that you install [SDKMAN!](https://sdkman.io/) and use this
tool to set Java 21 as current session version.

> SDKMAN! is a tool for managing parallel versions of multiple Software Development Kits on most Unix based systems.

Once you have `SDKMAN!` installed, run:

```shell
sdk install java 21.0.1-amzn
sdk use java 21.0.1-amzn
```

Make sure you have Maven installed. If you don't have Maven installed, run: `sudo apt install maven`.

## Maven

### ⌛ Quads-Loader

This project:

- uses the `jena-arq` library for parsing SPARQL statements in Java,
- uses the `springdoc-openapi-starter-webmvc-ui` library to parse the Swagger API annotations and displays
  the [swagger-ui](http://localhost:8080/swagger-ui/index.html),
- needs a [PostgreSQL 17 database](https://www.postgresql.org/docs/17/index.html), so the `postgresql` driver is
  installed too.

This project has been tested with:

- `sonarqube`, assuring the code quality,
- `JaCoCo`, testing the code coverage.

### 🦆 Quads-Query

This project:

- uses the `jena-fuseki-server` Apache Jena Fuseki is a SPARQL server,
- needs a [PostgreSQL 17 database](https://www.postgresql.org/docs/17/index.html) if you use this target language, so
  the `postgresql` driver is
  installed too.

This project has been tested with: `junit-jupiter-engine`

### 🔀 Quads-Delta

This project computes the delta (additions and deletions) between two RDF datasets. It provides an
in-memory program (`RDFDelta`) and a stream-mode program (`RDFStreamDelta`) for large or
memory-constrained inputs. See [quads-delta/README.md](../quads-delta/README.md) for which one to use.

## Start the application

### ⌛ Quads-Loader

```shell
# at the root of the project
# starts the database declared inside the docker-compose.yml file
docker compose up -d

# if you want to hack the import program
cd quads-loader

## wait until the PostgreSQL database is up
## starts the Java Spring application locally (http://localhost:8080/)
## the connection settings are optional, they default to the docker-compose database
## (jdbc:postgresql://localhost:5432/converg, postgres/password)
SPRING_DATASOURCE_URL=<url> SPRING_DATASOURCE_USERNAME=<username> SPRING_DATASOURCE_PASSWORD=<password> \
  java -jar target/quads-loader-0.0.1-SNAPSHOT.jar
```

### 🦆 Quads-Query

```shell
# at the root of the project
# starts the database declared inside the docker-compose.yml file
docker compose up -d

# if you want to hack the import program
cd quads-query

## wait until the PostgreSQL database is up
# build the project
mvn package

## starts the Apache Jena Fuseki server locally (http://localhost:8081/)
## `mvn package` produces the runnable shaded jar at target/quads-query-1.0-SNAPSHOT.jar
## the connection settings are optional, they default to the docker-compose database
DATASOURCE_URL=<url> DATASOURCE_USERNAME=<username> DATASOURCE_PASSWORD=<password> \
  java -jar target/quads-query-1.0-SNAPSHOT.jar

## optional environment variables:
##   TARGET_LANG=<SQL>                            target query language (only SQL is supported, default: SQL)
##   CONDENSED_MODE=<true|false>                  condensed vs flat versioned representation (default: true)
##   ENTAILMENT_REGIME=<NONE|RDFS|OWL_LITE>       entailment regime (default: NONE)
##   SWRL_RULES=<path to a SWRL rules ontology>   SWRL rules to apply, see inference.md
```

See [inference.md](inference.md) for what the `ENTAILMENT_REGIME` and `SWRL_RULES` options enable.

## Testing

### Swagger

The API description is available on the [swagger-ui](http://localhost:8080/swagger-ui/index.html) at runtime.

### Tests

```shell
# make sure the databases are up (docker compose up -d)

# run the unit tests
mvn test

# run the unit + integration tests (integration tests require the databases)
mvn verify

# run the integration tests against the flattened representation
mvn verify -Pflat
```

### Code quality and coverage

The code coverage and quality is available on the [Sonarqube server](http://localhost:9000) after running a sonar
inspection.
