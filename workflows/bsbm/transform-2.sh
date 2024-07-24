#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset || exit

echo "---------------------------------------------------------------- [BEGIN TRANSFORM] ----------------------------------------------------------------"

mkdir -p quads/relational
mkdir -p quads/theoretical

## Transform data as quads
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

docker run --name "annotate_graph-theoretical" -v "$PWD:/data" vcity/annotate_graph "/data/quads/theoretical" "/data/triples" "*" ttl theoretical BSBM
docker run --name "annotate_graph-relational" -v "$PWD:/data" vcity/annotate_graph "/data/quads/relational" "/data/triples" "*" ttl relational BSBM
docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

## Adds data in test metadata
printf "\n%s$(date +%FT%T) - [Transformations] Copy for test metadata started."
### Adds BSBM tagged data in test metadata

rm -rf ../quads-loader/src/test/resources/dataset
mkdir -p ../quads-loader/src/test/resources/dataset
cp quads/relational/* ../quads-loader/src/test/resources/dataset

printf "\n%s$(date +%FT%T) - [Transformations] Copy for test metadata completed."
echo "----------------------------------------------------------------- [END TRANSFORM] -----------------------------------------------------------------"
