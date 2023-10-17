#!/bin/bash

######################################################
# Import the data inside the Triple Store
######################################################

cd ../dataset || exit

echo "[Triple Store] Dataset import started."

## Villeurbanne tagged data
find . -type f -name "*.nq" -print0 | while IFS= read -r -d '' file
do
    echo "[Triple Store] $file."
    curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
      --header 'Content-Type:text/x-nquads' \
      --data-binary @"$file"
done

echo "[Triple Store] Dataset import completed."


