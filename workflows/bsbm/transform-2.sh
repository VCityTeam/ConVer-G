#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset || exit

echo "---------------------------------------------------------------- [BEGIN TRANSFORM] ----------------------------------------------------------------"

## Transform data as quads
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

# Blazegraph
docker run --name "annotate_graph-theoretical-data" -v "$PWD:/data" vcity/annotate_graph "/data/quads/data/theoretical" "/data/triples/data" "*" theoretical BSBM
# ConVer-G
docker run --name "annotate_graph-relational-data" -v "$PWD:/data" vcity/annotate_graph "/data/quads/data/relational" "/data/triples/data" "*" relational BSBM
# OSTRICH
ostrich_files=($PWD/triples/data/*)
num_files=${#ostrich_files[@]}

for ((i=0; i<num_files-1; i++)); do
    file1="${ostrich_files[$i]}"
    file2="${ostrich_files[$((i+1))]}"

    filename1=$(basename "$file1")
    filename2=$(basename "$file2")

    docker run --name "compute-deltas-$i" -v "$PWD/triples/data:/data" vcity/quads-delta "$filename1" "$filename2"
    echo "$filename1 $filename2"
done

docker ps --filter name=compute-deltas-* -aq | xargs docker stop | xargs docker rm

# Blazegraph
docker run --name "annotate_graph-theoretical-alt" -v "$PWD:/data" vcity/annotate_graph "/data/quads/alt/theoretical" "/data/triples/alt" "*" theoretical BSBM-alt
# ConVer-G
docker run --name "annotate_graph-relational-alt" -v "$PWD:/data" vcity/annotate_graph "/data/quads/alt/relational" "/data/triples/alt" "*" relational BSBM-alt

docker ps --filter name=annotate_graph-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

echo "----------------------------------------------------------------- [END TRANSFORM] -----------------------------------------------------------------"
