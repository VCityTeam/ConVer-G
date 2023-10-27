#!/bin/bash

######################################################
# Import the data inside the Relational Database (through SPARQL-to-SQL import endpoint)
######################################################

cd ../dataset || exit

echo "[SPARQL-to-SQL] Dataset import started."

## Villeurbanne tagged data
### Import the versions of Villeurbanne workspace
echo "[SPARQL-to-SQL] Versions import started."
echo "[SPARQL-to-SQL] Version GratteCiel_2018_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2018_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version GratteCiel_2015_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2015_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version GratteCiel_2012_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2012_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version GratteCiel_2012_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2012_alt_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version GratteCiel_2009_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2009_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version GratteCiel_2009_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2009_alt_split.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Versions import completed."

### Import the workspace
echo "[SPARQL-to-SQL] Workspace cleaning started."

curl --location --request DELETE 'http://localhost:8080/import/workspace'

echo "[SPARQL-to-SQL] Workspace cleaning completed."

echo "[SPARQL-to-SQL] Workspace import started."

echo "[SPARQL-to-SQL] Workspace GratteCiel_2009_2018_Workspace."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/GratteCiel_2009_2018_Workspace.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Workspace import completed."

### Import the version transitions
echo "[SPARQL-to-SQL] Version transitions import started."

echo "[SPARQL-to-SQL] Transition Transition_2009_2009b."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2009_2009b.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Transition Transition_2009_2012."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2009_2012.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Transition Transition_2009b_2012b."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2009b_2012b.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Transition Transition_2012_2015."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2012_2015.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Transition Transition_2012b_2015."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2012b_2015.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Transition Transition_2015_2018."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"quads/named_graph/Transition_2015_2018.ttl.quads_named_graph.nq"'

echo "[SPARQL-to-SQL] Version transitions import completed."

## non-tagged data (default graph)
### Import the versions of workspace
echo "[SPARQL-to-SQL] Versions import started."
echo "[SPARQL-to-SQL] Version GratteCiel_2018_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2018_split.ttl"'

echo "[SPARQL-to-SQL] Version GratteCiel_2015_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2015_split.ttl"'

echo "[SPARQL-to-SQL] Version GratteCiel_2012_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2012_split.ttl"'

echo "[SPARQL-to-SQL] Version GratteCiel_2012_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2012_alt_split.ttl"'

echo "[SPARQL-to-SQL] Version GratteCiel_2009_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2009_split.ttl"'

echo "[SPARQL-to-SQL] Version GratteCiel_2009_alt_split."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2009_alt_split.ttl"'

echo "[SPARQL-to-SQL] Versions import completed."

### Import the workspace
echo "[SPARQL-to-SQL] Workspace cleaning started."

curl --location --request DELETE 'http://localhost:8080/import/workspace'

echo "[SPARQL-to-SQL] Workspace cleaning completed."

echo "[SPARQL-to-SQL] Workspace import started."

echo "[SPARQL-to-SQL] Workspace GratteCiel_2009_2018_Workspace."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/GratteCiel_2009_2018_Workspace.ttl"'

echo "[SPARQL-to-SQL] Workspace import completed."

### Import the version transitions
echo "[SPARQL-to-SQL] Version transitions import started."

echo "[SPARQL-to-SQL] Transition Transition_2009_2009b."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009_2009b.ttl"'

echo "[SPARQL-to-SQL] Transition Transition_2009_2012."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009_2012.ttl"'

echo "[SPARQL-to-SQL] Transition Transition_2009b_2012b."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2009b_2012b.ttl"'

echo "[SPARQL-to-SQL] Transition Transition_2012_2015."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2012_2015.ttl"'

echo "[SPARQL-to-SQL] Transition Transition_2012b_2015."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2012b_2015.ttl"'

echo "[SPARQL-to-SQL] Transition Transition_2015_2018."
curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"triples/Transition_2015_2018.ttl"'

echo "[SPARQL-to-SQL] Version transitions import completed."

echo "[SPARQL-to-SQL] Dataset import completed."


