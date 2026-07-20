# Architecture

## Ontology

![The graph versioning ontology](https://www.ldf.fi/service/rdf-grapher?rdf=%40prefix+vers%3A+%3Chttps%3A%2F%2Fgithub.com%2FVCityTeam%2FConVer-G%2F%3E+.%0D%0A%40prefix+rdf%3A+%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23%3E+.%0D%0A%40prefix+rdfs%3A+%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%23%3E+.%0D%0A%40prefix+purl%3A+%3Chttp%3A%2F%2Fpurl.org%2Fdc%2Felements%2F1.1%3E+.%0D%0A%40prefix+prov%3A+%3Chttp%3A%2F%2Fwww.w3.org%2Fns%2Fprov%23%3E+.%0D%0A%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Crdf%3Atype%3E+prov%3AEntity+.%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Crdfs%3Acomment%3E+%22A+versioned+graph+name%22+.%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Crdfs%3Alabel%3E+%22Versioned+named+graph%22%40en+.%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Crdfs%3Alabel%3E+%22Graphe+nomm%C3%A9+versionn%C3%A9%22%40fr+.%0D%0A%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Cprov%3AatLocation%3E+prov%3ALocation+.%0D%0A%0D%0A%3Cvers%3AVersioned-Named-Graph%3E+%3Cprov%3AspecializationOf%3E+vers%3ANamed-Graph+.%0D%0A%3Cvers%3ANamed-Graph%3E+%3Crdf%3Atype%3E+prov%3AEntity+.%0D%0A%3Cvers%3ANamed-Graph%3E+%3Crdfs%3Acomment%3E+%22A+graph+name%22+.%0D%0A%3Cvers%3ANamed-Graph%3E+%3Crdfs%3Alabel%3E+%22Named+graph%22%40en+.%0D%0A%3Cvers%3ANamed-Graph%3E+%3Crdfs%3Alabel%3E+%22Graphe+nomm%C3%A9%22%40fr+.%0D%0A%0D%0A%3Cvers%3AVersioning%3E+%3Crdf%3Atype%3E+%3Chttp%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23Ontology%3E+.%0D%0A%3Cvers%3AVersioning%3E+%3Cpurl%3Atitle%3E+%22The+graph+versioning+vocabulary%22+.%0D%0A%3Cvers%3AVersioning%3E+%3Cpurl%3Adate%3E+%222024-08-26%22+.%0D%0A%3Cvers%3AVersioning%3E+%3Cpurl%3Adescription%3E+%22RDF+schema+of+the+graph+versioning+vocabulary+terms%22+.&from=ttl&to=png)

## Conceptual model

```mermaid
erDiagram
    VersionedQuad |{--|{ Version: "bitstring index"
    VersionedQuad ||--|{ VersionedNamedGraph: "(named_graph, bitstring index)"
    Version ||--|{ VersionedNamedGraph: "index"
    VersionedQuad {
        text subject
        text predicate
        text object
        text named_graph
        bitstring validity
    }
    VersionedNamedGraph {
        text versioned_named_graph
        int index_version
        text named_graph
    }
    Version {
        int index_version
        text message
        timestamptz transaction_time_start
        timestamptz transaction_time_end
    }
    Metadata {
        text subject
        text predicate
        text object
    }
```

## Entity–Relationship model

```mermaid
erDiagram
    VersionedQuad ||--|{ ResourceOrLiteral: "subject"
    VersionedQuad ||--|{ ResourceOrLiteral: "object"
    VersionedQuad ||--|{ ResourceOrLiteral: "predicate"
    VersionedQuad ||--|{ ResourceOrLiteral: "named graph"
    VersionedNamedGraph ||--|{ ResourceOrLiteral: "named graph"
    VersionedNamedGraph ||--|{ ResourceOrLiteral: "versioned named graph"
    Metadata }|--|{ ResourceOrLiteral: "subject"
    Metadata }|--|{ ResourceOrLiteral: "object"
    Metadata }|--|{ ResourceOrLiteral: "predicate"
    VersionedQuad ||--|{ VersionedNamedGraph: "foreign key"
    VersionedQuad {
        int id_subject PK, FK
        int id_predicate PK, FK
        int id_object PK, FK
        int id_named_graph FK
        bitstring validity
    }
    VersionedNamedGraph {
        int id_versioned_named_graph PK, FK
        int index_version
        int id_named_graph FK
    }
    ResourceOrLiteral {
        int id_resource_or_literal PK, FK
        text name
        string type "Not null if literal"
    }
    Version {
        int index_version "PK, (FK)"
        text message
        timestamptz transaction_time_start
        timestamptz transaction_time_end
    }
    Metadata {
        int id_subject PK, FK
        int id_predicate PK, FK
        int id_object PK, FK
    }
```

## Flowcharts

### Translation of a SPARQL query to SQL

```mermaid
flowchart TD
    %% Main Control Flow
    n1([SPARQL Query]) --> A("Compile SPARQL &<br>Initialize an empty SQL query")
    A --> C("Get current operator from the algebraic plan")
    C --> D{"Is operator composite?"}
    
    D -- No --> n3
    D -- Yes --> E{"Operator Type?"}

    %% Subgraphs for Operator Processing
    subgraph s1 [Create Quad Pattern SQL Fragment]
        direction TB
        n3("Translate quads to SQL SELECT")
        n3 --> n4("Filter rows based on given resources")
        n4 --> J3("Add the Quad Pattern fragment to the SQL query")
    end
    
    subgraph s4 [Create Join SQL Fragment]
        direction TB
        F1("Recursively evaluate left & right parts")
        F1 --> n13{"Are there more<br>common variables?"}
        n13 -- Yes --> IE1{"Are variable representations equal?"}
        IE1 -- No --> Jo2("Unify to a lower representation")
        Jo2 --> Jo1("Build join equality condition")
        IE1 -- Yes --> Jo1
        Jo1 --> n13
        n13 -- No --> F2("Add the Join fragment to the SQL query")
    end
    
    subgraph s2 [Create Group SQL Fragment]
        direction TB
        n5{"Is grouped variable condensed?"}
        n5 -- Yes --> n6("Flatten representation")
        n6 --> n10
        n5 -- No --> n10("Build aggregations<br>(e.g., COUNT, SUM)")
        n10 --> n11("Build GROUP BY variables")
        n11 --> n12("Add the Group fragment to the SQL query")
    end

    %% Operator Dispatcher
    E -- OpJoin --> F1
    E -- OpGroup --> n5
    E -- ... --> H("Process other composite operators")

    %% Final Control Flow
    J3 --> K
    n12 --> K
    F2 --> K
    H --> K{"Are there more operators to process?"}
    K -- Yes --> C
    K -- No --> L("Return the built SQL query")
    L --> n2([SQL Query])
```

### Query the relational database with a SPARQL query

```mermaid
flowchart BT
    CS[Computer Scientist] -->|Sends the SPARQL query to the endpoint| SE
    SE -->|Sends the quads to the Computer Scientist| CS
    subgraph Server
        SE -->|Sends the SPARQL query for translation| ARQ[SPARQL to SQL translator]
        ARQ -->|Sends the SQL translated query to JDBC| JDBC[Java Database Connectivity]
        JDBC -->|The filtered quads| ARQ
        ARQ -->|The filtered quads| SE

    end
    subgraph Database
        JDBC -->|Sends the SQL query to the database| DB[PostgreSQL]
        DB -->|Sends the result of the SQL query| JDBC
    end
```

### Store RDF quads inside a relational database

```mermaid
flowchart TB
    CS[Computer Scientist] -->|Sends the files to the import endpoint| SE
    SE -->|Returns the version number via HTTP| CS
    
    subgraph Server
        SE -->|Sends files to import| RIOT[Jena RIOT]
        RIOT -->|Send the quads for insertion| JDBC[Java Database Connectivity]
        
        JDBC -->|Sends the version number| SE
    end
    
    subgraph Database
        JDBC -->|Sends the SQL query to the database| DB[PostgreSQL]
        DB -->|Sends the version information| JDBC
    end
```
