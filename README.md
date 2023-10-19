
![Compatible with: Camunda Platform 7](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%207-26d07c)
![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)
[![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)


# process-execution-automator

Create scenarios to automate any execution of processes. Objectives are
* Unit test and regression: You need to verify that a process reacts the same if you create a process instance with the variable "amount=100", and that the process comes to the user task "review".
* Unit performance test: The process calls a service task "getCreditScore," and you want to verify this execution stays under 200 ms
* Developer reason: you developed the task "getCreditScore", and this task is in the process after 4 user tasks and 3 service tasks that you need to simulate

The Unit Test section covers these goals.

* Load test: to verify that the platform can handle 1000 process instances created every 10 minutes
  and can process this throughput: it should terminate this 1000 process every 10 minutes.
* Generate process instances: For any reason, you want to generate 400 process instances and advance them to the user task "Review" to check your user application

The Load Test section covers these goals.


Process-Automator executes scenario. One scenario pilot a process.

It is possible to execute multiple at the same time to handle a use case like
"generate 100 process instances/minute on process Review, 5 process instances per second on process Expense."

Automator does not start a Camunda Engine; it communicates with it. It can be a Camunda 7 server or a Camunda 8 server.

The goal of the Automator is not to simulate the execution. It is to pilot an execution on a real
system, and to verify that the process reacts as expected.

## Execute a process

From a scenario, the Process-Automator calls the Camunda Engine server (C7 or C8) and executes the different
steps in the scenario. Let's take an example with this scenario:

````
Create a new process instance with the variable "subscriptionLevel: "GOLD", "customerId": 14422
````

The Camunda Engine (C7 or C8) creates and processes the process. The Camunda Engine executes the `GetContext` operation, and, according to the information, the process instance moves to the task `Review Level 1`.
In the scenario, Process-Automator waits for this user task. It will execute it and set `ReviewLevel2Needed.`
to True. The Camunda Engine moves the process instance to `Review Level 2`. In the scenario, Process-Automator waits for this user task. It will execute it. The Camunda engine continues the execution. It
executes `Register Application`, waits for the message, executes `Notify Applicant`, and completes the process instance.

Another scenario can execute only `Review Level 1` or no review.

What Process-Automator do:

* It creates a process instance with some specific value
* It executes user tasks with some specific value
* It can throw a BPMN Message
* Simulate execute service task in Flow Scenario

Process-Automator do not

* Execute service task in unit-scenario
* It is not expected to throw a BPMN Message in the flow:  Process-Automator piloted a real system.

## Requirement

Process-Automator needs to connect to a running platform, Camunda 7 or Camunda 8. Automator is not a process
simulator. The running platform will execute all service tasks.

A scenario can be executed on a Camunda 7 or a Camunda 8 server. Process-Automator provides:

* a server running under Springboot
* a docker image
* An API to be integrated into any other tools

## Different usages


### Unit test and regression, unit performance test (unit-scenario)

The unit scenario describes one process instance execution. Creation and user tasks are described.
This functionality is used to run regression tests, coverage tests, or just advance process instances in the process
for the development.

Visit [Unit Scenario](doc/unittestscenario/README.md)

### Load test (flow-scenario)

The flow scenario describes an environment and sends requirements like "generate 500 PI every 40 seconds".
The flow scenario has a duration and objective to verify.


You can specify objectives: produce 1000 Process Instances, end 500 process instances, and produce 300 tasks in a user task.

Visit [Load Test Scenario](doc/loadtestscenario/README.md) and the [Load test Tutorial](doc/loadtestscenario/Tutorial.md)

## Scenario

This section references all the information to build a scenario.
Visit [Scenario reference](doc/scenarioreference/README.md)


## Connection to a server

Process-Automator does not contain any Camunda server. It connects to an existing Camunda Engine. Two
communication interfaces exist, one for Camunda 7 and one for Camunda 8. A scenario can then pilot a
Camunda 7 or a Camunda 8 server.

The scenario does not contain any server information. It has only the server.

Process-Automator references a list of servers in the configuration in multiple ways:
* serverConnection : String, containing a list of connections separated by ;
* serverList: List of records.
* camunda7 : information to connnect a Camunda 7 server
* camunda8 : information to connect a Camunda 8 server
* camunda8Saas: information to connect a Camunda 8 Saas server


At the execution, two parameters are mandatory:
* the scenario to run
* the server to connect to run the scenario

In this way, using the same scenario in different environments is possible.

Example to run the CLI command

````
`java -jar target/process-execution-automator.jar \
  -s Camunda8Ruby \ 
  -v  \
  -l MAIN \ 
  -x run doc/unittestscenario/resources/C8LoanManagementScn.json
````
## Use in Docker
A docker image is created. The image can be used in a docker-compose.

Visit [Docker documentation](doc/docker/README.md)

Scenario and Server configuration can be set up at startup.

## Use in Kubernetes

The project can be used in a docker environment to create, for example,
* one container to run a scenario within a context, creating 500 process instances every 40 seconds
* one container to simulate the service task `getCreditScore`. This worker runs 200 threads
* twenty containers to simulate the service task `checkRisk`. This worker runs 100 threads in ThreadExecutionWorker implementation.

Visit [Kubernetes documentation](doc/kubernetes/README.md)


## Different worker implementation
The project can simulate the service task. Different implementations are available: the classical and the Thread implementation.


Visit [Different Worker Implementation](https://github.com/pierre-yves-monnet/C8-workers-implementation) project
to have an explanation of the different implementations.



# Application properties

Different properties:

## automator.startup

This section gives different parameters to execute at startup.

| parameter                  | explanation                                                                                      | default |
|----------------------------|--------------------------------------------------------------------------------------------------|---------|
| serverName                 | server to use at startup. The server name must exist in the list of servers |         |
| scenarioPath               | Path to load the scenario to run at startup. The path must be accessible by the application          |         |
| scenarioResourceAtStartup  | Resource to load the scenario. Useful in the Kubernetes word to give the scenario in a config map |         |
| logLevel                   | "DEBUG", "INFO", "MONITORING", "MAIN", "NOTHING"                                                 |         |
| policyExecution            | "DEPLOYPROCESS", "WARMINGUP", "CREATION", "SERVICETASK"", "USERTASK" see bellow                    |         | 
| filterService              | in combinaison with SERVICETASK. Topic name                                                      |         |
| deepTracking               | If true, more information are coming on the log (each execution for example)                    | false   |

**policyExecution**
A combinaison of "DEPLOYPROCESS", "WARMINGUP","CREATION","SERVICETASK"", "USERTASK", for example "WARMINUP|CREATION|USERTASK"

SERVICETASK is running in combination with the filterService value.
The application runs only these role. Doing that, in a cluster, it's possible to start 10 pods running the creation and 5 pods running specific service tasks.

## main information


| parameter                  | explanation                     | default |
|----------------------------|---------------------------------|--|
| logDebug  | Specify the information to log  | false  |

## server connection

### String connection

The string contains a list of connections, separate by a semi-colon (":").
Each parameter in the connection is separated by a comma (,)

First parameters are

* Server name
* Type of server: CAMUNDA_7, CAMUNDA_8,  <name>,CAMUNDA_7,CAMUNDA_8_SAAS

The following parameters depend on the type.


**CAMUNDA_7**


* URL to connect the server

**CAMUNDA_8**

* ZeebeGatewayAddress,
* OperateUserName,
* OperateUserPassword,
* OperateUrl,
* ExecutionThreads,
* MaxJobActive


**CAMUNDA_8_SAAS**

* zeebeCloudRegion,
* zeebeCloudClusterId,
* zeebeCloudClientId,
* zeebeCloudOAuthUrl,
* zeebeCloudAudience,
* clientSecret,
* OperateUserName,
* OperateUserPassword,
* OperateUrl,
* ExecutionThreads,
* MaxJobActive

**Example**

````yaml
automator.serversConnection: \
  Camunda7Diamond,CAMUNDA_7,http://localhost:8080/engine-rest; \
  Camunda8Safir,CAMUNDA_8,127.0.0.1:26500,demo,demo,http://localhost:8081
````


### List of server connection

Yaml list

`````yaml
automator.serversList:
    - type: "camunda7"
      name: "camunda7Emeraud"
      url: "http://localhost:8080/engine-rest"
      workerMaxJobsActive: 20

    - type: "camunda8"
      name: "Camunda8Ruby"
      zeebeGatewayAddress: "127.0.0.1:26500"
      operateUserName: "demo"
      operateUserPassword: "demo"
      operateUrl: "http://localhost:8081"
      taskListUrl: "http://localhost:8082"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10

    - type: "camunda8saas"
      name: "Camunda8Grena"
      region: "bru-2"
      clusterId: "4b...e2"
      clientId: "bs...6a"
      secret: "-Ez...ZG"
      oAuthUrl: "https://login.cloud.camunda.io/oauth/token"
      audience: "zeebe.camunda.io"
      operateUrl: "https://bru-2.operate.camunda.io/4b..e2"
      taskListUrl: "https://bru-2.tasklist.camunda.io/4b..e2"
      workerExecutionThreads: 10
      workerMaxJobsActive: 10
``````

### Explicit address

This definition is straightforward to use in the Kubernetes definition because one variable can be override


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
    workerExecutionThreads: 10
    workerMaxJobsActive: 10
    operateUrl: "https://ont-1.operate.camunda.io/25fdd1e6-e4a1-4362-b49c-5eced08cb893"
    taskListUrl: "https://ont-1.tasklist.camunda.io/25fdd1e6-e4a1-4362-b49c-5eced08cb893"

    operateUserName: "demo"
    operateUserPassword: "demo"

    region: "ont-1"
    clusterId: "25fdd1e6-e4a1-4362-b49c-5eced08cb893"
    clientId: "eknOoiO5GYDdFf4ZjDSh8yaLG-BVCw9L"
    oAuthUrl: "https://login.cloud.camunda.io/oauth/token"
    audience: ""
    secret: "4BPUva1U4lDtoG2-torvAtx6w5RbHULUFhGZ-bBXOMWwZJG3d3VDlfPHjVO3Kz-N"
````
