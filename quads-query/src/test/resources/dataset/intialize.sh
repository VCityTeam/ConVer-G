#!/bin/bash

echo "Importing datasets..."

cd relational || exit

find . -type f -name "*.ttl.trig" -print0 | while IFS= read -r -d '' file
do
      printf "\n%s$(date +%FT%T) - [quads-loader] Version $file"
      start=$(date +%s%3N)
      curl --location 'http://localhost:8080/import/version' \
        --header 'Content-Type: multipart/form-data' \
        --connect-timeout 60 \
        --form file=@"$file"
      end=$(date +%s%3N)
      printf "\n%s$(date +%FT%T) - [Measure] (Import STS $file):$((end-start))ms;"
done

cd ../theoretical || exit

find . -type f -name "*.ttl.trig" -print0 | while IFS= read -r -d '' file
do
      printf "%s\n$(date +%FT%T) - [Triple Store] $file."
      start=$(date +%s%3N)
      curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
            --header 'Content-Type:application/x-trig' \
            --connect-timeout 60 \
            --data-binary @"$file"
      end=$(date +%s%3N)
      printf "\n%s$(date +%FT%T) - [Measure] (Import BG $file):$((end-start))ms;"
done