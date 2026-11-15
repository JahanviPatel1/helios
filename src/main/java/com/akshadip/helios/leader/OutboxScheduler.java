package com.akshadip.helios.leader;

import com.akshadip.helios.dtos.JobMessage;
import com.akshadip.helios.enums.OutboxStatus;
import com.akshadip.helios.models.OutboxEvent;
import com.akshadip.helios.services.OutboxEventService;
import com.akshadip.helios.metrics.JobMetrics;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxScheduler {
    private final LeaderElectionService leaderElectionService;
    private final OutboxEventService outboxEventService;
    private final KafkaTemplate<String, JobMessage> kafkaTemplate;
    private final JobMetrics jobMetrics;

    public OutboxScheduler(LeaderElectionService leaderElectionService,
                           OutboxEventService outboxEventService,
                           KafkaTemplate<String, JobMessage> kafkaTemplate,
                           JobMetrics jobMetrics) {
        this.leaderElectionService = leaderElectionService;
        this.kafkaTemplate = kafkaTemplate;
        this.outboxEventService = outboxEventService;
        this.jobMetrics = jobMetrics;
    }

    @Scheduled(fixedRate = 5000)
    public void processOutboxEvents() {
        if(!leaderElectionService.isLeader()) {
            return;
        }
        List<OutboxEvent> events = outboxEventService.getPendingOutboxEvents(100);
        for (OutboxEvent event : events) {
            try {
                JobMessage jobMessage = new JobMessage(event.getJobId());
                kafkaTemplate.send("jobs", jobMessage.getJobId().toString(), jobMessage).get();
                outboxEventService.updateOutboxEventStatus(event.getId(), OutboxStatus.SENT);
                jobMetrics.incrementDispatchedSuccess();
            } catch (Exception e) {
                jobMetrics.incrementDispatchedFailure();
                outboxEventService.incrementRetryCount(event.getId());
            }
        }
    }
}
