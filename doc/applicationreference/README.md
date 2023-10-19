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

### URL

List of server. Easy to use in the CLI on the command line.


This is a list of <name>,CAMUNDA_7|CAMUNDA_8|CAMUNDA_8_SAAS,(<property>)*

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
<name>,CAMUNDA_8_SAAS,zeebeCloudRegister,zeebeCloudRegion,zeebeCloudClusterId,zeebeCloudClientId,zeebeCloudOAuthUrl,zeebeCloudAudience,clientSecret,OperateUserName,OperateUserPassword,OperateUrl,,ExecutionThreads,MaxJobActive
```

**Example**

````yaml


automator.serversConnection: \ 
   Camunda7Diamond,CAMUNDA_7,http://localhost:8080/engine-rest; \
   Camunda8Safir,CAMUNDA_8,127.0.0.1:26500,demo,demo,http://localhost:8081

````

### List of servers
In the YAML, the list of servers can be done by giving a list:

| Parameter              | Explanation                                                  | Type    | Default |
|------------------------|--------------------------------------------------------------|---------|---------|
| name                   | Name of server (to be use to define the server to connect)   | String  | ""      |
| type                   | "CAMUNDA_7", "CAMUNDA_8", "CAMUNDA_8_SAAS"                   | String  | ""      |
| workerExecutionThreads | (CAMUNDA_8, CAMUNDA_8_SAAS) Number of threads to use         | Integer | 20      |           
| workerMaxJobsActive    | (all) Number of jobs to fetch                                | Integer | 10      |               
| url                    | (CAMUNDA_7) Url to connect to Camunda 7                      | String  | ""      |
| zeebeGatewayAddress    | (CAMUNDA_8) Address to connect Camunda 8                     | String  | ""      |
| operateUrl             | (CAMUNDA_8, CAMUNDA_8_SAAS) Url to connect Operate           | String  | ""      |  
| taskListUrl            | (CAMUNDA_8, CAMUNDA_8_SAAS) Url to connect Tasklist          | String  | ""      | 
| operateUserName        | (CAMUNDA_8, CAMUNDA_8_SAAS) User name to connect to Operate  | String  | "demo"  |
| operateUserPassword    | (CAMUNDA_8, CAMUNDA_8_SAAS) Password to connect to Operate   | String  | "demo"  |                    
| region                 | (CAMUNDA_8_SAAS) Saas region                                 | String  | ""      |
| clusterId              | (CAMUNDA_8_SAAS) Cluster ID                                  | String  | ""      |
| clientId               | (CAMUNDA_8_SAAS) Client ID                                   | String  | ""      |
| oAuthUrl               | (CAMUNDA_8_SAAS) Authorization URL                           | String  | ""      |
| audience               | (CAMUNDA_8_SAAS) Audience                                    | String  | ""      |
| secret                 | (CAMUNDA_8_SAAS) Secret                                      | String  | ""      |

***Example***

```yaml
automator.serversList:
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
      region:
      clusterId:
      clientId:
      secret:
      oAuthUrl:
      audience:
      workerExecutionThreads: 10
      workerMaxJobsActive: 10
      
```

### Explicit values

This method is simplest to use to configure a Docker/Kubernetes connection


This definition is very simple to use in the K8 definition, because one variable can be override

For example, to set up a Camunda 8 server, use in the environment:
````yaml
env:
- name: JAVA_TOOL_OPTIONS
  value: >-
    -Dautomator.startup.serverName=zeebeCloud
    -Dautomator.servers.camunda8.name=zeebeCloud
    -Dautomator.servers.camunda8.zeebeGatewayAddress=camunda-zeebe-gateway:26500
    -Dautomator.servers.camunda8.operateUserName=demo
    -Dautomator.servers.camunda8.operateUserPassword=demo
    -Dautomator.servers.camunda8.operateUrl=http://camunda-operate:80
    -Dautomator.servers.camunda8.taskListUrl=http://camunda-tasklist:80
    -Dautomator.servers.camunda8.workerExecutionThreads=1
````

Variables are:

Prefix **automator.servers.camunda7**

| Parameter                  | Explanation                                                 | Type     | Default |
|----------------------------|-------------------------------------------------------------|----------|---------|
| name                       | Name of server (to be use to define the server to connect)  | String   | ""      |
| url                        | Url to connect to Camunda 7                                 | String   | ""      |
| workerMaxJobsActive        | Number of jobs to fetch                                     | Integer  | 20      |


Prefix **automator.servers.camunda8**

| Parameter              | Explanation                                                | Type    | Default |
|------------------------|------------------------------------------------------------|---------|---------|
| name                   | Name of server (to be use to define the server to connect) | String  | ""      |
| zeebeGatewayAddress    | Address to connect Camunda 8                               | String  | ""      |
| operateUrl             | Url to connect Operate                                     | String  | ""      |  
| taskListUrl            | Url to connect Tasklist                                    | String  | ""      | 
| operateUserName        | User name to connect to Operate                            | String  | "demo"  |
| operateUserPassword    | Password to connect to Operate                             | String  | "demo"  |                    
| workerExecutionThreads | Number of threads to use                                   | Integer | 20      |           
| workerMaxJobsActive    | Number of jobs to fetch                                    | Integer | 10      |               


Prefix **automator.servers.camunda8Saas**

| Parameter              | Explanation                                                | Type    | Default |
|------------------------|------------------------------------------------------------|---------|---------|
| name                   | Name of server (to be use to define the server to connect) | String  | ""      |
| operateUrl             | Url to connect Operate                                     | String  | ""      |  
| taskListUrl            | Url to connect Tasklist                                    | String  | ""      | 
| operateUserName        | User name to connect to Operate                            | String  | "demo"  |
| operateUserPassword    | Password to connect to Operate                             | String  | "demo"  |                    
| region                 | Saas regions                                               | String  | ""      |
| clusterId              | Cluster ID                                                 | String  | ""      |
| clientId               | Client ID                                                  | String  | ""      |
| oAuthUrl               | Authorization URL                                          | String  | ""      |
| audience               | Audience                                                   | String  | ""      |
| secret                 | Secret                                                     | String  | ""      |
| workerExecutionThreads | Number of threads to use                                   | Integer | 20      |           
| workerMaxJobsActive    | Number of jobs to fetch                                    | Integer | 10      |


***Example***


````yaml
automator.servers:

    camunda7:
      name: "Camunda7Granit"
      url: "http://localhost:8080/engine-rest"
      workerMaxJobsActive: 20
  

    camunda8:
      name: "Camunda8Calcair"
      zeebeGatewayAddress: "127.0.0.1:26500"
      operateUserName: "demo"
      operateUserPassword: "demo"
      operateUrl: "http://localhost:8081"
      taskListUrl: "http://localhost:8082"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10

    camunda8Saas:
      name: "Camunda8Marbble"
      clusterId: "25fdd1e6-e4a1-4362-b49c-5eced08cb893"
      clientId: "eknOoiO5GYDdFf4ZjDSh8yaLG-BVCw9L"
      oAuthUrl: "https://login.cloud.camunda.io/oauth/token"
      audience: ""
      secret: "4BPUva1U4lDtoG2-torvAtx6w5RbHULUFhGZ-bBXOMWwZJG3d3VDlfPHjVO3Kz-N"
      operateUrl: "https://ont-1.operate.camunda.io/25fdd1e6-e4a1-4362-b49c-5eced08cb893"
      taskListUrl: "https://ont-1.tasklist.camunda.io/25fdd1e6-e4a1-4362-b49c-5eced08cb893"

      operateUserName: "demo"
      operateUserPassword: "demo"

      region: "ont-1"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10
````

