#!/bin/bash

######################################################
# Copy the dataset from the Test dataset
######################################################
echo "---------------------------------------------------------------- [BEGIN DOWNLOAD] ----------------------------------------------------------------"
rm -rf ../dataset

mkdir -p ../dataset/quads && cd ../dataset/quads || exit

cp ../../quads-loader/src/test/resources/dataset/dataset-1.ttl.trig .
cp ../../quads-loader/src/test/resources/dataset/dataset-2.ttl.trig .

echo "----------------------------------------------------------------- [END DOWNLOAD] -----------------------------------------------------------------"