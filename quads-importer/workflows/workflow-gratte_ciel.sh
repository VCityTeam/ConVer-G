#!/bin/bash

start=$(date +%s)

/bin/bash ./init_stack.sh

#/bin/bash ./gratte_ciel/download.sh
/bin/bash ./gratte_ciel/download-2.sh

#/bin/bash ./gratte_ciel/transform.sh
/bin/bash ./gratte_ciel/transform-2.sh

#/bin/bash ./gratte_ciel/import_relational.sh
/bin/bash ./gratte_ciel/import_relational-2.sh

#/bin/bash ./gratte_ciel/import_triple.sh
/bin/bash ./gratte_ciel/import_triple-2.sh

end=$(date +%s)

echo "$((end-start))s"