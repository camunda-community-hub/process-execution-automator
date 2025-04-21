# Process Execution Automator

# Build the docker image

at root, use 
````yaml
docker build -t myGithubId/processautomator:1.0.0 .

docker push myGithubId/processautomator:1.0.0
````

Or use the image generated on camunda-hub

# Start it

````yaml
kubectl create -f process-execution-automator.yaml -n camunda
````

A pod is started, and a service `pea-service` is available, under theport number `8381`

use this port-forward command

````shell
 
kubectl port-forward svc/pea-service 8381:8381 -n camunda

````