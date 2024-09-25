#!/bin/bash

######################################################
# Import the data inside the Relational Database (through quads-loader import endpoint)
######################################################

cd ../dataset/quads/relational || exit

echo "------------------------------------------------------------ [BEGIN IMPORT RELATIONAL] ------------------------------------------------------------"
printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import started."

## BSBM tagged data
### Import the versions of BSBM
printf "\n%s$(date +%FT%T) - [quads-loader] Versions import started."

find . -type f -name "*.ttl.relational.trig" -print0 | while IFS= read -r -d '' file
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

printf "\n%s$(date +%FT%T) - [quads-loader] Versions import completed."

### Import the version transitions

#printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import started."
#
#printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import completed."
printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import completed."

echo "------------------------------------------------------------- [END IMPORT RELATIONAL] -------------------------------------------------------------"
