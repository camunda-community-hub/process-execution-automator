# How to conduct a load test

#  Introduction

Zeebe engine has different parameters to fit the performance.

* The number of partitions is a primary parameter. 

Process instances are distributed against partitions. 
The more partitions you have, the more cases you can handle simultaneously. 
But, having too many partitions implied a delay in service tasks: to search for a job, they must address all partitions.

* The cluster size, which is the number of pods you created to host the Zeebe Engine
* The service task: How many workers do you need? More workers mean the throughput increases, 
but the network loads increase simultaneously on the ZeebeEngine and then on Zeebe.

* Data is exported in ElasticSearch and reindexed by Operate to display them. 
Multiple ElasticSearch and Operate pods may be needed.

The best way to find the correct configuration is to simulate the process load. The peak load must be used because changing the number of partitions is impossible. Then, downsizing the number of nodes (but not the partition/cluster size) is possible.

This is why identifying the goal is crucial.

# Identify the goal

The goal must be identified carefully to absorb the three next years, plus a margin. 
Keep in mind this is not possible at the moment to change the main parameters of a cluster (partitions, cluster size).
Do not overload the goal: you may result in a large cluster, costly, when actually it is not necessary.


## Number of process instances completed per period

The number of process instance created and executed for a period.

Example:
* 	10000 process instances per day, in 20 hours, regularly

*	Or 4000 process instances per day, but the peak to absorb is 120 process instances per second for a 2 hours durations

if the platform must absobe the peak, then the throughput in the peak is your goal.


## Latency

The latency is the time for one process instance to be completed. The goal maybe to create and complete 120 process instances per seconds, 
and 95% of the completion must be done under 4 minutes.
A platform can face this thouput but execution may take more time, due to the batch mechanism. Respecting a hight latency means in general 
increase the platform ressource, to ensure all execution run as fast as possible.

# Inputs

Inputs to simulate the load are mandatory:

* the different processes running on the platform

* The load on each process: how many process instances per period of time to create

* Data are essential, mainly if the process contains multi-instance tasks (one process instance may generate 100 service tasks)

* The service task and the execution time for each service task. This is important to size the number of workers.
For example, a task may be called 2000 times per minute, but its execution requires 1 minute. 
The workload is then 2000 * 1 mn every minute: 2000 pods worker are necessary (or 20 pods worker with 100 threads) to address that.
More pod worker you have in the platform, more pressure you put ohn the Zeebe engine, and you may need more partitions to absorbe this pressure.


With this information, a platform test can be set up, and a tool can load the platform. Service tasks can be simulated. 

# Tooling

it's important to be able to run a load test on demand.
Your platform is up and running. You deployed the process on the platform. How to run a load test?
Two tools exist:
C8 Benchmak
This tool creates process instance on the flow, and will increase the frequency of creation: start creating 10 process instance per seconds, and increase the frequency to 15, 2) and so on. The goal is to find the maximum load the platform can handle at one moment, performance reach a plateau. With this tool, you can simulate some service tasks, but the simulation are in “synchronous thread mode”
Process Automator
This tool is used to stick the load test as close as possible of the reality. You create a scenario, asking to create 10 process per seconds, or with a different frequency, for example 10 process instance per 15 minutes.
It can simulate user, executing user task at a certain frequency.
It has different way to simulate the service task: synchronously, synchronous thread and synchronous limited thread mode. It is possible to update variable during the service task.
It is possible to execute at the same time multiple scenarios, to load multiple processes with different frequency.
Lest but not the least, the tool can simulate a platform: you want to setup 100 pods running the service task “credit-charging” and then 200 pods running “customer-credit”? The tool accepts the configuration to execute this configuration. Then, you can see how Zeebe react when 300 pods request jobs via the gateway, to size correctly the gateway component. In the same way, you can select the number of threads on each simulation service tasks.

# Worker implementation


5.1	Synchronous, Synchronous thread, synchronous limited thread, asynchronous
Synchronous, Synchronous thread, synchronous limited thread, asynchronous
When you execute a service task, you can set up multiple threads and ask in one-time multiple jobs.
Let’s say you set up, for the service task “credit-charging”, 3 threads and 3 jobs at each time. This service task takes 1 to 5 seconds to answer.
Synchronous
In the synchronous mode, the service task “handles” the call, and the execution is in the handle method.
Doing that, Zeebe's client
•	Request 3 jobs
•	Send 3 jobs in 3 different threads (call handle() method)
•	Wait until the 3 jobs are finished to ask again for a new batch of 3 jobs.
If the execution varies between 1 to 5 seconds, it will wait for the longest execution to ask again for the next job: it will wait 5 seconds. So, the pod may have a low CPU time execution.

Some threads will not be used when a new job is requested. So, the worker does not work with a 100%efficiency.
•	To have 100% efficiency, use only one thread.
•	Or request a batch higher than the number of threads: Thread 1 will pick up a new job when it finishes. The issue is still here at the end of the batch.
Synchronous Thread
A solution consists of creating a new thread in the handle() method to realize the work. Then, all handle() methods were executed very fast, and Zeebe asked for a new batch.

Doing that, there is no more limitation if one job needs more time. The Java Client will immediately ask again for a new job with Zeebe.
A lot of threads are created, and the main issue is the overflow on the Java machine. If the management consists of sending a request to an external service and having one thread to capture the answer, this is acceptable. But if the management consists of executing a Java execution, this method can overflow the Java machine.
Synchronous Limited Thread
To remove the main issue in the precedent implementation, the idea is to control the number of threads that can be executed at a time. The Concurrent Java class is used to manage a limited number of tokens. To create a new thread, the handle() method must first get a token. If it can’t get one, it will wait, and the handle() method is frozen. Then, the zeebeClient will stop to request a new job for Zeebe.

Asynchronous call
The asynchronous call consists of sending the complete() feedback before the execution of the worker. This implementation has this aspect:
•	The process execution is faster: the task is immediately released, and the process instance can advance to the next step, even if the treatment is not performed.
•	Because the feedback is sent before the treatment, it is not possible to return any values. The process instance is already advanced, and it may be finished.
•	Does the treatment face an issue, is it not possible to send an error or ask for a retry?
•	The treatment is immediately done, but the ZeebeClient will not ask for a new Job until all handle() methods are finished
The Asynchronous call is an option to implement a worker, but the number of concerns is very important, and this implementation is not recommended.
Conclusion
The synchronous implementation is simple and should handle 80% of the use cases.
If the number of tasks to execute is important, and the treatment may vary from one task to another, then the Synchronous Limited Thread is the best option. This implementation works even when treatment has the same time) increase the efficiency by 30%. Then, to handle a throughput, the number of pods to handle it can be reduced by 30% The limited implementation is the best because the non-limited can arrive to have a pod with exhausted resources, and a local test can hide the issue is the number of tasks is not so important.



# Principle
Change one parameter at a time. To see the impact of a change, chaing multiple parameters between two executions can't help to understand the impact on one parameter.

Choose a raisonable time to run a test. Calculate the time to execute one process instance: for example, additionning the time of all service task, a process instance needs 4 minutes.
This implie a warming up of 4 minutes minimum, adding 20% means 5 minutes.
With Zeebe, when a worker does not have anything to do, it will sleep a little before askeing. This means, when a tasks arrived, nothing may arrive before 10 or 20 seconds. 
When there is multiple workers on a service task, this means enought tasks must arrive to wake up all workers. This is why during the warmup, the task thougput inrease slowly. 
To mesure the througput, this warmup period must be over.

using a too long time is Counterproductive: you reduce the number of test you can run.

At the end, when you fit the expected thorugput, run a long test (1 or 2 hours): some issue may be visible after a period of time. if the operate import does not follow the thougout, it's hard to detect it. if the cluster is build on some instable performance ressource, the issue may not be visible in a 10 minutes test.



# How to start
First metric: the number of tasks per seconds. Divide it by 150


# Understand the main concept

## Partitions – why to add a new partition?

## Cluster size

## Backpressure

## Jobs throupught

## GRPC latency



## Worker – synchronous or asynchronous

## Flows: Zeebe, Exporter, Reindex



# Action-Reaction

## No back pressure, but throughput is lower than expected

## Backpressure

## Platform is not stable

![NotStable_GrpcLatency.png](images/NotStable_GrpcLatency.png)

![notStable_Throughput.png](images/notStable_Throughput.png)

![NotStable_backpressure.png](images/NotStable_backpressure.png)


## Low GRPC throughput

![LowGrpc.png](images%2FLowGrpc.png)

## Operate is behind the reality
