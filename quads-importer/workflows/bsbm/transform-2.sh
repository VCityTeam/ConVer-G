#!/bin/bash

######################################################
# Data transformations
######################################################

cd ../dataset/triples || exit

## Transform data as quads
### Use the find command to locate all files ending with "split.ttl"
printf "\n%s$(date +%FT%T) - [Transformations] Version annotation started.\n"

find . -type f -name "*.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.relational.nq" relational BSBM
done

find . -type f -name "version*split.ttl" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" ttl "$file.theoretical.nq" theoretical BSBM
done

find . -type f -name "*.nt" -print0 | while IFS= read -r -d '' file
do
    python3 ../../python/annotate_graph.py "$file" nt "$file.relational.nq" relational BSBM
done

printf "\n%s$(date +%FT%T) - [Transformations] Version annotation completed."

## Moving quads
printf "\n%s$(date +%FT%T) - [Transformations] Moving quads started."
mkdir -p ../quads/theoretical
mkdir -p ../quads/relational

cp *.theoretical.nq ../quads/theoretical
cp *.relational.nq ../quads/relational
cp theoretical_annotations.nq ../quads/theoretical
rm *.theoretical.nq
rm *.relational.nq
printf "\n%s$(date +%FT%T) - [Transformations] Moving quads completed."

## Adds data in test workspace
printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace started."
### Adds BSBM tagged data in test workspace

rm -rf ../../src/test/resources/dataset
mkdir -p ../../src/test/resources/dataset
cp ../quads/relational/* ../../src/test/resources/dataset

printf "\n%s$(date +%FT%T) - [Transformations] Copy for test workspace completed."
