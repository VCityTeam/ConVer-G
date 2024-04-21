#!/bin/bash

# Check if the argument is provided
if [ "$#" -ne 2 ]; then
    echo "Usage: $0 <number of version> <number of product>"
    exit 1
fi

start=$(date +%s)

/bin/bash ./init_stack.sh

/bin/bash ./bsbm/download-2.sh "$1" "$2"

/bin/bash ./bsbm/transform-2.sh

/bin/bash ./bsbm/import_relational-2.sh

/bin/bash ./bsbm/import_triple-2.sh

end=$(date +%s)

echo "$((end-start))s"