package com.akshadip.helios.controllers;

import com.akshadip.helios.dtos.JobExecutionResponseDto;
import com.akshadip.helios.dtos.JobRequestDto;
import com.akshadip.helios.dtos.JobResponseDto;
import com.akshadip.helios.dtos.JobUpdateDto;
import com.akshadip.helios.models.Job;
import com.akshadip.helios.services.JobExecutionService;
import com.akshadip.helios.services.JobService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class JobController {
    JobService jobService;
    JobExecutionService jobExecutionService;

    public JobController(JobService jobService, JobExecutionService jobExecutionService) {
        this.jobService = jobService;
        this.jobExecutionService = jobExecutionService;
    }

    @PostMapping("/jobs/create")
    public JobResponseDto createJob(@Valid @RequestBody JobRequestDto jobRequestDto) {
        return jobService.createJob(jobRequestDto);
    }

    @GetMapping("/jobs/{id}")
    public Job getJobById(@PathVariable String id) {
        return jobService.getJobById(UUID.fromString(id));
    }

    @GetMapping("/jobs")
    public java.util.List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PutMapping("/jobs/{id}")
    public JobResponseDto updateJob(@PathVariable String id, @Valid @RequestBody JobUpdateDto jobUpdateDto) {
        return jobService.updateJob(UUID.fromString(id), jobUpdateDto);
    }

    @DeleteMapping("/jobs/{id}")
    public String deleteJobById(@PathVariable String id) {
        jobService.deleteJob(UUID.fromString(id));
        return "Job with id " + id + " deleted successfully";
    }

    @PostMapping("/jobs/{id}/pause")
    public JobResponseDto pauseJob(@PathVariable String id) {
        return jobService.pauseJob(UUID.fromString(id));
    }

    @PostMapping("/jobs/{id}/resume")
    public JobResponseDto resumeJob(@PathVariable String id) {
        return jobService.resumeJob(UUID.fromString(id));
    }

    @PostMapping("/jobs/create/bulk")
    public List<JobResponseDto> createJobsBulk(@Valid @RequestBody java.util.List<JobRequestDto> jobRequestDtos) {
        return jobService.createJobsBulk(jobRequestDtos);
    }

    @GetMapping("/jobs/{id}/executions")
    public List<JobExecutionResponseDto> getJobExecutions(@PathVariable String id) {
        return jobExecutionService.getExecutionsByJobId(UUID.fromString(id));
    }
}
