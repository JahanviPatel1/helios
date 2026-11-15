package com.akshadip.helios.services;

import com.akshadip.helios.dtos.JobExecutionResponseDto;
import com.akshadip.helios.enums.JobStatus;
import com.akshadip.helios.exceptions.JobNotFoundException;
import com.akshadip.helios.models.JobExecution;
import com.akshadip.helios.repositories.JobExecutionRepository;
import com.akshadip.helios.repositories.JobRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class JobExecutionService {
    private final JobExecutionRepository jobExecutionRepository;
    private final JobRepository jobRepository;

    public JobExecutionService(JobExecutionRepository jobExecutionRepository, JobRepository jobRepository) {
        this.jobExecutionRepository = jobExecutionRepository;
        this.jobRepository = jobRepository;
    }

    public UUID recordStart(UUID jobId, int attempt) {
        JobExecution execution = new JobExecution();
        execution.setId(UUID.randomUUID());
        execution.setJobId(jobId);
        execution.setStatus(JobStatus.RUNNING);
        execution.setAttempt(attempt);
        execution.setStartedAt(LocalDateTime.now());
        execution.setCreatedAt(LocalDateTime.now());
        jobExecutionRepository.save(execution);
        return execution.getId();
    }

    public void recordCompletion(UUID executionId, boolean success, String errorMessage) {
        JobExecution execution = jobExecutionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Job execution not found: " + executionId));
        LocalDateTime finishedAt = LocalDateTime.now();
        execution.setFinishedAt(finishedAt);
        execution.setDurationMs(Duration.between(execution.getStartedAt(), finishedAt).toMillis());
        execution.setStatus(success ? JobStatus.COMPLETED : JobStatus.FAILED);
        execution.setErrorMessage(success ? null : errorMessage);
        jobExecutionRepository.save(execution);
    }

    public List<JobExecutionResponseDto> getExecutionsByJobId(UUID jobId) {
        if (!jobRepository.existsById(jobId)) {
            throw new JobNotFoundException(jobId);
        }
        return jobExecutionRepository.findByJobIdOrderByStartedAtDesc(jobId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    private JobExecutionResponseDto toResponseDto(JobExecution execution) {
        JobExecutionResponseDto dto = new JobExecutionResponseDto();
        dto.setId(execution.getId().toString());
        dto.setJobId(execution.getJobId().toString());
        dto.setStatus(execution.getStatus());
        dto.setAttempt(execution.getAttempt());
        dto.setStartedAt(execution.getStartedAt());
        dto.setFinishedAt(execution.getFinishedAt());
        dto.setDurationMs(execution.getDurationMs());
        dto.setErrorMessage(execution.getErrorMessage());
        return dto;
    }
}
