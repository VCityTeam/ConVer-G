#!/bin/bash

######################################################
# Import the data inside the Triple Store
######################################################

cd ../dataset/quads/data/theoretical || exit

echo "-------------------------------------------------------------- [BEGIN IMPORT TRIPLE] --------------------------------------------------------------"
printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import started."

number_of_versions=$1

## BSBM tagged data
find . -type f -name "*.trig" -print0 | while IFS= read -r -d '' file
do
    # Extract version number from the file name (assuming the format dataset-{version}.{format}.trig)
    version=$(echo "$file" | grep -oP '(?<=-)\d+(?=.\w+\.trig)')

    # Check if the version is less than or equal to the specified number_of_versions
    if [ "$version" -le "$number_of_versions" ]; then
        printf "%s\n$(date +%FT%T) - [Triple Store] $file."
        start=$(date +%s%3N)
        curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
              --header 'Content-Type:application/x-trig' \
              --connect-timeout 60 \
              --data-binary @"$file"
        end=$(date +%s%3N)
        printf "\n%s$(date +%FT%T) - [Measure] (Import BG $file):$((end-start))ms;"
    fi
done

start=$(date +%s%3N)
curl -X POST --location 'http://localhost:9999/blazegraph/sparql' \
      --header 'Content-Type:application/x-trig' \
      --connect-timeout 60 \
      --data-binary @"theoretical_annotations.trig"
end=$(date +%s%3N)
printf "\n%s$(date +%FT%T) - [Measure] (Import BG theoretical_annotations.trig):$((end-start))ms;"

printf "\n%s$(date +%FT%T) - [Triple Store] Dataset import completed."
echo "--------------------------------------------------------------- [END IMPORT TRIPLE] ---------------------------------------------------------------"
