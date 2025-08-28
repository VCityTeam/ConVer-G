#!/bin/bash

######################################################
# Import the data inside the Relational Database (through quads-loader import endpoint)
######################################################

cd ../dataset/quads/data/relational || exit

echo "------------------------------------------------------------ [BEGIN IMPORT RELATIONAL] ------------------------------------------------------------"
printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import started."

## BSBM tagged data
### Import the versions of BSBM
printf "\n%s$(date +%FT%T) - [quads-loader] Versions import started."

number_of_versions=$1

find . -type f -name "*.trig" -print0 | while IFS= read -r -d '' file
do
    # Extract version number from the file name (assuming the format dataset-{version}.{format}.trig)
    version=$(echo "$file" | grep -oP '(?<=-)\d+(?=.\w+\.trig)')

    # Check if the version is less than or equal to the specified number_of_versions
    if [ "$version" -le "$number_of_versions" ]; then
        printf "\n%s$(date +%FT%T) - [quads-loader] Version $file"
        start=$(date +%s%3N)
        curl --location 'http://localhost:8080/import/version' \
          --header 'Content-Type: multipart/form-data' \
          --connect-timeout 60 \
          --form file=@"$file"
        end=$(date +%s%3N)
        printf "\n%s$(date +%FT%T) - [Measure] (Import STS $file):$((end-start))ms;"
    fi
done

printf "\n%s$(date +%FT%T) - [quads-loader] Versions import completed."

### Import the version transitions

#printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import started."
#
#printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import completed."
printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import completed."

echo "------------------------------------------------------------- [END IMPORT RELATIONAL] -------------------------------------------------------------"
