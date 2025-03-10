#!/bin/bash

######################################################
# Import the data inside the Relational Database (through quads-loader import endpoint)
######################################################

cd ../dataset/quads || exit

echo "------------------------------------------------------------ [BEGIN IMPORT RELATIONAL] ------------------------------------------------------------"

until curl --output /dev/null --silent --fail -X DELETE --location "http://localhost:8080/import/metadata"; do
  printf '.'
  sleep 1
done

find . -type f -name "*.ttl.trig" -print0 | while IFS= read -r -d '' file
do
      start=$(date +%s%3N)
      curl --location 'http://localhost:8080/import/version' \
        --header 'Content-Type: multipart/form-data' \
        --connect-timeout 60 \
        --form file=@"$file"
      end=$(date +%s%3N)
      printf "\n%s$(date +%FT%T) - [Measure] (Import STS $file):$((end-start))ms;"
done

printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import completed."

echo "------------------------------------------------------------- [END IMPORT RELATIONAL] -------------------------------------------------------------"
