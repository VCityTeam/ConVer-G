#!/bin/bash

# Check if the argument is provided
# check if there are 2 or 4 arguments

if [ "$#" -ne 2 ] && [ "$#" -ne 5 ]; then
    echo "Usage: $0 <number of version> <number of product> ?<ram limitation> ?<number of cpu limitation> ?<log output file>"
    exit 1
fi

start=$(date +%s)


echo "----------------------------------------------------------------- [BEGIN WORKFLOW] -----------------------------------------------------------------"
/bin/bash ./init_stack.sh "$3" "$4" "$5"

/bin/bash ./bsbm/download-2.sh "$1" "$2"

/bin/bash ./bsbm/transform-2.sh

/bin/bash ./bsbm/import_relational-2.sh

/bin/bash ./bsbm/import_triple-2.sh
echo "------------------------------------------------------------------ [END WORKFLOW] ------------------------------------------------------------------"

end=$(date +%s)

echo "$((end-start))s"