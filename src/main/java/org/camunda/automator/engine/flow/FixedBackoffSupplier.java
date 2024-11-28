/* ******************************************************************** */
/*                                                                      */
/*  FixedBackoffSupplier                                                */
/*                                                                      */
/*  A backoff based on a fixed delay                                    */
/* ******************************************************************** */
package org.camunda.automator.engine.flow;

import io.camunda.zeebe.client.api.worker.BackoffSupplier;

public class FixedBackoffSupplier implements BackoffSupplier {

    private long fixedBackOffDelay = 0;

    public FixedBackoffSupplier(long fixedBackOffDelay) {
        this.fixedBackOffDelay = fixedBackOffDelay;
    }

    @Override
    public long supplyRetryDelay(long currentRetryDelay) {
        return fixedBackOffDelay;
    }
}