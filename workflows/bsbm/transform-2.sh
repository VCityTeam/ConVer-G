#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset || exit

echo "---------------------------------------------------------------- [BEGIN TRANSFORM] ----------------------------------------------------------------"

## Transform data as quads
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

docker run --name "annotate_graph-theoretical-data" -v "$PWD:/data" vcity/annotate_graph "/data/quads/data/theoretical" "/data/triples/data" "*" theoretical BSBM
docker run --name "annotate_graph-relational-data" -v "$PWD:/data" vcity/annotate_graph "/data/quads/data/relational" "/data/triples/data" "*" relational BSBM

docker run --name "annotate_graph-theoretical-alt" -v "$PWD:/data" vcity/annotate_graph "/data/quads/alt/theoretical" "/data/triples/alt" "*" theoretical BSBM-alt
docker run --name "annotate_graph-relational-alt" -v "$PWD:/data" vcity/annotate_graph "/data/quads/alt/relational" "/data/triples/alt" "*" relational BSBM-alt

docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

echo "----------------------------------------------------------------- [END TRANSFORM] -----------------------------------------------------------------"
