#!/bin/bash

######################################################
# Copy the dataset from the BSBM generated dataset
######################################################
echo "---------------------------------------------------------------- [BEGIN DOWNLOAD] ----------------------------------------------------------------"
printf "\versions_number%s$(date +%FT%T) - [Copy] Dataset generation started."

rm -rf ../dataset

mkdir -p ../dataset/triples && cd ../dataset/triples || exit

# Check if the argument is provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <number of version> <number of product>"
    exit 1
fi

# Get the argument
versions_number=$1
products_number=$2

# Run the command versions_number times
docker run --name "bsbm-$versions_number-$products_number" -v "$PWD:/app/data" vcity/bsbm generate-n "$versions_number" "$products_number"
for i in $(seq 1 "$versions_number")
do
  mv "dataset-$i.ttl" "version-$i.split.ttl"
done

# Cleaning metadata
docker ps --filter name=bsbm-* -aq | xargs docker stop | xargs docker rm

printf "\versions_number%s$(date +%FT%T) - [Copy] Dataset generation completed."
echo "----------------------------------------------------------------- [END DOWNLOAD] -----------------------------------------------------------------"