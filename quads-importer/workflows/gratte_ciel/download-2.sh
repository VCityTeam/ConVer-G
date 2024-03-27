#!/bin/bash

######################################################
# Copy the dataset from the BSBM generated dataset
######################################################

printf "\n%s$(date +%FT%T) - [Copy] Dataset generation started."

rm -rf ../dataset

mkdir -p ../dataset/triples && cd ../dataset/triples || exit

# Check if the argument is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <n>"
    exit 1
fi

# Get the argument
n=$1

# Create the save directory if it doesn't exist
rm -rf save
mkdir -p save

echo "------- Generating $n versions -------"

# Run the command n times
for ((i=0;i<n;i++)); do
    echo "Generating version $i"
    docker run --name "bsbm-$i" -v "$PWD:/data" vcity/bsbm generate -s ttl -pc $((500 + i)) -tc $((10*i)) -ud -ppt 10
    mv dataset.ttl "save/version-$i.split.ttl"
    mv dataset_update.nt "save/transition-$i.nt"
done

cp save/* .
rm -rf save

# Cleaning workspace
docker ps --filter name=bsbm-* -aq | xargs docker stop | xargs docker rm

printf "\n%s$(date +%FT%T) - [Copy] Dataset generation completed."