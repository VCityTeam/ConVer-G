#!/bin/bash

######################################################
# Import the data inside the Relational Database (through quads-loader import endpoint)
######################################################

cd ../dataset || exit

printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import started."

## Grand-Lyon tagged data
### Import the versions of Grand-Lyon metadata
printf "\n%s$(date +%FT%T) - [quads-loader] Versions import started."
printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2018_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2018_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2015_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2015_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2012_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2012_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2012_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2012_alt_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2009_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2009_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version GratteCiel_2009_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/relational/GratteCiel_2009_alt_split.ttl.relational.trig"'

printf "\n%s$(date +%FT%T) - [quads-loader] Versions import completed."

### Import the workspace
printf "\n%s$(date +%FT%T) - [quads-loader] Workspace import started."

printf "\n%s$(date +%FT%T) - [quads-loader] Workspace GratteCiel_2009_2018_Workspace."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2009_2018_Workspace.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Workspace import completed."

### Import the version transitions
printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import started."

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2009_2009b."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009_2009b.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2009_2012."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009_2012.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2009b_2012b."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009b_2012b.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2012_2015."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2012_2015.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2012b_2015."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2012b_2015.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Transition Transition_2015_2018."
curl --location 'http://localhost:8080/import/metadata' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2015_2018.ttl"'

printf "\n%s$(date +%FT%T) - [quads-loader] Version transitions import completed."
printf "\n%s$(date +%FT%T) - [quads-loader] Dataset import completed."
