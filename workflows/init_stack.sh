#!/bin/bash

cd ..

echo "---------------------------------------------------------------- [BEGIN INIT STACK] ----------------------------------------------------------------"

docker compose down

for docker_volume_id in $(docker volume ls -q | grep sparql-to-sql)
do
   docker volume rm "$docker_volume_id"
done

if [ "$#" -gt 1 ]; then
  printf "[Init stack] {Limitation} Setting RAM to %smb and CPU number to %s\n" "$1" "$2"
  echo "RAM_LIMITATION=$1" > .env
  echo "CPU_LIMITATION=$2" >> .env
fi

if [ "$#" -eq 3 ]; then
  printf "[Init stack] Saving logs in workflows/%s\n" "$3"
  # docker compose up, redirecting logs to a file in background
  docker compose up >> "workflows/$3" 2>&1 &
else
  docker compose up -d
fi

echo "----------------------------------------------------------------- [END INIT STACK] -----------------------------------------------------------------"