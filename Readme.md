# SPARQL to SQL Project
This project aims to create a Java Spring parser that can convert SPARQL queries into SQL queries.
The project is part of the BD team's research efforts within the [LIRIS](https://liris.cnrs.fr/) and [VCity project](https://projet.liris.cnrs.fr/vcity/).
The aim of this POC is to query a set of city version and extract associated knowledge.

## Getting started
### Installation
This project uses Java 17 JDK + Maven and a [dockerized (make sure that Docker is installed too)](https://www.docker.com/) [PostgreSQL 15 database](https://www.postgresql.org/docs/15/index.html).
If you don't have Java 17 installed by default, I recommend that you install [SDKMAN!](https://sdkman.io/) and use this tool to set Java 17 as current session version.

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
#### Dockerized database + Java Spring

```shell
# at the root of the project
# starts the database declared inside the docker-compose.yml file
docker compose up -d

# wait until the PostgreSQL database is up
# starts the Java Spring application locally (http://localhost:8080/)
mvn spring-boot:run 
```

### Implementation
#### Entity–Relationship model
```mermaid
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

#### Flowcharts
##### Query the relational database with a SPARQL query

```mermaid
flowchart LR
    CS[Computer Scientist] --> |Sends the SPARQL query to the endpoint| SE
    SE --> |Sends the quads to the Computer Scientist| CS 
    subgraph Server
        SE --> |Sends the SPARQL query for translation| ARQ[Jena ARQ]
        ARQ --> |Sends the SQL translated query to JDBC| JDBC[Java Database Connectivity]
        JDBC --> |The filtered quads| ARQ
        ARQ --> |The filtered quads| SE
        
    end
    subgraph Database
        JDBC --> |Sends the SQL query to the database| DB[PostgreSQL]
        DB --> |Sends the result of the SQL query| JDBC
    end
```

##### Store RDF quads inside a relational database

```mermaid
flowchart LR
    CS[Computer Scientist] --> |Sends the files to the import endpoint| SE
    subgraph Server
        SE --> |Sends files to import| RIOT[Jena RIOT]
        RIOT --> |Send the quads for insertion| JDBC[Java Database Connectivity]        
    end
    subgraph Database
        JDBC --> |Sends the SQL query to the database| DB[PostgreSQL]
    end
```

### Testing
The API description is available on the [swagger-ui](http://localhost:8080/swagger-ui/index.html) at runtime.

```shell
# make sure your database is up

# starts the tests
mvn spring-boot:run test
```
