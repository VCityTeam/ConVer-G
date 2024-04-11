#!/bin/bash

######################################################
# Find all the queries and send them to the SPARQL-to-SQL and Blazegraph servers
######################################################

printf "\n%s$(date +%FT%T) - [Query - SPARQL-to-SQL] Query started."

find . -type f -name "sts*" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [Query - SPARQL-to-SQL] Query $file"
    name=$(basename "$file")

    curl --location http://localhost:8081/rdf/sparql -X POST --data-binary @"$file" \
      --header 'Content-Type: application/sparql-query' \
      --output "sts-$name.json" \
      --header 'Accept: application/sparql-results+json'
done

printf "\n%s$(date +%FT%T) - [Query - SPARQL-to-SQL] Query completed."


printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query started."

find . -type f -name "blazegraph*" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query $file"
    name=$(basename "$file")

    curl --location http://localhost:9999/blazegraph/namespace/kb/sparql -X POST --data-binary @"$file" \
      --header 'Content-Type: application/sparql-query' \
      --output "blazegraph-$name.json" \
      --header 'Accept: application/sparql-results+json'
done

printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query completed."

