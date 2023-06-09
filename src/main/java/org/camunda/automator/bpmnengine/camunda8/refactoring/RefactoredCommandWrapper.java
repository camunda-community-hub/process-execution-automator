package org.camunda.automator.bpmnengine.camunda8.refactoring;

import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.FinalCommandStep;
import io.camunda.zeebe.client.api.worker.BackoffSupplier;
import io.camunda.zeebe.spring.client.jobhandling.CommandWrapper;
import io.camunda.zeebe.spring.client.jobhandling.DefaultCommandExceptionHandlingStrategy;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Copied from CommandWrapper from spring-zeebe. Refactor over there to be able to use built-in stuff directly
 */
public class RefactoredCommandWrapper extends CommandWrapper {

  private final FinalCommandStep<Void> command;
  private final long deadline;
  private final String entityLogInfo;
  private final DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy;
  private final int maxRetries = 20;
  private long currentRetryDelay = 50L;
  private int invocationCounter = 0;

  public RefactoredCommandWrapper(FinalCommandStep<Void> command,
                                  long deadline,
                                  String entityLogInfo,
                                  DefaultCommandExceptionHandlingStrategy commandExceptionHandlingStrategy) {
    super(command, null, commandExceptionHandlingStrategy);
    this.command = command;
    this.deadline = deadline;
    this.entityLogInfo = entityLogInfo;
    this.commandExceptionHandlingStrategy = commandExceptionHandlingStrategy;
  }

  @Override
  public void executeAsync() {
    ++this.invocationCounter;
    this.command.send().exceptionally(t -> {
      this.commandExceptionHandlingStrategy.handleCommandError(this, t);
      return null;
    });
  }

  public Object executeSync() {
    ++this.invocationCounter;
    ZeebeFuture<Void> zeebeFutur = this.command.send();

    zeebeFutur.exceptionally(t -> {
      this.commandExceptionHandlingStrategy.handleCommandError(this, t);
      return null;
    });
    return zeebeFutur.join();
  }

  @Override
  public void increaseBackoffUsing(BackoffSupplier backoffSupplier) {
    this.currentRetryDelay = backoffSupplier.supplyRetryDelay(this.currentRetryDelay);
  }

  @Override
  public void scheduleExecutionUsing(ScheduledExecutorService scheduledExecutorService) {
    scheduledExecutorService.schedule(this::executeAsync, this.currentRetryDelay, TimeUnit.MILLISECONDS);
  }

  @Override
  public String toString() {
    return "{command=" + this.command.getClass() + ", entity=" + this.entityLogInfo + ", currentRetryDelay="
        + this.currentRetryDelay + '}';
  }

  @Override
  public boolean hasMoreRetries() {
    if (this.jobDeadlineExceeded()) {
      return false;
    } else {
      return this.invocationCounter < this.maxRetries;
    }
  }

  @Override
  public boolean jobDeadlineExceeded() {
    return Instant.now().getEpochSecond() > this.deadline;
  }

}
