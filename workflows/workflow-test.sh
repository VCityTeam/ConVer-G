#!/bin/bash

echo "----------------------------------------------------------------- [BEGIN WORKFLOW] -----------------------------------------------------------------"
/bin/bash ./init_stack.sh

/bin/bash ./test/download-3.sh

/bin/bash ./test/import_relational-3.sh

echo "------------------------------------------------------------------ [END WORKFLOW] ------------------------------------------------------------------"
