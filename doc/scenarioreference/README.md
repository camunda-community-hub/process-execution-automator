# Scenario reference

A scenario is a JSON file. A scenario explains one execution, from the process creation until a
point. It may not be the end of the process: Automator can be used to advance process instances
until a specific task. It contains:

* Information on the process: Which process has to start? Some information on a delay between two
  creations can be set
* Service task can be registered: Automator will check the process instance executes the task (but
  does not execute it)
* The end event can be registered to verify that the process goes to the end. The process instance
  can execute other tasks: Automator does not verify that, except if the "mode verification" is set
  to "Strict."

# Different BPM

The Automator executes a process instance. It does not care about the definition of the process:
Does the process instance call a sub-process? An Event sub-process? It does not matter.

## Call Activity and sub-process

Process-Automator does not care about these artifacts. An execution is a suite of Activities. These
activities are in the process, or a sub-process does not change the execution.

## User Multi-instance

A process can have a multi-instance task. In the scenario, each task may have multiple executions.
It is possible to execute a multi-instance and give different values for each execution.

## External operation

A scenario may consist of executing some tasks and then sending a Cancellation message or starting a
process instance in a different process to get a Cancellation message. It is possible to describe
this operation in a step.

# Example

`````json


{
  "name": "execution Round 14",
  "version": "1.2",
  "processId": "MergingInclusive",
  "typeScenario": "UNIT",
  "executions": [
    {
      "name": "multinstance",
      "policy": "STOPATFIRSTERROR",
      "numberProcessInstances": 100,
      "numberOfThreads": 5,
      "steps": [
        {
          "name": "classical Start Event",
          "type": "STARTEVENT",
          "taskId": "StartEvent_1"
        },
        {
          "name": "Get context",
          "type": "SERVICETASK",
          "taskId": "Get context",
          "executiontargetms": 10000
        },
        {
          "type": "USERTASK",
          "taskId": "Review level 1",
          "waitingTime": "PT5S",
          "numberofexecution": 10,
          "taskvariable": {
            "statusreview": "YES"
          }
        }
      ],
      "verifications": {
        "activities": [
          {
            "type": "USERTASK",
            "taskId": "Review level 1",
            "state": "ACTIVE"
          },
          {
            "type": "ENDEVENT",
            "taskId": "Application Done"
          }
        ],
        "variables": [
          {
            "variableName": "Score",
            "variableValue": 120
          }
        ],
        "performances": [
          {
            "taskIdBegin": "getScore",
            "taskIdEND": "getScore",
            "performanceTarget": "PT0.5S"
          },
          {
            "taskIdBegin": "getScore",
            "taskIdEND": "riskLevel",
            "performanceTarget": "PT4S"
          }
        ]
      }
    }
  ]
}
`````

# Header

The header gives some main information

| Parameter    | Explanation                                                                            | Example                         |
|--------------|----------------------------------------------------------------------------------------|---------------------------------|
| Name         | Name of the scenario                                                                   | "name" : "Test Load accepted"   |
| version      | version of the scenario                                                                | "version": "1.0"                |
| processId    | process ID: the scenario run on this scenario, last version deployed                   | "processId": "C8LoadManagement" |
| typeScenario | "UNIT" or "FLOW". The type determine the execution, and the other part of the scenario | "typeScenario": "UNIT"          |
| serverName   | optional. Each server has a name, and the scenario can provide this information.       | "serverName": "ZeebeGrena"      |

For some usage of scenario (automator function), the scenario can provide the name of the server. This name must exist in the list of scenario.
Only the Automator function use this function.

# UNIT scenario

This is valid for a typeScenario UNIT.

## Execution parameters

The parent attribute is "executions"

| Parameter              | Explanation                                                                                               | Example                         |
|------------------------|-----------------------------------------------------------------------------------------------------------|---------------------------------|
| Name                   | Name of execution                                                                                         | "name": "This is the first run" |
| policy                 | "STOPATFIRSTERROR" or "CONTINUE": In case of an error, what is the next move. Default is STOPATFIRSTERROR | "policy": "STOPATFIRSTERROR"    |


Then, the execution contains a list of steps.


## STARTEVENT step

Start a new process instance.

| Parameter          | Explanation                                                     | Example                                                                             |
|--------------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------|
| name               | name of the step, optional                                      | "name": "Happy path start event"                                                    |
| type               | Specify the type (STARTEVENT)                                   | "type": "STARTEVENT"                                                                |
| taskId             | Activity ID of start event                                      | "activityId": "StartEvent_1"                                                        |
| numberOfExecutions | Number of execution: number of process instance to create       | "numberOfExecutions" : 300                                                          |
| frequency          | Frequence to create the <numberOfExecution>. ISO format (PT10S) | "frequency": "PT30S"                                                                | 
| nbThreads          | Number of threads to execute the creation in parallel (1)       | "nbThreads": 30                                                                     |   
| variables          | List of variables (JSON file) to update                         | "variables": {"amount": 450, "account": "myBankAccount", "colors": ["blue","red"]}  |
| variablesOperation | List of variables, but the value is an operation                |                                                                                     | 

(1) see multithreading section

The startup wake up every <frequency> time. Then, it created the <numberOfExecution> process instances during the period. 
if after the period it can't create all the process instance, then it stop, and a warning is sent.
For example, with a frequency of 30 S and a number of execution of 300, two scenario:
* it wake up, and create the 300 PI in 5 second. So, it waits 25 seconds, wake up again and create 300 new PI
* it wake up. In 30 seconds, it creates only 240 PI. So, it stop the creation, send an error. Because the period is finish and a new one start, it start create again a new batch of 300.



Example

````yaml


{
  "name": "execution Round 14",
  "executions": [
    {
      "steps": [
        {
          "name": "classical Start Event",
          "type": "STARTEVENT",
          "taskId": "StartEvent_1"
        }
    }
}
````

## USERTASK step

The step waits for a user task and executes it.

| Parameter          | Explanation                                                                                                                                          | Example                                                                            |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| name               | name of the step, optional                                                                                                                           | "name": "review"                                                                   |
| type               | Specify the type (USERTASK)                                                                                                                          | "type": "USERTASK"                                                                 |
| delay              | Deplay to wait before looking for the task, in ISO 8601                                                                                              | "delay": "PT0.1S" waits 100 ms                                                     |
| waitingTime        | Wait for maximum this time before returning an error. Process-Automator queries the engine every 500 ms until this delay. Default value is 5 minutes | "waitingTime" : "PT10S"                                                            |
| taskId             | Activity ID to query                                                                                                                                 | "activityId" : "review"                                                            |
| variables          | List of variables (JSON file) to update                                                                                                              | "variables": {"amount": 450, "account": "myBankAccount", "colors": ["blue","red"]} |
| variablesOperation | List of variables, but the value is an operation                                                                                                     |                                                                                    | 
| numberOfExecutions | Number of execution, the task may be multi-instance. Default is 1                                                                                    | "numberOfExecutions" : 3                                                           |

## SERVICETASK step

The step waits for a service task and executes it.

It depends on the usage of the scenario: if a CD/CI, the service task should be executed by the real
workers, not by the Process-Automator. But in some environments, or to advance quickly the task to a
certain position, you may want to simulate the worker. Then, the Process-Aautomator can execute a
service task. The real worker shouldbe deactivated then. If the service task is not found, then the
scenario will have an error.

| Parameter          | Explanation                                                                                                                                          | Example                                                                             |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| name               | name of the step, optional                                                                                                                           | "name": "get Score"                                                                 |
| type               | Specify the type (SERVICETASK)                                                                                                                       | "type": "SERVICETASK"                                                               |
| delay              | Deplay to wait before looking for the task, in ISO 8601                                                                                              | "delay" : "PT0.1S" waits 100 ms                                                     |
| waitingTime        | Wait for maximum this time before returning an error. Process-Automator queries the engine every 500 ms until this delay. Default value is 5 minutes | "waitingTime" : "PT10S"                                                             |
| taskId             | Activity ID to query                                                                                                                                 | "activityId": "review"                                                              |
| topic              | Topic to search the task (mandatory in C8)                                                                                                           | "topic" : "get-score"                                                               |
| streamEnqbled      | Specify if the worker use the streamEnabled function . Default is true.                                                                              | "streamEnabled: true                                                                |  
| variables          | List of variables (JSON file) to update                                                                                                              | "variables": {"amount": 450, "account": "myBankAccount", "colors": ["blue","red"]}  |
| variablesOperation | List of variables, but the value is an operation                                                                                                     |                                                                                     | 
| modeExecution      | Implementation: options are CLASSICAL, THREAD, THREADTOKEN. Default is CLASSICAL                                                                     | "modeExecution" : "CLASSICAL"                                                       |
| nbTokens           | Number of token (modeExecution==THREADTOKEN).Default is 1                                                                                            | "nbTokens" : 200                                                                    |

See the Multithreading section

There is different implementation for the worker. Choose the one you will use for the simulation.

**"modeExecution" : "CLASSICAL"**

A classical worker executes the job in the handle method. Zeebe Client waits for all threads are
complete before asking for a new batch of jobs.

**"modeExecution" : "THREAD"**

A thread worker creates a new thread to execute the job and then finishes the method. So,
ZeebeClient gets back quickly to all handle() methods and can ask for a new batch. If you run 20
jobs active, 20 jobs are locked, and threads are forked, but immediately, a new batch of 20 jobs is
requested. So, if the task contains 300 items, this method will catch these 300 items very fast, and
300 threads are running.

If the implementation of the workers consists of sending a message to an external service and
waiting for the result, this implementation is perfectly fine: the worker will not have 300 threads;
just send 300 requests and wait for the return.

**"modeExecution" : "THREADTOKEN"**

To control the number of threads working on the worker and to get maximum efficiency, this
implementation can be used. This is the same implementation as before, but a token acquisition is
added. The number of token is setup in <numberOfExecutions> value

## ENDEVENT step

The EndEvent does not have any execution purpose, but a verification goal during a unit test.
Process automator check that the process instance finished by this end event. The EndEvent ID is
store in the `taskId` parameter.

## Variablesoperation

The content of the variable is not a direct value, but an operation.

**generaterandomlist(<Number>)**
Generate a list with a number of value
Example:
````
"loopcrawl": "generaterandomlist(500)"
````
the variable `loopcrawl` will be a list of 500 random string.


**generateuniqueid(<Prefix>)**
Generate a unique sequential number.
The prefix is used to allow multiple counter
Example:
````
"tidblue": "generateuniqueid(blue)"
"tidred": "generateuniqueid(red)"
````
Variables `tidblue` and `tidred` got a unique id, each following a different counter.

**now(LOCALDATETIME|DATE|ZONEDATETIME|LOCALDATE)**
Generate a String object, containing the current date and time.


**stringToDate(LOCALDATETIME|DATE|ZONEDATETIME|LOCALDATE, dateSt)**
Transform a String to a Date (LocalDateTime, Date, ZoneDateTime or LocalDate)


## Verification

Each execution can declare verifications. Verification is executed after the execution.

It's possible to check:

* Active activity: Is the process instance correctly waiting on the task "Final Review" after the
  execution?
* any completed activity: does the process instance execute the task "GetScore"? Does the process
  instance end on the end event "Application Done"?
* any variable value: does the process variable "ApplicantScore" 150?
* any performance: Does the execution of the activity "getScore" stay under 500 milliseconds? Does
  the execution from the activity "getScore" to "GetRiskLevel" stay under 4 seconds?

## Warmup

This section is valid for a scenario FLOW. The parent attribute is "warmup"

The Warmup is used to load the process before starting the measurement. For example, a process may
have a service task that takes 2 mn, then another which needs 5 mn. To load the process before
starting the measurement, it is preferable to start to create process instances and execute service
tas for 5 mn.

| Parameter       | Explanation                                                                   | Example                 |
|-----------------|-------------------------------------------------------------------------------|-------------------------|
| duration        | Maximum time to run the warmup. After this delay, warmup ends and test starts  | "duration": "PT2M"      |
| useServiceTasks | When true, all service's tasks in the scenario are started (default is false) | "useServiceTasks": true | 
| useUserTasks    | When true, all user's tasks in the scenario are started (default is false)    | "useUserTasks": true    | 
| operations      | List of operations                                                            |                         |

An operation is a record with multiple parameters.

| Parameter          | Explanation                                         | Example |
|--------------------|-----------------------------------------------------|---------|
| type               | Type of operation (STARTEVENT, USERTASK,SERVICETASK |         |
| taskId             |                                                     |         |
| processId          |                                                     |         |
| variables          |                                                     |         |   
| variablesOperation | List of variables, but the value is an operation                                                                                                     |                                                                                    | 
| frequency          |                                                     |         |
| numberOfExecutions |                                                     |         |
| endWarmingUp       |                                                     |         | 

Example:

```json
{
  "warmingUp": {
    "duration": "PT2M",
    "operations": [
      {
        "type": "STARTEVENT",
        "taskId": "StartEvent",
        "processId": "CrawlUrl",
        "variablesOperation": {
          "loopcrawl": "generaterandomlist(1000)",
          "urlNotFound": false
        },
        "frequency": "PT6S",
        "numberOfExecutions": "1",
        "endWarmingUp": "EndEventThreshold(EndEvent,1)"
      }
    ]
  }
}
```

### endWarmingUp

Decide when the warmup can finish. If a duration is set and the properties are true, then the warmup
is ended.

The parameter is an expression. Possible functions are:

**UserTaskThreshold**
A task appears in a user task. The expression gives the Number of tasks expected to be true.

Syntax: UserTaskThreshold( < ActivityId >,< Value >)

Example:

````
UserTaskThreshold(Activity_DiscoverySeedExtraction_TheEnd,7)
````

**EndEventThreshold**
If an end event is detected after the instant the scenario starts, then the warmup is ended.

Syntax: EndEventThreshold(< ActivityId >,< Value >)

Example:

````
UserTaskThreshold(Activity_DiscoverySeedExtraction_TheEnd,7)
````


# multi threading

To send more request on server, it's possible to scale the different topic

## Start event
During the start event operation, process instance are created. They are created per period.
Parameters are:

| Parameter          | Explanation                                                     | Example                                                                             |
|--------------------|-----------------------------------------------------------------|-------------------------------------------------------------------------------------|
| numberOfExecutions | Number of execution: number of process instance to create       | "numberOfExecutions" : 300                                                          |
| frequency          | Frequency to create the <numberOfExecution>. ISO format (PT10S) | "frequency": "PT30S"                                                                |
| nbThreads          | Number of threads to execute the creation in parallel (1)       | "nbThreads": 30                                                                     |   

The frequency is the period to create the process instance. Let's use a frequency of 30 Seconds. Each 30 seconds, it will create numberOfExecutions process instances.
If it can't create this number, it will send a error.
To access this number, it's possible to multithreads the creation. This is the "nbWorkers" parameters. A threadPool is created with this value, and the numberOfExecutions creation is sent to this thread pool.

The numberOfWorkers is part of the scenario.
It's possible to override this value via the application.yaml
```yaml
automator:
  startevent:
    nbThreads: 45
```
or by an environment variable in a kubernetes deployment
```yaml

env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -Dautomator.startevent.nbThreads=200
```

## Workers
A workers can be multi thread by two different mechanism: the asynchrounous execution and the multi threading execution

**asynchronous execution**
it consist to not execute the treatment in the handle() or execute() method. This is done via a ThreadToken or an Asynchrounous mode.

**Worker thread**
This section is only for Camunda 8. Camunda 7 have only one thread per worker.

The number of threads for a worker is set at the Zeebe connection, not worker per worker.
These parameter are
```yaml
      workerExecutionThreads: 100
      workerMaxJobsActive: 100
```

It can be overridden by a environment variable in the Kubernetes file
```yaml

env:
  - name: JAVA_TOOL_OPTIONS
    value: >-
      -Dautomator.startup.serverName=zeebeCloud
      -Dautomator.servers.camunda8.name=zeebeCloud
      -Dautomator.servers.camunda8.workerExecutionThreads=100
```
By this method:
* you set up the server name (`automator.startup.serverName`) to use for the Zeebe Connection to `zeebeCloud`
* then, you qualify the camunda8 variable (`automator.servers.camunda8.name`) with this name. The Zeebe connection can be configured via the `automator.servers.camunda8`variables
* the number of threads are set to 100 via the `automator.servers.camunda8.workerExecutionThreads` variable.

