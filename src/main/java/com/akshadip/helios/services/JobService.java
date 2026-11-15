package com.akshadip.helios.services;


import com.akshadip.helios.dtos.JobRequestDto;
import com.akshadip.helios.dtos.JobResponseDto;
import com.akshadip.helios.dtos.JobUpdateDto;
import com.akshadip.helios.enums.JobStatus;
import com.akshadip.helios.exceptions.InvalidJobRequestException;
import com.akshadip.helios.exceptions.JobNotFoundException;
import com.akshadip.helios.mappers.JobMapper;
import com.akshadip.helios.models.Job;
import com.akshadip.helios.repositories.JobRepository;
import com.akshadip.helios.scheduler.cron.CronCalculator;
import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Getter
@Setter
public class JobService {
    private JobRepository jobRepository;
    private CronCalculator cronCalculator;
    private JobMapper jobMapper;

    JobService(JobRepository jobRepository, CronCalculator cronCalculator, JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.cronCalculator = cronCalculator;
        this.jobMapper = jobMapper;
    }

    public JobResponseDto createJob(JobRequestDto jobRequestDto) {
        validateCronExpression(jobRequestDto.getCronExpression());
        LocalDateTime now = LocalDateTime.now();
        return getJobResponseDto(now, jobRequestDto);
    }

    public Job getJobById(UUID id) {
        return jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }


    public void deleteJob(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        jobRepository.delete(job);
    }

    public void updateJob(Job job) {
        jobRepository.save(job);
    }

    public JobResponseDto updateJob(UUID id, JobUpdateDto jobUpdateDto) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));

        if (jobUpdateDto.getName() != null) {
            job.setName(jobUpdateDto.getName());
        }
        if (jobUpdateDto.getJobType() != null) {
            job.setJobType(jobUpdateDto.getJobType());
        }
        if (jobUpdateDto.getPayload() != null) {
            job.setPayload(jobUpdateDto.getPayload());
        }
        if (jobUpdateDto.getMaxRetries() != null) {
            job.setMaxRetries(jobUpdateDto.getMaxRetries());
        }
        if (jobUpdateDto.getCronExpression() != null) {
            validateCronExpression(jobUpdateDto.getCronExpression());
            job.setCronExpression(jobUpdateDto.getCronExpression());
            job.setNextFireAt(cronCalculator.getNextFireTime(jobUpdateDto.getCronExpression(), LocalDateTime.now()));
        }

        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }


    public void updateJobStatus(UUID jobId, JobStatus status){
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new JobNotFoundException(jobId));
        job.setStatus(status);
    }

    public JobResponseDto pauseJob(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        if (job.getStatus() == JobStatus.PAUSED) {
            return jobMapper.toJobResponse(job);
        }
        job.setStatus(JobStatus.PAUSED);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }

    public JobResponseDto resumeJob(UUID id) {
        Job job = jobRepository.findById(id).orElseThrow(() -> new JobNotFoundException(id));
        if (job.getStatus() != JobStatus.PAUSED) {
            return jobMapper.toJobResponse(job);
        }
        job.setStatus(JobStatus.PENDING);
        job.setUpdatedAt(LocalDateTime.now());
        job.setNextFireAt(cronCalculator.getNextFireTime(job.getCronExpression(), LocalDateTime.now()));
        jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }

    public List<JobResponseDto> createJobsBulk(List<JobRequestDto> jobRequestDtos) {
        LocalDateTime now = LocalDateTime.now();
        return jobRequestDtos.stream().map(jobRequestDto -> {
            return getJobResponseDto(now, jobRequestDto);
        }).toList();
    }

    private void validateCronExpression(String cronExpression) {
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new InvalidJobRequestException("Invalid cron expression: " + cronExpression);
        }
    }

    private JobResponseDto getJobResponseDto(LocalDateTime now, JobRequestDto jobRequestDto) {
        Job job = new Job();
        job.setId(UUID.randomUUID());
        job.setName(jobRequestDto.getName());
        job.setCronExpression(jobRequestDto.getCronExpression());
        job.setJobType(jobRequestDto.getJobType());
        job.setPayload(jobRequestDto.getPayload());
        job.setMaxRetries(jobRequestDto.getMaxRetries());
        job.setCreatedAt(now);
        job.setUpdatedAt(now);
        job.setRetryCount(0);
        job.setStatus(JobStatus.PENDING);
        job.setNextFireAt(cronCalculator.getNextFireTime(jobRequestDto.getCronExpression(), now));
        jobRepository.save(job);
        return jobMapper.toJobResponse(job);
    }

}
