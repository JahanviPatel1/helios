package com.akshadip.helios.leader;

import com.akshadip.helios.dtos.JobMessage;
import com.akshadip.helios.enums.JobStatus;
import com.akshadip.helios.models.Job;

import com.akshadip.helios.services.JobService;
import com.akshadip.helios.services.OutboxEventService;
import com.akshadip.helios.metrics.JobMetrics;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobDispatcher {
    private static final Logger log = LoggerFactory.getLogger(JobDispatcher.class);
    private final OutboxEventService outboxEventService;
    private final JobService jobService;
    private final JobMetrics jobMetrics;

    public JobDispatcher(OutboxEventService outboxEventService, JobService jobService, JobMetrics jobMetrics) {
        this.outboxEventService = outboxEventService;
        this.jobService = jobService;
        this.jobMetrics = jobMetrics;
    }

    @Transactional
    public void dispatchJob(Job job) {
        log.info("Dispatching job with ID: {}", job.getId());
        // Create an outbox event
        outboxEventService.createOutboxEvent(job.getId());
        jobMetrics.incrementOutboxCreated();

        // update the job status
        jobService.updateJobStatus(job.getId(), JobStatus.DISPATCHED);
    }
}
