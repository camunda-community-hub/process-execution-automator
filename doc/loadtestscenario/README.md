# Load Test scenario

A load test scenario is different from a unit test.

In a load test, the goal is to mimic the production:

* in terms of process instance creation
* in terms of service tasks, using the actual service task or simulating it

A load test scenario will set up an environment, run it for a delay (30 mn), and check through
objectives. This is, for example, a load test scenario.

* Create 10 process instances par minutes in the process "Loan application"
* simulate the service task "GetCreditScore": each execution needs 2 minutes. Run 2 workers on this
  service task, and for each worker, 10 threads. The worker will return for 80% a credit score lower
  than 500.
* Check the Objective that 10 process instances can be completed every minute

* Because the GetCreditScore is a 2-minute execution, on the first two minutes, the throughput will
  be 0
  (no process instance will be completed), so a warmup of 3 minutes is necessary before monitoring
  the Objective

Process Automator executes processes on a platform. Some services may be available on the platform,
so it is needed to simulate only the missing piece. Conversely, if all services are available, the
scenario will just create process instances and check objectives.

## Tutorial

A complete example of running a scenario and changing the Zeebe Parameters to reach the expected
delay is available in the (Tutorial)[Tutorial.md]

## Create process instance

A load test scenario creates process instances at a frequency. The goal is not to create one process
instance and to follow how it runs but to create multiple process instances. For example, 5 process
instances every 20 seconds. Or 100 process instances every second.

It is possible to create process instances with different values: for example, 5 process instances
every 10 seconds with amount=100, and 6 process instances every 12 seconds with amount=150.

The application uses the Spring Boot Scheduler function. So, at the frequency, it starts to create
process instances. If it doesn't finish the number of process instances at the end of the frequency,
it will not start a new thread; it will spend time creating the requester number. But then, waiting
to create the next batch is not delayed.

To face a large number of creations, it is possible to start multiple threads at the same frequency.
Then, the number of process instances created is the <number> * <number of creatorZ.

If the creator does not create enough process instances, no errors will pop up. Verifying that the
expected number of process instances is created is checking it via the Objective.

## Simulate a service task

A service task can be simulated. In a load test, workers are started and execute tasks when they
appear. The service task uses the topic to search for tasks to execute. It is possible to define the
implementation to use for a service task. CLASSICAL, THREAD, THREADTOKEN. See
the [Scenario manual](doc/scenarioreference/README.md) for an explanation of different modes.

The number of threads and active jobs can be configured when the application starts. These
parameters are defined on the service configuration. To start a simulation with a worker "getScore"
running 100 threads with 200 jobs actives and a worker with different configurations, two
applications must be started with different configurations. Then, filtering each application to run
only one specific worker is necessary.

See [Kubernetes configuration](doc/Kubernetes/README.md) for details, 
and the [Tutorial](doc/loadtestscenario/Tutorial.md) for example.


See the [scenario reference](doc/scenarioreference/README.md) for details on arguments
Example: 
`````yaml
{
"type": "SERVICETASK",
"topic": "crawl-search",
"waitingTime": "PT10S",
"modeExecution": "ASYNCHRONOUS"
}
`````




## Simulate a user task

Process Execution Automator can simulate a user task. The simulation specifies the number of users
and the duration needed to execute the task. Specifying a pool of 50 users, which requires 2 minutes
to complete each task, is possible.

The simulator runs a "getListTask", then an "assignTask", waits for the duration, and executes a 
"completeTask".

See the [scenario reference](doc/scenarioreference/README.md) for details on arguments

`````yaml
{
"type": "USERTASK",
"taskId": "Activity_Verify",
"waitingTime": "PT10S",
"modeExecution": "ASYNCHRONOUS",
"variables": {
"processAcceptable": true
}
`````


## Objectives

A load test flow starts all workers and creates multiple process instances at a specific frequency.
Hoz to mesure the success?

Looking at the metrics during the load test is possible. Some objectives can be set too.

The load test runs for a duration. Then, at the end of this duration, the Process Execution
Automator stops generating process instances and stops workers. If some workers are executing a
task, they have time to finish it.

Process Execution Automator waits for one minute (in the log, the label "collecting data" is
visible). This time is necessary because Operate needs time to index the last execution. On a very
heavy load test, Operate needs multiple minutes to index the last execution.

Then, all objectives are checked, and a status is given for each Objective.

### CREATED Objective

Measure the number of process instances created in a process instance. Process Automator keeps the
time when it starts the campaign, search the number of process instances created AFTER this time.

Only root process instances are searched. If a process calls a sub-process, digging in the
sub-process returns 0 (except if some process instances are created explicitly in this process)

| Argument  | Explanation                       | Default value |
|-----------|-----------------------------------|---------------|
| type      | "CREATED"                         |               |
| label     | Label of objective                |               |
| processId | Process Id to search              |               |
| value     | Value to reach, for example, 1000 |               |
| comment   | Comment displayed in the log      |               | 

### USERTASK objective

Measure the number of tasks in a specific user task. This number is not the number of tasks executed
in this activity but the number of tasks at the end of the load test.

The process automation does not filter the user task by the creation time, so if a test should run
multiple times, the user task must be purged before the new test.

| Argument  | Explanation                       | Default value |
|-----------|-----------------------------------|---------------|
| type      | "USERTASK"                        |               |
| label     | Label of objective                |               |
| processId | Process Id to search              |               |
| taskId    | Id of the task                    |               |
| value     | Value to reach, for example, 1000 |               |
| comment   | Comment displayed in the log      |               | 

## FLOWRATEUSERTASKMN Rate on user task

The Objective is to check the rate of incoming tasks in a user task. The scenario expects to receive
120 tasks per minute on a user task.

Process Execution Automator checks minute per minute during the load test, and the expected number
shows up.

The flow may not be constant: in a period, 118 tasks may show up, and in the next period, 124. The
standard deviation rate takes care of this variation to verify that the flow is still in the
acceptable range.

| Argument          | Explanation                              | Default value |
|-------------------|------------------------------------------|---------------|
| type              | "FLOWRATEUSERTASKMN"                     |               |
| label             | Label of objective                       |               |
| processId         | Process Id to search                     |               |
| taskId            | Id of the task                           |               |
| value             | Value expected per minute                |               |
| standardDeviation | standard deviation expected, in percent. | 0 (for 0 %)   |
| comment           | Comment displayed in the log             |               | 

### ENDED Objective

Measure the number of process instances that ended in a process instance. Process Automator keeps
the time when it starts the campaign, search the number of process instances that ended AFTER this
time.

| Argument  | Explanation                       | Default value |
|-----------|-----------------------------------|---------------|
| type      | "ENDED"                           |               |
| label     | Label of objective                |               |
| processId | Process Id to search              |               |
| value     | Value to reach, for example, 1000 |               |
| comment   | Comment displayed in the log      |               | 

### Complete example

This load test runs for 10 minutes and has four objectives.

Note: Objective verifies the result in multiple processes. The process should create (via a message)
some instances in different processes.

```yaml

"flowControl": {
  "duration": "PT10M",
  "objectives": [
    {
      "label": "Creation",
      "processId": "CrawlUrl",
      "type": "CREATED",
      "value": 4000,
      "comment": "100/30 s. Duration=10M => 2000"
    },
    {
      "label": "Ended",
      "processId": "CrawlUrl",
      "type": "ENDED",
      "value": 4000,
      "comment": "Same as creation"
    },
    {
      "label": "Ended (UserTask TheEnd) Verification",
      "processId": "DiscoveryHostExtraction",
      "type": "USERTASK",
      "taskId": "Activity_HostExtraction_TheEnd",
      "value": 150
    },
    {
      "label": "Flow per minute",
      "processId": "DiscoveryHostExtraction",
      "type": "FLOWRATEUSERTASKMN",
      "taskId": "Activity_HostExtraction_TheEnd",
      "standardDeviation": 10,
      "value": 15
    }

  ]
}
```

## Warmup

A process can start with a 2-minute service task, followed by a 5-minute service task.

To check the objective, the process must be warmup: in that situation, process instances must be
created, and it must wait 5 minutes to start to check the Objective. Otherwise, on a 10-minute test,
process instances ended will show up only after 7 minutes and are not representative.

The warmup phase reacts as the expected behavior and will finish after a duration or when a
condition is met (mainly, one process instance ends or shows up in a user task).

Objective records (rate, number of process instances available in a user task) starts after the
warmup. The duration begins after the warmup, too.


| Argument        | Explanation                                             | Default value |
|-----------------|---------------------------------------------------------|---------------|
| duration        | maximum duration for the warming up. May finish before. |               |
| useServiceTasks | All Service Tasks declared in the Flow are started      | false         |
| useUserTasks    | All User Tasks declared in the Flow are started         | false         |
| operations      | List of operations                                      |               |

Operations contain elements to run during the warm-up. It may contain some operations like USERTASK, SERVICETASK, or STARTEVENT.

To not duplicate USERTASK and SERVICETASK, the flag `useServiceTask` and/or `useUserTasks` can be set to true.

On the operation, an endWarmingUp argument can be declared. This argument specifies a function.
When the function returns true, the operation is marked as "warm up" but continues to process.
When all operations are marked "Warm up" or the duration is over, the warm-up is finished, and the
load test start

On operation, see the [scenario reference](doc/scenarioreference/README.md) for details on arguments

The new argument is

| Argument      | Explanation                                            | Default value |
|---------------|--------------------------------------------------------|---------------|
| endWarmingUp  | Function to check if the operation is marked as ended  |               |


the [Scenario manual](doc/scenarioreference/README.md) for the list of all functions.