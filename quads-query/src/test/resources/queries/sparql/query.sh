#!/bin/bash

######################################################
# Find all the queries and send them to the quads-query and Blazegraph servers
######################################################

printf "\n%s$(date +%FT%T) - [quads-query] Query started."

find . -type f -name "sts*" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [quads-query] Query $file"
    name=$(basename "$file")

    start_query_relational=$(date +%s%3N)

    curl --location http://localhost:8081/rdf/sparql -X POST --data-binary @"$file" \
      --header 'Content-Type: application/sparql-query' \
      --output "sts-$name.json" \
      --header 'Accept: application/sparql-results+json'

    end_query_relational=$(date +%s%3N)
    printf "[Measure] {Query relational} Query %s duration: %s ms\n" "$file" "$((end_query_relational-start_query_relational))s"
done

printf "\n%s$(date +%FT%T) - [quads-query] Query completed."


printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query started."

find . -type f -name "blazegraph*" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query $file"
    name=$(basename "$file")

    start_query_triple=$(date +%s%3N)

    curl --location http://localhost:9999/blazegraph/namespace/kb/sparql -X POST --data-binary @"$file" \
      --header 'Content-Type: application/sparql-query' \
      --output "blazegraph-$name.json" \
      --header 'Accept: application/sparql-results+json'

    end_query_triple=$(date +%s%3N)
    printf "[Measure] {Query triple} Query %s duration: %s ms\n" "$file" "$((end_query_triple-start_query_triple))s"
done

printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query completed."

