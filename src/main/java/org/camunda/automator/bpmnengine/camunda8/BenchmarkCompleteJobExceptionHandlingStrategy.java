package org.camunda.automator.bpmnengine.camunda8;

import io.camunda.zeebe.client.api.worker.BackoffSupplier;
import io.camunda.zeebe.spring.client.jobhandling.CommandWrapper;
import io.camunda.zeebe.spring.client.jobhandling.DefaultCommandExceptionHandlingStrategy;
import io.grpc.StatusRuntimeException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Primary
@Component
public class BenchmarkCompleteJobExceptionHandlingStrategy extends DefaultCommandExceptionHandlingStrategy {

  @Autowired
  private StatisticsCollector stats;

  public BenchmarkCompleteJobExceptionHandlingStrategy(@Autowired BackoffSupplier backoffSupplier) {
    super(backoffSupplier, Executors.newScheduledThreadPool(1));
  }

  @Override
  public void handleCommandError(CommandWrapper command, Throwable throwable) {
    if (StatusRuntimeException.class.isAssignableFrom(throwable.getClass())) {
      StatusRuntimeException exception = (StatusRuntimeException) throwable;
      stats.incCompletedJobsException(exception.getStatus().getCode().name());
            /* Backpressure on Job completion cannot happen at the moment (whitelisted)
            if (Status.Code.RESOURCE_EXHAUSTED == exception.getStatus().getCode()) {
                stats.getBackpressureOnJobCompleteMeter().mark();
                return;
            }*/
    } else {
      stats.incCompletedJobsException(throwable.getMessage());
    }

    // use normal behavior, e.g. increasing back-off for backpressure
    super.handleCommandError(command, throwable);
  }
}
