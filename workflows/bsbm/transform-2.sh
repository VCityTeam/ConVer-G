#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset || exit

echo "---------------------------------------------------------------- [BEGIN TRANSFORM] ----------------------------------------------------------------"

mkdir -p quads/relational
mkdir -p quads/theoretical

## Transform data as quads
### Use the find command to locate all files ending with "split.ttl"
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

find . -type f -name "*.ttl" -print0 | while IFS= read -r -d '' file
do
    file=$(basename "$file")
    docker run --name "annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/relational" "/data/triples" "$file" ttl relational BSBM
done
docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

find . -type f -name "version*split.ttl" -print0 | while IFS= read -r -d '' file
do
    file=$(basename "$file")
    docker run --name "annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/theoretical" "/data/triples" "$file" ttl theoretical BSBM
done
docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

find . -type f -name "*.nt" -print0 | while IFS= read -r -d '' file
do
    file=$(basename "$file")
    docker run --name "annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/relational" "/data/triples" "$file" nt  relational BSBM
done
docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

## Adds data in test workspace
printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace started."
### Adds BSBM tagged data in test workspace

rm -rf ../quads-importer/src/test/resources/dataset
mkdir -p ../quads-importer/src/test/resources/dataset
cp quads/relational/* ../quads-importer/src/test/resources/dataset

printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace completed."
echo "----------------------------------------------------------------- [END TRANSFORM] -----------------------------------------------------------------"
