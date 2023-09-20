
![Compatible with: Camunda Platform 7](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%207-26d07c)
![Compatible with: Camunda Platform 8](https://img.shields.io/badge/Compatible%20with-Camunda%20Platform%208-0072Ce)
[![](https://img.shields.io/badge/Lifecycle-Incubating-blue)](https://github.com/Camunda-Community-Hub/community/blob/main/extension-lifecycle.md#incubating-)


# process-execution-automator

Create scenarios to automate any execution of processes. Objectives are
* Unit test and regression: You need to verify that a process reacts the same if you create a process instance with the variable "amount=100", and that the process comes to the user task "review".
* Unit performance test: The process calls a service task "getCreditScore" and you want to verify this execution stays under 200 ms
* Developer reason: you developed the task "getCreditScore", and this task is in the process after 4 user tasks and 3 service tasks that you need to simulate

These goals are covered by the Unit Test section.

* Load test: to verify that the platform can handle 1000 process instances created every 10 minutes
  and can process this throughput: it should terminate this 1000 process every 10 minutes.
* Generate process instances: For any reason, you want to generate 400 process instances and advance them to the user task "Review" to check your user application

These goals are covered by the Load Test section.


Process-Automator executes scenario. One scenario pilot a process.

It is possible to execute multiple at the same time to handle a use case like
"generate 100 process instances/minute on process Review, 5 process instances per second on process Expense."

Automator does not start a Camunda Engine; it communicates with it. It can be a Camunda 7 server or a Camunda 8 server.

The goal of the Automator is not to simulate the execution. It is to pilot an execution on a real
system, and to verify that the process reacts as expected.

## Execute a process

From a scenario, Process-Automator calls the Camunda Engine server (C7 or C8) and executes the different
steps in the scenario. Let's take an example with this scenario:

````
Create a new process instance with the variable "subscriptionLevel: "GOLD", "customerId": 14422
````

The process is created and processed by the Camunda Engine (C7 or C8). The `GetContext` operation is executed by
the Camunda Engine, and, according to the information, the process instance moves to the task `Review Level 1`.
In the scenario, Process-Automator waits for this user task. It will execute it and set `ReviewLevel2Needed.`
to True. The Camunda Engine moves the process instance to `Review Level 2`. In the scenario, Process-Automator waits for this user task. It will execute it. The Camunda engine continues the execution. It
executes `Register Application`, waits for the message, executes `Notify Applicant`, and completes the process instance.

Another scenario can execute only `Review Level 1` or no review at all.

What Process-Automator do:

* It creates a process instance with some specific value
* It executes user tasks with some specific value
* It can throw a BPMN Message
* Simulate execute service task in Flow Scenario

Process-Automator do not

* Execute service task in unit-scenario
* It is not expected to throw a BPMN Message in the flow: a real system is piloted by the Automator.

## Requirement

Automator needs to connect to a running platform, Camunda 7 or Camunda 8. Automator is not a process
simulator. The running platform will execute all service tasks.

A scenario can be executed on a Camunda 7 or a Camunda 8 server. Automator provides:

* a server running under Springboot
* a docker image
* An API to be integrated into any other tools

## Different usages


### Unit test and regression, unit performance test (unit-scenario)

The unit scenario describes one process instance execution. Creation and user tasks are described.
This functionality is used to run regression tests, coverage tests, or just advance process instances in the process
for the development.

Visit (Unit Scenario)[doc/unitscenario/README.md]

### Load test (flowscenario)

The flow scenario describes an environment and sends a requirement like "generate 500 PI every 40 seconds".
The flow scenario has a duration and objective to verify.


You can specify objectives: produce 1000 Process Instances, end 500 process instances, and produce 300 tasks in a user task.

Visit (Load Test Scenario)[doc/loadtestscenario/README.md] and the (Load test Tutorial) [doc/loadtestscenario/Tutorial.md]

## Scenario

This section references all the information to build a scenario.
Visit (Scenario reference)[doc/scenarioreference/README.md]


## Connect to a server

Process-Automator does not contain any Camunda server. It connects to an existing Camunda Engine. Two
communication interfaces exist, one for Camunda 7 and one for Camunda 8. A scenario can then pilot a
Camunda 7 or a Camunda 8 server.

## Use in Docker
A docker image is created. The image can be used in a docker-compose.

Visit [Docker documentation](doc/docker/README.md)

## Use in Kubernetes

The project can be used in a docker environment to create, for example,
* one container to run a scenario within a context, creating 500 process instances every 40 seconds
* one container to simulate the service task `getCreditScore`. This worker runs 200 threads
* twenty containers to simulate the service task `checkRisk`. This worker runs 100 threads in ThreadExecutionWorker implementation.

Visit [Kubernetes documentation](doc/kubernetes/README.md)


## Different worker implementation
The service task can be simulated by the project. Different implementations are available: the classical and the Thread implementation.


Visit [Different Worker Implementation](https://github.com/pierre-yves-monnet/C8-workers-implementation) project
to have an explanation of the different implementations. 



