# Unit Scenario

## Goal



### Verification

![Process](doc/explanationProcess.png)

in a CD/CI, you want to verify that a process follows the same behavior in the same performance
time. Running every day (or hours) or asking via an API call to replay a scenario is useful to
verify there is no difference. If the customer is 4555, do we still move the process instance to
Review Level 1"? The second verification is the performance. The scenario can record an expected
duration target (for example, 4 seconds to execute the Get Context service task. Does the execution
still at this time?

### Coverage report

Execute multiple scenarios to be sure that all the process is covered correctly. An "Execution
round" is a set of scenarios executed at the same time. At the end of the execution, a coverage test
can be performed. A CD/CI verification may be to check the scenario execution, the target time, and
the coverage.

### Advance process instances to a step for development

During the development, you verify the task "Notify applicant". To test it in the situation, you
must have a process instance in the process and pass four user tasks. Each test takes time: when you
deploy a new process or want a new process instance, you need to execute again the different user
task. Using Automator with the correct scenario solves the issue. Deploy a new process, but instead
of starting from the beginning of a new process instance, start it via Automator. The scenario will
advance the process instance where you want it.


# Build a Scenario

<Soon>

Automator can generate a scenario from a real execution. The user creates a process instance and
executes it. It executes user tasks until the end of the process instance or at a certain point. Via
the UI (or the API), the user gives the process instance. Automator queries Camunda Engine to
collect the history of the process and, for each user task, which variable was provided. A new
scenario is created from this example.


## Scenario definition

## execute