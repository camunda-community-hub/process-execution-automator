# Process Execution Automator

# Build the docker image

at root, use 
````yaml
docker build -t pierreyvesmonnet/vobilediscovery:1.0.0 .

docker push pierreyvesmonnet/vobilediscovery:1.0.0
````

Or use the image generated on camunda-hub

# Deploy

````yaml
kubectl create -f pa-verification.yaml
````
