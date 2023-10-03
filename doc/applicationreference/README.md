# Application reference

This document gives the different parameters to use the application.

These parameters can be used in the properties file (YAML, PROPERTIES) or as a parameter
in the docker/Kubernetes file.


For example, for a variable  `automator.startup.scenarioPath`, you can define this like

```yaml
automator:
  startup:
    scenarioPath: /app/automator
```

or like this in a docker image

```yaml
env:
- name: JAVA_TOOL_OPTIONS
  value: >-
     -Dautomator.startup.scenarioPath=/app/automator

```

## general

This section is dedicated to getting the primary information.

| Parameter           | Explanation                      | Type     | Default |
|---------------------|----------------------------------|----------|---------|
| automator.logdebug  | level of log in the application  | Boolean  | false   |



## Startup

These variables pilot the application at startup.

The policy Execution gives action to execute at the startup:
* DEPLOYPROCESS: The scenario presented in the scenario is deployed. They must be accessible in the scenarioPath.
  This scenario path must be accessible by the image using a PVC or a ConfigMap (See the Kubernetes part)
* WARMINGUP: If the scenario declares a warming, this phase is executed
* CREATION: if the scenario declares a process instance creation, this phase is executed
* SERVICETASK: if the scenario declares service simulation task, they are executed. If a `filterService` is declared, only services matching the filter are executed
* USERTASK: If the scenario declares user task, they are executed.

| Parameter                                   | Explanation                                                                               | Type   | Default |
|---------------------------------------------|-------------------------------------------------------------------------------------------|--------|---------|
| automator.startup.policyExecution           | Actions running by the application (DEPLOYPROCESS,WARMINGUP,CREATION,SERVICETASK,USERTASK | String | ""      |
| automator.startup.scenarioPath              | Path where scenario are                                                                   | String | "/"     |
| automator.startup.scenarioFileAtStartup     | list of Resource separate by ;                                                            | String | ""      |
| automator.startup.scenarioResourceStartup   | One resource to load to get the scenario                                                  | String | ""      |
| automator.startup.logLevel                  | Level of startup component (DEBUG, INFO, MONITORING, DASHBOARD, MAIN, NOTHING)            | String | MAIN    |
| automator.startup.filterService             | Filter to run only specific service                                                       | String | ""      |


### Load scenario from file
A scenarioPath and a multiple scenarioFile are given and are loaded from this part.

### Load scenario from resource
Passing the scenario file to a running container is more challenging.
In detail, check the  (Kubernetes manual)[doc/kubernetes/README.md].

The idea is:
* to load the scenario in a config map
* to create a volume from the config map
* to mount this volume in the container
* to reference the file as a resource.



## Servers

This section declared a list of servers. Each server has a name. In a scenario, servers are using: it must be executed on the server "Diamond".
This section declared how to connect the server "Diamond": is that a Camunda7 server or Camunda 8? What are the properties to connect to the server?

There ed is two ways to declare a list of server:
a ServerString Connection.

This is a list of <name>,CAMUNDA_7|CAMUNDA_8,(<property>)*

For Camunda 7:
```
<name>,CAMUNDA_7,<url>
```

For Camunda 8 to Self-manage
```
<name>,CAMUNDA_8,ZeebeGatewayAddress,OperateUserName,OperateUserPassword,OperateUrl,ExecutionThreads,MaxJobActive;
```

For Camunda 8 Saas:
```
<name>,CAMUNDA_8_SAAS,zeebeCloudRegister,zeebeCloudRegion,zeebeCloudClusterId,zeebeCloudClientId,clientSecret,OperateUserName,OperateUserPassword,OperateUrl,ExecutionThreads,MaxJobActive
```




# Another way to provide the list of server connection
In the YAML, the list of servers can be done by giving

```
automator.servers:
    - name: "Gold"
      type: "CAMUNDA_7"
      url: "http://localhost:8080/engine-rest"
      workerMaxJobsActive: 20

    - name: "Platinium"
      type: "CAMUNDA_8"
      zeebeGatewayAddress: "127.0.0.1:26500"
      operateUserName: "demo"
      operateUserPassword: "demo"
      operateUrl: "http://localhost:8081"
      taskListUrl: "http://localhost:8082"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10

    - name: "Diamond"
      type: "CAMUNDA_8_SAAS"
      zeebeCloudRegister:
      zeebeCloudRegion:
      clientSecret:
      zeebeCloudClusterId:
      zeebeCloudClientId:
```

