#!/bin/bash

######################################################
# Import the data inside the Relational Database (through SPARQL-to-SQL import endpoint)
######################################################

cd ../dataset/quads/relational || exit

printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Dataset import started."

## BSBM tagged data
### Import the versions of BSBM
printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Versions import started."

find . -type f -name "*.split.ttl.relational.nq" -print0 | while IFS= read -r -d '' file
do
    printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Version $file"
    curl --location 'http://localhost:8080/import/version' \
      --header 'Content-Type: multipart/form-data' \
      --form file=@"$file"
done

printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Versions import completed."

### Import the version transitions

#printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Version transitions import started."
#
#printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Version transitions import completed."
printf "\n%s$(date +%FT%T) - [SPARQL-to-SQL] Dataset import completed."
