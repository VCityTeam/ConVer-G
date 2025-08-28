#!/bin/bash

######################################################
# Copy the dataset from the BSBM generated dataset
######################################################
echo "---------------------------------------------------------------- [BEGIN DOWNLOAD] ----------------------------------------------------------------"
printf "\versions_number%s$(date +%FT%T) - [Copy] Dataset generation started."

rm -rf ../dataset

mkdir -p ../dataset/triples && cd ../dataset/triples || exit

# Check if the argument is provided
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <number of version> <number of product> <step of products>"
    exit 1
fi

# Get the argument for bsbm
versions_number=$1
products_number=$2
products_steps=$3

# Randomize another bsbm dataset
random_product_number=$(( ( RANDOM % 5 ) + 3 ))
random_product_step=$(( ( RANDOM % 3 ) + 1 ))

# Run the command versions_number times
docker run --name "bsbm-$versions_number-$products_number" -v "$PWD:/app/data" vcity/bsbm generate-n -v "$versions_number" -p "$products_number" -s "$products_steps" -f "nt"
docker run --name "bsbm-alt-$versions_number-$products_number" -v "$PWD:/app/data" -e "DATA_DESTINATION=alt" vcity/bsbm generate-n -v "$versions_number" -p "$random_product_number" -s "$random_product_step" -f "nt"

# Cleaning metadata
docker ps --filter name=bsbm-* -aq | xargs docker stop | xargs docker rm

printf "\versions_number%s$(date +%FT%T) - [Copy] Dataset generation completed."
echo "----------------------------------------------------------------- [END DOWNLOAD] -----------------------------------------------------------------"