#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset/triples || exit

## WORKAROUND - Replace data prefix (enabling versioning)
echo "[Transformations] Replacement started."

### We set the new data prefix to a versionable compatible one
### FIXME : Autoincrement (fix entity linking)
replacement="@prefix data: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#> ."

### Use the find command to locate all files ending with "split.ttl"
find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    sed -i '/^@prefix data:/c\'"$replacement" "$file"
done

echo "[Transformations] Replacement completed."

## Transform data as quads
### Use the find command to locate all files ending with "split.ttl"
echo "[Transformations] Version annotation started."

find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.quads_versions.nq" version
done

find . -type f -name "*.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.quads_named_graph.nq" named_graph Villeurbanne
done

echo "[Transformations] Version annotation completed."

## Moving quads
echo "[Transformations] Moving quads started."
mkdir -p ../quads/named_graph
mkdir -p ../quads/versions

mv *.quads_versions.nq ../quads/versions
mv *.quads_named_graph.nq ../quads/named_graph
echo "[Transformations] Moving quads completed."

## Adds data in test workspace
echo "[Transformations] Copy for test workspace started."
### Adds Villeurbanne tagged data in test workspace
mkdir -p ../../src/test/resources/dataset/quads
cp ../quads/named_graph/GratteCiel_* ../../src/test/resources/dataset/quads
cp ../quads/named_graph/Transition_* ../../src/test/resources/dataset/quads

### Adds non tagged data in test workspace
mkdir -p ../../src/test/resources/dataset/triples
cp ../triples/GratteCiel_* ../../src/test/resources/dataset/triples
cp ../triples/Transition_* ../../src/test/resources/dataset/triples
echo "[Transformations] Copy for test workspace completed."
