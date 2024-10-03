#!/bin/bash

cd ..

echo "---------------------------------------------------------------- [BEGIN INIT STACK] ----------------------------------------------------------------"

docker compose down

for docker_volume_id in $(docker volume ls -q | grep conver-g)
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

cd quads-creator || exit
docker build -t vcity/annotate_graph .

echo "----------------------------------------------------------------- [END INIT STACK] -----------------------------------------------------------------"