# Kubernetes

Process Automator can be use with Kubernetes. A docker image is available at ..

## Kubernetes

Here an example of a Kubernetes file

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: camunda-8-processautomator-service
  labels:
    app: camunda-8-processautomator-service
spec:
  selector:
    matchLabels:
      app: camunda-8-processautomator-service
  replicas: 462
  template:
    metadata:
      labels:
        app: camunda-8-processautomator-service
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/port: "8088"
        prometheus.io/path: "/actuator/prometheus"
    spec:
      containers:
        - name: camunda-8-processautomator-service
          image: pierreyvesmonnet/processautomator:1.0.3
          imagePullPolicy: Always
          env:
            - name: JAVA_TOOL_OPTIONS
              value: >-
                -Dautomator.servers.camunda8.zeebeGatewayAddress=camunda-zeebe-gateway:26500
                -Dautomator.servers.camunda8.operateUserName=demo
                -Dautomator.servers.camunda8.operateUserPassword=demo
                -Dautomator.servers.camunda8.operateUrl=http://camunda-operate:80
                -Dautomator.servers.camunda8.taskListUrl=
                -Dautomator.servers.camunda8.workerExecutionThreads=2000
                -Dautomator.startup.scenarioPath=/scenarii
                -Dautomator.startup.scenarioAtStartup=file:/MyScenario.json
                -Dautomator.startup.waitWarmup=PT10S
                -Dautomator.startup.policyExecution=SERVICETASK
                 -Dautomator.startup.topic=takedown-create
                -Dautomator.startup.logLevel=MAIN
          resources:
            limits:
              cpu: "1"
              memory: 2Gi
            requests:
              cpu: "1"
              memory: 1Gi
          volumeMounts:
            - name: scenarii
              mountPath: MyScenario.json
              subPath: MyScenario.json
              readOnly: true
      volumes:
        - name: scenarii
          configMap:
            name: scenarii


```

# Upload your scenario in a config map
* to load the scenario in a config map
* to create a volume from the config map
* to mount this volume in the container
* to reference the file as a resource.

# Create multiple pods to simulate multiple service task container

# kubectl create configmap scenarii --from-file=$(root)/MyScenario.json -n $(namespace)

## Start

cd k8s kubectl create -f pa-DiscoverySeedExtraction.yaml