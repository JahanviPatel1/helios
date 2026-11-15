package com.akshadip.helios.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Central Micrometer counters for dispatch vs execution throughput.
 *
 * <ul>
 *   <li>helios.jobs.outbox.events - outbox rows created in JobDispatcher</li>
 *   <li>helios.jobs.dispatched (outcome=success|failure) - Kafka publishes in OutboxScheduler</li>
 *   <li>helios.jobs.executions.started - worker picked up message</li>
 *   <li>helios.jobs.executions.completed (result=success|failure) - worker finished</li>
 * </ul>
 *
 * Per-second rates are computed in Prometheus/Grafana, e.g.
 * sum(rate(helios_jobs_dispatched_total[1m])).
 */
@Component
public class JobMetrics {

    private final Counter outboxCreated;
    private final Counter dispatchedSuccess;
    private final Counter dispatchedFailure;
    private final Counter executionsStarted;
    private final Counter executionsCompletedSuccess;
    private final Counter executionsCompletedFailure;

    public JobMetrics(MeterRegistry registry) {
        this.outboxCreated = Counter.builder("helios.jobs.outbox.events")
                .description("Outbox events created by JobDispatcher")
                .register(registry);
        this.dispatchedSuccess = Counter.builder("helios.jobs.dispatched")
                .description("Jobs published to Kafka by OutboxScheduler")
                .tag("outcome", "success")
                .register(registry);
        this.dispatchedFailure = Counter.builder("helios.jobs.dispatched")
                .description("Jobs published to Kafka by OutboxScheduler")
                .tag("outcome", "failure")
                .register(registry);
        this.executionsStarted = Counter.builder("helios.jobs.executions.started")
                .description("Job executions started by WorkerService")
                .register(registry);
        this.executionsCompletedSuccess = Counter.builder("helios.jobs.executions.completed")
                .description("Job executions completed by WorkerService")
                .tag("result", "success")
                .register(registry);
        this.executionsCompletedFailure = Counter.builder("helios.jobs.executions.completed")
                .description("Job executions completed by WorkerService")
                .tag("result", "failure")
                .register(registry);
    }

    public void incrementOutboxCreated() {
        outboxCreated.increment();
    }

    public void incrementDispatchedSuccess() {
        dispatchedSuccess.increment();
    }

    public void incrementDispatchedFailure() {
        dispatchedFailure.increment();
    }

    public void incrementExecutionsStarted() {
        executionsStarted.increment();
    }

    public void incrementExecutionsCompleted(boolean success) {
        if (success) {
            executionsCompletedSuccess.increment();
        } else {
            executionsCompletedFailure.increment();
        }
    }
}
