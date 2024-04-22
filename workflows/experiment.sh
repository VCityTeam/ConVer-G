#!/bin/bash

# Set a list of number of versions and loop through them
#for version in 1 2 4 8 16 32 64 128
#do
#    # Set a list of number products and loop through them
#    for products in 1 10 100 1000 10000
#    do
#      # Set a list of CPU limitation and loop through them
#      for cpu in 1 2 4 8 16
#      do
#        # Set a list of RAM limitation and loop through them
#        for ram in 512 1024 2048 4096 8192 16384
#        do
#          /bin/bash workflow-bsbm.sh $version $products $ram $cpu
#        done
#      done
#    done
#done

#for version in 1 2 4 8
#do
#    # Set a list of number products and loop through them
#    for products in 1 10 100
#    do
#      # Set a list of CPU limitation and loop through them
#      for cpu in 1 2 4 8
#      do
#        # Set a list of RAM limitation and loop through them
#        for ram in 512 1024 2048 4096 8192
#        do
#          /bin/bash workflow-bsbm.sh $version $products $ram $cpu
#        done
#      done
#    done
#done

for version in 1 8
do
    # Set a list of number products and loop through them
    for products in 1 100
    do
      # Set a list of CPU limitation and loop through them
      for cpu in 1 4
      do
        # Set a list of RAM limitation and loop through them
        for ram in 256Mb 2048Mb
        do
          experiment_folder="experiments/results-v$version-p$products-r$ram-c$cpu"
          experiment_logs="$experiment_folder/output.logs"

          printf "[Experiment] Running with: version: %s, products: %s, ram: %s, cpu: %s, logs: %s\n" $version $products $ram $cpu $experiment_logs
          mkdir -p $experiment_folder

          # shellcheck disable=SC2094
          /bin/bash workflow-bsbm.sh $version $products $ram $cpu $experiment_logs >> $experiment_logs
        done
      done
    done
done