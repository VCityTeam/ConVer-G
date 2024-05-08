#!/bin/bash

cd ..

echo "---------------------------------------------------------------- [BEGIN INIT STACK] ----------------------------------------------------------------"

docker compose down

for docker_volume_id in $(docker volume ls -q | grep sparql-to-sql)
do
   docker volume rm "$docker_volume_id"
done

if [ "$#" -eq 1 ]; then
    printf "[Init stack] Saving logs in workflows/%s\n" "$1"
    # docker compose up, redirecting logs to a file in background
    docker compose up >> "workflows/$1" 2>&1 &
else
  printf "[Init stack] launching docker compose on detached mode"
  docker compose up -d
fi

echo "----------------------------------------------------------------- [END INIT STACK] -----------------------------------------------------------------"