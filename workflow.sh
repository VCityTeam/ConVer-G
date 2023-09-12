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

# Import the data
echo "Dataset import started."

## Import the versions
curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2018_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2015_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2012_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2012_alt_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2009_split.ttl"'

curl --location 'http://localhost:8080/import/version' \
  --header 'Content-Type: multipart/form-data' \
  --form 'files=@"GratteCiel_2009_alt_split.ttl"'

## Import the workspace

# TODO : Import the workspace

echo "Dataset import completed."
