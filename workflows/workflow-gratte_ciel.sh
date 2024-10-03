#!/bin/bash

start=$(date +%s%3N)

/bin/bash ./init_stack.sh

download=true

# if -local is set to true, don't download the data
if [ "$1" = "-local" ]; then
  download=false
fi

if [ "$download" = true ]; then
  /bin/bash ./gratte_ciel/download.sh

  /bin/bash ./gratte_ciel/transform.sh
fi

/bin/bash ./gratte_ciel/import_relational.sh

/bin/bash ./gratte_ciel/import_triple.sh

end=$(date +%s%3N)

echo "$((end-start))ms"