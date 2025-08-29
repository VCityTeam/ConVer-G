#!/bin/bash

# Check if the argument is provided
# check if there are 2 or 4 arguments

if [ "$#" -ne 3 ] && [ "$#" -ne 6 ]; then
    echo "Usage: $0 <number of version> <number of product> <step of products> ?<ram limitation> ?<number of cpu limitation> ?<log output file>"
    exit 1
fi

if [ "$#" -gt 5 ]; then
  printf "[Init stack] {Limitation} Setting RAM to %smb and CPU number to %s\n" "$5" "$6"
  echo "RAM_LIMITATION=$5" > .env
  echo "CPU_LIMITATION=$6" >> .env

  mv .env ../.env
fi

echo "----------------------------------------------------------------- [BEGIN WORKFLOW] -----------------------------------------------------------------"
/bin/bash ./init_stack.sh

/bin/bash ./bsbm/download-2.sh "$1" "$2" "$3"

/bin/bash ./bsbm/transform-2.sh

start_import_relational=$(date +%s%3N)
/bin/bash ./bsbm/import_relational-1.sh "$1"
/bin/bash ./bsbm/import_relational-2.sh "$1"
end_import_relational=$(date +%s%3N)
printf "[Measure] {Import relational} Import duration: %s ms\n" "$((end_import_relational-start_import_relational))"

start_import_triple=$(date +%s%3N)
/bin/bash ./bsbm/import_triple-1.sh "$1"
/bin/bash ./bsbm/import_triple-2.sh "$1"
end_import_triple=$(date +%s%3N)
printf "[Measure] {Import triple} Import duration: %s ms\n" "$((end_import_triple-start_import_triple))"

start_import_deltas=$(date +%s%3N)
/bin/bash ./bsbm/import_deltas.sh "$1"
end_import_deltas=$(date +%s%3N)
printf "[Measure] {Import deltas} Import duration: %s ms\n" "$((end_import_deltas-start_import_deltas))"

echo "------------------------------------------------------------------ [END WORKFLOW] ------------------------------------------------------------------"
