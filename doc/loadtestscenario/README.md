# Load Test scenario

A load test scenario is different than a unit test. In a load test, the goal is to mimic the production:

* in term on process instance creation
* in term of service task, using the real service task or simulate it

A load test scenario will set up an environment, and run it for a delay (like 30 mn) and check
throuput objectives. This is for example a load test scenario

* Create 10 process instances par minutes in the process "Loan application"
* simulate the service task "GetCreditScore": each execution needs 2 minutes to complete. 
Run 2 workers on this service task, and for each worker, 10 threads. 
The worker will return in 80% a credit score lower than 500.
* Check the objective that 10 process instances can be completed every minutes   

* Because the GetCreditScore is a 2 minutes execution, on the first two minutes, the throuput will be 0 
(no process instance will be completed), so a warmup of 3 minutes is necessary before monitoring 
the objective

Process Automator execute processes on a platform. Some services may be available on the platform, so
it is needed to simulate only the missing piece. On the opposite, if all services are available, the 
scenario will just creates process instances and check objectives.

## Tutorial
A complete example to run a scenario, and change the Zeebe Parameters to reach the expecting delay is
available in the (Tutorial)[Tutorial.md]

## Create process instance

## Simulate a service task

## Simulate a user task

## Objectives

