mkdir dataset

cd dataset || exit

# Download the dataset from the LIRIS dataset server
echo "Dataset download started."

## Get the versions
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2018_split.ttl' \
  --output 'GratteCiel_2018_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2015_split.ttl' \
  --output 'GratteCiel_2015_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2012_split.ttl' \
  --output 'GratteCiel_2012_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2012_alt_split.ttl' \
  --output 'GratteCiel_2012_alt_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_split.ttl' \
  --output 'GratteCiel_2009_split.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_alt_split.ttl' \
  --output 'GratteCiel_2009_alt_split.ttl'


## Get the workspace
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel_2009_2018_Workspace.ttl' \
  --output 'GratteCiel_2009_2018_Workspace.ttl'

## Get the version transitions
curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009_2009b.ttl' \
  --output 'Transition_2009_2009b.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009_2012.ttl' \
  --output 'Transition_2009_2012.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2009b_2012b.ttl' \
  --output 'Transition_2009b_2012b.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2012_2015.ttl' \
  --output 'Transition_2012_2015.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2012b_2015.ttl' \
  --output 'Transition_2012b_2015.ttl'

curl --request GET -sL \
  --url 'https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/Transition_2015_2018.ttl' \
  --output 'Transition_2015_2018.ttl'

echo "Dataset download completed."

# WORKAROUND - Replace data prefix (enabling versioning)
echo "Replacement started."

## We set the new data prefix to a versionable compatible one
replacement="@prefix data: <https://dataset-dl.liris.cnrs.fr/rdf-owl-urban-data-ontologies/Datasets/GratteCiel_Workspace_2009_2018/3.0/GratteCiel#> ."

## Use the find command to locate all files ending with "split.ttl"
find . -type f -name "*split.ttl" -print0 | while IFS= read -r -d '' file
do
    sed -i '/^@prefix data:/c\'"$replacement" "$file"
done

echo "Replacement completed."

# Adds data in test workspace
echo "Copy for test workspace started."

cp GratteCiel_* ../src/test/resources/static/dataset
cp Transition_* ../src/test/resources/static/dataset

echo "Copy for test workspace completed."

# Import the data
echo "Dataset import started."

## Import the versions
echo "Versions import started."
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2018_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2015_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2012_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2012_alt_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2009_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2009_alt_split.ttl"'

echo "Versions import completed."

## Import the workspace
echo "Workspace cleaning started."

curl --location --request DELETE 'http://localhost:8080/import/workspace'

echo "Workspace cleaning completed."

echo "Workspace import started."

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"GratteCiel_2009_2018_Workspace.ttl"'

echo "Workspace import completed."

## Import the version transitions
echo "Version transitions import started."

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2009_2009b.ttl"'

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2009_2012.ttl"'

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2009b_2012b.ttl"'

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2012_2015.ttl"'

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2012b_2015.ttl"'

curl --location 'http://localhost:8080/import/workspace' \
  --header 'Content-Type: multipart/form-data' \
  --form 'file=@"Transition_2015_2018.ttl"'

echo "Version transitions import completed."

echo "Dataset import completed."
