#!/bin/bash

######################################################
# Import the data inside the OSTRICH Store
######################################################

cd ../dataset/triples/data || exit

echo "-------------------------------------------------------------- [BEGIN IMPORT OSTRICH] --------------------------------------------------------------"
printf "\n%s$(date +%FT%T) - [OSTRICH Store] Dataset import started."

number_of_versions=$1
IMAGE_NAME="rdfostrich/ostrich"

docker run --rm -v "$(pwd):/var/evalrun" --entrypoint /opt/ostrich/build/ostrich-insert ${IMAGE_NAME} -v 0 + /var/evalrun/dataset-1.nt

for ((i=1; i<number_of_versions; i++)); do
    additions="dataset-${i}-dataset-$((i+1)).additions.nt"
    deletions="dataset-${i}-dataset-$((i+1)).deletions.nt"

    echo "Files to import for delta between versions $i and $((i+1)):"
    echo "  Additions: /var/evalrun/$additions"
    echo "  Deletions: /var/evalrun/$deletions"

    docker run --rm -v "$(pwd):/var/evalrun" --entrypoint /opt/ostrich/build/ostrich-insert ${IMAGE_NAME} -v "$i" + "/var/evalrun/$additions" - "/var/evalrun/$deletions"
done

printf "\n%s$(date +%FT%T) - [OSTRICH Store] Dataset import completed."
echo "--------------------------------------------------------------- [END IMPORT OSTRICH] ---------------------------------------------------------------"
