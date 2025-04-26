# Process Execution Automator

# Build the docker image

at root, use 
````yaml
docker build -t pierre-yves-monnet/process-execution-automator:2.0.0 .
````

The docker image is built using the Dockerfile present on the root level.



Push the image to the Camunda hub (you must be login first to the docker registry)

````
docker tag pierre-yves-monnet/process-execution-automator:2.0.0 ghcr.io/camunda-community-hub/process-execution-automator:latest

docker push ghcr.io/camunda-community-hub/process-execution-automator:latest
````


Tag as the latest:
````
docker tag pierre-yves-monnet/blueberry:1.0.0 ghcr.io/camunda-community-hub/blueberry:latest
docker push ghcr.io/camunda-community-hub/blueberry:latest
````



Or use the image generated on camunda-hub

# Start it

````yaml
kubectl create -f pea.yaml -n camunda
````

A pod is started, and a service `pea-service` is available, under theport number `8381`

This kubernetes start a load balancer with a public address.
````shell
$ kubectl get svc
pea-public                          LoadBalancer   34.118.231.236   35.229.70.3       8381:30440/TCP                 29s
pea-service                         ClusterIP      34.118.237.219   <none>            8381/TCP                       4m35s
p
````
Use this port-forward command

````shell
kubectl port-forward svc/pea-service 8381:8381 -n camunda
````