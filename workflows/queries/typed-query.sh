#!/bin/bash

######################################################
# Find all the queries and send them to the host with the given representation
######################################################

log_folder="."

host=$1
converg_or_blazegraph=$2
number_of_queries=$3
log_folder=${4:"."}

JSON_LOG='{"component":"%s","query":"%s","try":"%s","duration":"%s"}\n'

if [ "$#" -lt 3 ] ; then
    echo "Usage: $0 <Host> <converg or blazegraph> <number of queries> ?<log folder>"
    exit 1
fi

if [ "$converg_or_blazegraph" = "converg" ] ; then
  printf "\n%s$(date +%FT%T) - [quads-query] Query started."

  find . -type f -name "converg*.rq" -print0 | while IFS= read -r -d '' file
  do
      printf "\n%s$(date +%FT%T) - [quads-query] Query $file"
      name=$(basename "$file")

      for i in $(seq 1 "$number_of_queries");
      do
          start_query_relational=$(date +%s%3N)
          content=$(cat "$file")
          curl --location "http://$host:8081/rdf/query" \
            --header 'Content-Type: application/sparql-query' \
            --header 'Accept: application/sparql-results+json' \
            --output "$log_folder/$name.json" \
            --data "$content"

          end_query_relational=$(date +%s%3N)
          printf "$JSON_LOG" "QuaQue" "$file" "$i" "$((end_query_relational-start_query_relational))ms"
      done
  done

  printf "\n%s$(date +%FT%T) - [quads-query] Query completed."
fi

if [ "$converg_or_blazegraph" = "blazegraph" ] ; then
  printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query started."

  find . -type f -name "blazegraph*.rq" -print0 | while IFS= read -r -d '' file
  do
      printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query $file"
      name=$(basename "$file")

      for i in $(seq 1 "$number_of_queries");
      do
          start_query_triple=$(date +%s%3N)
          content=$(cat "$file")
          curl --location "http://$host:9999/blazegraph/namespace/kb/sparql" -X POST --data-binary @"$file" \
                --header 'Content-Type: application/sparql-query' \
                --output "$log_folder/$name.json" \
                --header 'Accept: application/sparql-results+json'

          end_query_triple=$(date +%s%3N)
          printf "$JSON_LOG" "Blazegraph" "$file" "$i" "$((end_query_triple-start_query_triple))ms"
      done
  done

  printf "\n%s$(date +%FT%T) - [Query - Blazegraph] Query completed."
fi
