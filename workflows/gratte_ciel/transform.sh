#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset || exit

mkdir -p quads/relational
mkdir -p quads/theoretical

## WORKAROUND - Replace data prefix (enabling versioning)
printf "\n%s$(date +%FT%T) - [Transformations] Replacement started."

### We set the new data prefix to a versionable compatible one
### FIXME : Autoincrement (fix entity linking)
replacement="@prefix data: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#> ."

### Use the find command to locate all files ending with "split.ttl"
find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    sed -i '/^@prefix data:/c\'"$replacement" "$file"
done

printf "\n%s$(date +%FT%T) - [Transformations] Replacement completed."

## Transform data as quads
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

docker run --name "annotate_graph-relational" -v "$PWD:/data" vcity/annotate_graph "/data/quads/relational" "/data/triples" "*" relational BSBM
docker ps --filter name=annotate_graph-relational -aq | xargs docker stop | xargs docker rm

find . -type f -name "Transition*.ttl" -print0 | while IFS= read -r -d '' file
do
    file=$(basename "$file")
    docker run --name "relational_annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/theoretical" "/data/triples" "$file" relational Metadata
done

docker run --name "relational_annotate_graph-workspace" -v "$PWD:/data" vcity/annotate_graph "/data/quads/theoretical" "/data/triples" GratteCiel_2009_2018_Workspace.ttl relational Metadata
docker ps --filter name=relational_annotate_graph-* -aq | xargs docker stop | xargs docker rm

find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    file=$(basename "$file")
    docker run --name "theoretical_annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/theoretical" "/data/triples" "$file" theoretical Grand-Lyon
done
docker ps --filter name=theoretical_annotate_graph-* -aq | xargs docker stop | xargs docker rm

#find . -type f -name "*.nt" -print0 | while IFS= read -r -d '' file
#do
#    file=$(basename "$file")
#    docker run --name "annotate_graph-$(basename "$file")" -v "$PWD:/data" vcity/annotate_graph "/data/quads/relational" "/data/triples" "$file" nt  relational Grand-Lyon
#done
#docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."
