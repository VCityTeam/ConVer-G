#!/bin/bash

######################################################
# Copy the dataset from the BSBM generated dataset
######################################################

printf "\n%s$(date +%FT%T) - [Copy] Dataset copy started."

rm -rf ../dataset

mkdir -p ../dataset/triples

cp ../../../bsbmtools-0.2/save/* ../dataset/triples

printf "\n%s$(date +%FT%T) - [Copy] Dataset copy completed."