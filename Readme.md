# SPARQL to SQL Project
This project aims to create a Java Spring parser that can convert SPARQL queries into SQL queries.
The project is part of the BD team's research efforts within the [LIRIS](https://liris.cnrs.fr/) and [VCity project](https://projet.liris.cnrs.fr/vcity/).
The aim of this POC is to query a set of city version and extract associated knowledge.

## Getting started
### Installation
This project uses Java 17 JDK + Maven and a [dockerized (make sure that Docker is installed too)](https://www.docker.com/) [PostgreSQL 15 database](https://www.postgresql.org/docs/15/index.html).
If you don't have Java 17 installed by default, I recommend that you install [SDKMAN!](https://sdkman.io/).

> SDKMAN! is a tool for managing parallel versions of multiple Software Development Kits on most Unix based systems.

Once you have `SDKMAN!` installed, run:
```shell
sdk install java 17.0.7-amzn
sdk use java 17.0.7-amzn
```

Make sure you have Maven installed. If you don't have Maven installed, run: `sudo apt install maven`.

### Maven dependencies
This project uses:
- the `jena-arq 4.8.0` library for parsing SPARQL statements in Java,
- the `springdoc-openapi-starter-webmvc-ui 2.1.0` library to parse the Swagger API annotations and displays the [swagger-ui](http://localhost:8080/swagger-ui/index.html),
- a [Dockerized PostgreSQL 15 database](https://www.postgresql.org/docs/15/index.html), so the `postgresql` driver is installed too.

### Start the application

```shell
# at the root of the project
# starts the database declared inside the docker-compose.yml file
docker compose up -d

# wait until the PostgreSQL database is up
# starts the Java Spring application locally (http://localhost:8080/)
mvn spring-boot:run 
```

### Implementation

```mermaid
---
title: ER model
---
erDiagram
    VersionedQuad |{--|| ResourceOrLiteral: extends
    VersionedQuad |{--|| NamedGraph: extends
    VersionedQuad |{--|| Commit: extends
    VersionedQuad {
        int id_subject PK, FK
        int id_property PK, FK
        int id_object PK, FK
        int id_named_graph FK
        bitstring validity
    }
    NamedGraph {
        int id_named_graph PK, FK
        int name
    }
    ResourceOrLiteral {
        int id_resource_or_literal PK, FK
        Text name
        string type "Not null if literal"
    }
    Commit {
        int id_commit PK, FK
        Varchar(255) message
        timestamptz date_commit
    }
```
```mermaid
---
title: Query the relational database with SPARQL
---
sequenceDiagram
    actor CS as Computer Scientist
    CS ->> Query Endpoint: N-Quads file
    Query Endpoint -->> STS: Sends the SPARQL query to the parser
    STS -->> JPA: Sends the SQL query to the JPA
    JPA -->> PostgreSQL: Queries the database with generated SQL query
    JPA -->> STS: Returns the filtered quads
    STS -->> CS: Sends the result of the query (the filtered Quads)
```
```mermaid
---
title: Store RDF quads inside a relational database
---
sequenceDiagram
    actor CS as Computer Scientist
    CS->>Import Endpoint: RDF data
    Import Endpoint-->>Jena ARQ: Sends the data to the Jena ARQ parser
    Jena ARQ-->>JPA: Parses the RDF data and sends it to the JPA
    JPA-->>PostgreSQL: Saves the quads inside the database as a new version
```

### Testing
The API description is available on the [swagger-ui](http://localhost:8080/swagger-ui/index.html) at runtime.

```shell
# make sure your database is up

# starts the tests
mvn spring-boot:run test
```
