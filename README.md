
![Compatible with: Camunda Platform 7] ![Compatible with: Camunda Platform 8] [![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)


# process-execution-automator

Create scenarios to automate any execution of processes. Objectives are A unit test, load test,
CD/CI integration The Automator does not start a Camunda Engine. It communicates with an external
Camunda Engine and pilots the execution.

It can connect to a Camunda 7 or a Camunda 8 server.

## Execute a process

From a scenario, Automator calls the Camunda Engine server (C7 or C8) and executes the different
steps in the scenario. Let's take an example with this scenario:

````
create a new process instance with variable "subscriptionLevel: "GOLD", "customerId": 14422
````

The process is created and processed by the Camunda Engine. The `GetContext` operation is executed by
the Camunda Engine, and, according to the information, the process instance moves to the task `Review Level 1`
in the scenario, Automator waits for this user task. It will execute it and set `ReviewLevel2Needed`
to True. The Camunda Engine move the process instance to `Review Level 2`. In the scenario, Automator
waits for this user task. It will execute it. The Camunda engine continues the execution. It
executes `Register Application`, waits for the message, executes `Notify Applicant` and completes
the process instance.

Another scenario can execute only `Review Level1` or no review at all.

What Automator do:

* it creates a process instance with some specific value
* it executes user tasks with some specific value
* it can throw a BPMN Message
* simulate execute service task in Flow Scenario

Automator do not

* execute Service task in unitscenario
* It is not expected to throw BPMN Message in the flow: a real system is piloted by the Automator.

* The goal of the Automator is not to simulate the execution, it is to pilot an execution on a real
  system, and to verify that the process reacts as expected.

## Requirement

Automator needs to connect to a running platform, Camunda 7 or Camunda 8. Automator is not a process
simulator. The running platform will execute all service tasks.

A scenario can be executed on a Camunda 7 or a Camunda 8 server. Automator provides:

* a server running under Springboot
* a docker image
* an API to be integrated into any other tools

## Different usages

### Unit test (unitscenario)

The unit scenario describe one process instance execution. Creation and user task are describe.
This functionality is used to run regression test, coverage test or just advance process instances in the process for the development


Visit unitscenario/README.md

### Load test (flowscenario)

The flow scenario describe an environment, and send a requirement like "generate 500 PI every 40 seconds". 
The flow scenario has a duration, and objective to verify.


You can specify objectives: produce 1000 Process Instance, ended 500 process instances, produce 300 tasks in a user task.

Visit unitscenario/README.md

## Scenario

Visit the Scenario reference.


## Connect to a server

Automator does not contain any Camunda server. It connects to an existing Camunda Engine. Two
communication interfaces exist, one for Camunda 7 and one for Camunda 8. A scenario can then pilot a
Camunda 7 or a Camunda 8 server.

## Use in Docker
A docker image is created. The image can be used in a docker-compose. 

Visit [Docker documentation](doc/docker/README.md)

## Use in Kubernetes

The project can be used in a docker environment, to create for example one container to run a scenario within a context.
For example, it's possible to create multiple container to execute the same scenario, but different part:
* one container deploy the process, then create 500 process instances every 40 seconds
* one container simulate the worker `verification-retrieve` . This workers run 200 threads. 30 replicats must be instanciate

The docker image can be used to build this kind of platform. Visit [Kubernetes documentation](doc/kubernetes/README.md)
