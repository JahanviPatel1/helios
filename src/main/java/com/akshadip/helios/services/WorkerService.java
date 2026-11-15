package com.akshadip.helios.services;

import com.akshadip.helios.dtos.JobMessage;
import com.akshadip.helios.enums.JobStatus;
import com.akshadip.helios.models.Job;
import com.akshadip.helios.scheduler.cron.CronCalculator;
import com.akshadip.helios.metrics.JobMetrics;
import com.akshadip.helios.worker.HttpJobExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class WorkerService {

    private static final Logger log = LoggerFactory.getLogger(WorkerService.class);
    private final HttpJobExecutor httpJobExecutor;
    private final CronCalculator cronCalculator;
    private final JobService jobService;
    private final JobExecutionService jobExecutionService;
    private final JobMetrics jobMetrics;

    public WorkerService(HttpJobExecutor httpJobExecutor, CronCalculator cronCalculator, JobService jobService, JobExecutionService jobExecutionService, JobMetrics jobMetrics) {
        this.jobService = jobService;
        this.httpJobExecutor = httpJobExecutor;
        this.cronCalculator = cronCalculator;
        this.jobExecutionService = jobExecutionService;
        this.jobMetrics = jobMetrics;

    }

    @KafkaListener(
            id = "worker-listener",
            topics = "jobs",
            groupId = "worker-group")
    public void executeJob(JobMessage jobMessage) {
        log.info("Received job message: {}", jobMessage);
        Job job = jobService.getJobById(jobMessage.getJobId());
        int attempt = job.getRetryCount() != null ? job.getRetryCount() : 0;
        UUID executionId = jobExecutionService.recordStart(job.getId(), attempt);
        jobMetrics.incrementExecutionsStarted();
        try {
            httpJobExecutor.executeJob(job.getPayload());
            jobExecutionService.recordCompletion(executionId, true, null);
            jobMetrics.incrementExecutionsCompleted(true);
            LocalDateTime nextFireAt = cronCalculator.getNextFireTime(job.getCronExpression(), LocalDateTime.now());
            job.setNextFireAt(nextFireAt);
            job.setLastFireAt(LocalDateTime.now());
            job.setStatus(JobStatus.PENDING);
        } catch (Exception e) {
            log.info("Job execution failed for job ID: {}, error: {}", job.getId(), e.getMessage());
            jobExecutionService.recordCompletion(executionId, false, e.getMessage());
            jobMetrics.incrementExecutionsCompleted(false);
            job.setRetryCount(job.getRetryCount() + 1);
            if (job.getRetryCount() >= job.getMaxRetries()) {
                job.setStatus(JobStatus.FAILED);
            } else {
                job.setStatus(JobStatus.PENDING);
            }
        } finally {
            jobService.updateJob(job);
        }

    }
}
