#!/bin/bash

######################################################
# Import the data inside the Triple Store
######################################################

cd ../dataset/quads/theoretical || exit

printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import started."

## Grand-Lyon tagged data
find . -type f -name "*.trig" -print0 | while IFS= read -r -d '' file
do
    printf "%s\n$(date +%FT%T) - [Triple Store] $file."
    curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
      --header 'Content-Type:application/x-trig' \
      --data-binary @"$file"
done

printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import completed."


