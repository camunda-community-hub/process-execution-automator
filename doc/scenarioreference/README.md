# Scenario reference



A scenario is a JSON file. A scenario explains one execution, from the process creation until a
point. It may not be the end of the process: Automator can be used to advance process instances
until a specific task. It contains:

* Information on the process: which process has to start? Some information on a delay between two
  creations can be set
* Service task can be registered: Automator will check the process instance executes the task (but
  does not execute it)
* The end event can be registered to verify that the process goes to the end The process instance
  can execute other tasks: Automator does not verify that, except if the "mode verification" is set
  to "Strict."


# Different BPM

The Automator executes a process instance. It does not care about the definition of the process:
does the process instance call a sub-process? An Event sub-process? It does not matter.

## Call Activity and sub-process

Automator does not care about these artifacts. An execution is a suite of Activities. These
activities are in the process, or a sub-process does not change the execution.

## User Multi-instance

A process can have a multi-instances task. In the scenario, each task may have multiple executions.
It is possible to execute a multi-instance and give different values for each execution.

## External operation

A scenario may consist of executing some task and then sending a Cancellation message or starting a
process instance in a different process to get a Cancellation message. This is possible to describe
this operation in a step.

## Example

`````json


{
  "name": "execution Round 14",
  "version": "1.2",
  "processId": "MergingInclusive",
  "executions": [
    {
      "name": "multinstance",
      "policy": "STOPATFIRSTERROR",
      "numberProcessInstances": 100,
      "numberOfThreads": 5,
      "steps": [
        {
          "type": "STARTEVENT",
          "taskId": "StartEvent_1"
        },
        {
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

## Execution parameters

| Parameter              | Explanation                                                                                            | Example                         |
|------------------------|--------------------------------------------------------------------------------------------------------|---------------------------------|
| Name                   | Name of execution                                                                                      | "name": "This is the first run" |
| policy                 | "STOPATFIRSTERROR" or "CONTINUE": in case of error, what is the next move. Default is STOPATFIRSTERROR | "policy": "STOPATFIRSTERROR"    |
| numberProcessInstances | Number of process instance to create. Each process instance follows steps.             | "numberProcessInstances": 45    |
| numberOfThreads        | Number of thread to execute in parallel. Default is 1.                                        | "numberOfThreads": 5            |
| execution              | if false, the execution does not start. Unot present, the default value is TRUE.                       | "execution" : false             | 

Then the execution contains a list of steps

## STARTEVENT step

Start a new process instance

| Parameter          | Explanation                   | Example                   |
|--------------------|-------------------------------|---------------------------|
| type               | Specify the type (STARTEVENT) | type: "STARTEVENT"        |
| taskId         | Activity ID of start event    | actiityId= "StartEvent_1" |

## USERTASK step

The step wait for a user task, and execute it.

| Parameter          | Explanation                                                                                                                              | Example                                                               |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| type               | Specify the type (USERTASK)                                                                                                              | type: "USERTASK"                                                      |
| delay              | Deplay to wait before looking for the task, in ISO 8601                                                                                  | delay="PT0.1S" waits 100 ms                                           |
| waitingTime        | Wait maximum this time, before returning an error. Automator query the engine every 500 ms, until this delay. Default value is 5 minutes | waitingTime="PT10S"                                                   |
| taskId         | Activity ID to query                                                                                                                     | actiityId= "review"                                                   |
| variables          | List of variable (JSON file) to update                                                                                                   | {"amount": 450, "account": "myBankAccount", "colors": ["blue","red"]} |
| numberOfExecutions | Number of execution, the task may be multi instance. Default is 1                                                                        | numberOfExecutions = 3                                                |

## SERVICETASK step

The step wait for a service task, and execute it.

It's depends on the usage of the scenario: if a CD/CI, the service task should be executed by the
real workers, not by the automator But in some environment, or to advance quickly the task to a
certain position, you may want to simulate the worker. Then, the automator can execute a service
task. The real worker should be deactivate then. If the service task is not found, then the scenario
will have an error.

| Parameter          | Explanation                                                                                                                              | Example                                                               |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------|
| type               | Specify the type (SERVICETASK)                                                                                                           | type: "USERTASK"                                                      |
| delay              | Deplay to wait before looking for the task, in ISO 8601                                                                                  | delay="PT0.1S" waits 100 ms                                           |
| waitingTime        | Wait maximum this time, before returning an error. Automator query the engine every 500 ms, until this delay. Default value is 5 minutes | waitingTime="PT10S"                                                   |
| taskId             | Activity ID to query                                                                                                                     | actiityId= "review"                                                   |
| topic              | Topic to search the task (mandatory in C8)                                                                                               | get-score                                                             | 
| variables          | List of variable (JSON file) to update                                                                                                   | {"amount": 450, "account": "myBankAccount", "colors": ["blue","red"]} |
| numberOfExecutions | Number of execution, the task may be multi instance. Default is 1                                                                        | numberOfExecutions = 3                                                |

## Verification

Each execution can declare verifications. Verification are executed after the execution.

It's possible to check:

* active activity: does the process instance is correctly waiting on the task "Final Review" after
  the execution?
* any completed activity : does the process instance executed the task "GetScore"? Does the process
  instance ended on the end event "Application Done" ?
* any variable value : does the process variable "ApplicantScore" is 150?
* any performance: does the execution of the activity "getScore" stay under 500 milliseconds? Does
  the execution from the activity "getScore" to "GetRiskLevel" stay under 4 seconds?
