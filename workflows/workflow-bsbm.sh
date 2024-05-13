#!/bin/bash

# Check if the argument is provided
# check if there are 2 or 4 arguments

if [ "$#" -ne 2 ] && [ "$#" -ne 5 ]; then
    echo "Usage: $0 <number of version> <number of product> ?<ram limitation> ?<number of cpu limitation> ?<log output file>"
    exit 1
fi

if [ "$#" -gt 3 ]; then
  printf "[Init stack] {Limitation} Setting RAM to %smb and CPU number to %s\n" "$3" "$4"
  echo "RAM_LIMITATION=$3" > .env
  echo "CPU_LIMITATION=$4" >> .env

  mv .env ../.env
fi

echo "----------------------------------------------------------------- [BEGIN WORKFLOW] -----------------------------------------------------------------"
/bin/bash ./init_stack.sh

/bin/bash ./bsbm/download-2.sh "$1" "$2"

/bin/bash ./bsbm/transform-2.sh

start_import_relational=$(date +%s%3N)
/bin/bash ./bsbm/import_relational-2.sh
end_import_relational=$(date +%s%3N)
printf "[Measure] {Import relational} Import duration: %s ms\n" "$((end_import_relational-start_import_relational))"

start_import_triple=$(date +%s%3N)
/bin/bash ./bsbm/import_triple-2.sh
end_import_triple=$(date +%s%3N)
printf "[Measure] {Import triple} Import duration: %s ms\n" "$((end_import_triple-start_import_triple))"

echo "------------------------------------------------------------------ [END WORKFLOW] ------------------------------------------------------------------"
