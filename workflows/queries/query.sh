#!/bin/bash

######################################################
# Find all the queries and send them to the quads-query and Blazegraph servers
######################################################

log_folder="."

blazegraph_host=$1
quads_query_host=$2

if [ "$#" -lt 2 ] ; then
    echo "Usage: $0 <Blazegraph Host> <quads-query Host> [Log Folder]"
    exit 1
fi

if [ "$#" -eq 3 ] ; then
    echo "Logs will be saved in the folder $3"
    log_folder="$3"
    mkdir -p "$log_folder"
fi

printf "\n%s$(date +%FT%T) - [quads-query] Query started."

find . -type f -name "converg*.rq" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [quads-query] Query $file"
    name=$(basename "$file")

    start_query_relational=$(date +%s%3N)

    content=$(cat "$file")
    curl --location "http://$quads_query_host:8081/rdf/query" \
      --header 'Content-Type: application/sparql-query' \
      --header 'Accept: application/sparql-results+json' \
      --output "$log_folder/$name.json" \
      --data "$content"

    end_query_relational=$(date +%s%3N)
    printf "[Measure] (Query ConVer-G Query %s): %s ms\n" "$file" "$((end_query_relational-start_query_relational))s"
done

printf "\n%s$(date +%FT%T) - [quads-query] Query completed."


printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query started."

find . -type f -name "blazegraph*.rq" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query $file"
    name=$(basename "$file")

    start_query_triple=$(date +%s%3N)

    curl --location "http://$blazegraph_host:9999/blazegraph/namespace/kb/sparql" -X POST --data-binary @"$file" \
      --header 'Content-Type: application/sparql-query' \
      --output "$log_folder/$name.json" \
      --header 'Accept: application/sparql-results+json'

    end_query_triple=$(date +%s%3N)
    printf "[Measure] (Query triple Query %s): %s ms\n" "$file" "$((end_query_triple-start_query_triple))s"
done

printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query completed."

