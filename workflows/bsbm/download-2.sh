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

# Create the save directory if it doesn't exist
rm -rf save
mkdir -p save

echo "------- Generating $versions_number versions -------"

# Run the command versions_number times
for ((i=0;i<versions_number;i++)); do
    echo "Generating version $i"
    echo "Generating $((products_number + i)) products"
    docker run --name "bsbm-$i" -v "$PWD:/data" vcity/bsbm generate -s ttl -pc $((products_number + i)) -tc $((i)) -ppt $((i))
    mv dataset.ttl "save/version-$i.split.ttl"
done

cp save/* .
rm -rf save

# Cleaning metadata
docker ps --filter name=bsbm-* -aq | xargs docker stop | xargs docker rm

printf "\versions_number%s$(date +%FT%T) - [Copy] Dataset generation completed."
echo "----------------------------------------------------------------- [END DOWNLOAD] -----------------------------------------------------------------"