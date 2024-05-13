#!/bin/bash

######################################################
# Import the data inside the Triple Store
######################################################

cd ../dataset/quads/theoretical || exit

echo "-------------------------------------------------------------- [BEGIN IMPORT TRIPLE] --------------------------------------------------------------"
printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import started."

## BSBM tagged data
find . -type f -name "*.nq" -print0 | while IFS= read -r -d '' file
do
    printf "%s\n$(date +%FT%T) - [Triple Store] $file."
    start=$(date +%s%3N)
    curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
      --header 'Content-Type:text/x-nquads' \
      --connect-timeout 60 \
      --data-binary @"$file"
      end=$(date +%s%3N)
      printf "\n%s$(date +%FT%T) - [Measure] (Import BG $file):$((end-start))ms;"
done

printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import completed."
echo "--------------------------------------------------------------- [END IMPORT TRIPLE] ---------------------------------------------------------------"
