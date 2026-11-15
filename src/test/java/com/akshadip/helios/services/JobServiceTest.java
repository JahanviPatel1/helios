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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private CronCalculator cronCalculator;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    private JobRequestDto createRequestDto() {
        JobRequestDto dto = new JobRequestDto();
        dto.setName("test-job");
        dto.setCronExpression("0 * * * * *");
        dto.setJobType("HTTP");
        dto.setMaxRetries(3);
        dto.setPayload("{\"url\":\"http://example.com\"}");
        return dto;
    }

    @Test
    void createJobShouldSaveJobWithCorrectValues() {
        JobRequestDto requestDto = createRequestDto();
        LocalDateTime nextFire = LocalDateTime.of(2026, 8, 10, 12, 0, 0);
        when(cronCalculator.getNextFireTime(any(String.class), any(LocalDateTime.class))).thenReturn(nextFire);
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        jobService.createJob(requestDto);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertNotNull(saved.getId());
        assertEquals("test-job", saved.getName());
        assertEquals("0 * * * * *", saved.getCronExpression());
        assertEquals("HTTP", saved.getJobType());
        assertEquals(0, saved.getRetryCount());
        assertEquals(JobStatus.PENDING, saved.getStatus());
        assertEquals(nextFire, saved.getNextFireAt());
    }

    @Test
    void getJobByIdShouldReturnJobWhenFound() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        Job result = jobService.getJobById(id);

        assertEquals(id, result.getId());
    }

    @Test
    void getJobByIdShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.getJobById(id));
    }

    @Test
    void getAllJobsShouldReturnAllJobs() {
        Job job1 = new Job();
        job1.setId(UUID.randomUUID());
        Job job2 = new Job();
        job2.setId(UUID.randomUUID());
        when(jobRepository.findAll()).thenReturn(List.of(job1, job2));

        List<Job> result = jobService.getAllJobs();

        assertEquals(2, result.size());
    }

    @Test
    void deleteJobShouldDeleteWhenFound() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        jobService.deleteJob(id);

        verify(jobRepository).delete(job);
    }

    @Test
    void deleteJobShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.deleteJob(id));
    }

    @Test
    void updateJobShouldSaveJob() {
        Job job = new Job();
        job.setId(UUID.randomUUID());

        jobService.updateJob(job);

        verify(jobRepository).save(job);
    }

    @Test
    void updateJobStatusShouldSetNewStatus() {
        UUID jobId = UUID.randomUUID();
        Job job = new Job();
        job.setId(jobId);
        job.setStatus(JobStatus.PENDING);
        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        jobService.updateJobStatus(jobId, JobStatus.RUNNING);

        assertEquals(JobStatus.RUNNING, job.getStatus());
    }

    @Test
    void updateJobStatusShouldThrowWhenJobNotFound() {
        UUID jobId = UUID.randomUUID();
        when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () ->
                jobService.updateJobStatus(jobId, JobStatus.RUNNING));
    }

    @Test
    void createJobsBulkShouldCreateMultipleJobs() {
        JobRequestDto dto1 = createRequestDto();
        JobRequestDto dto2 = createRequestDto();
        dto2.setName("second-job");
        List<JobRequestDto> dtos = List.of(dto1, dto2);

        LocalDateTime nextFire = LocalDateTime.of(2026, 8, 10, 12, 0, 0);
        when(cronCalculator.getNextFireTime(any(String.class), any(LocalDateTime.class))).thenReturn(nextFire);
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        List<JobResponseDto> result = jobService.createJobsBulk(dtos);

        assertEquals(2, result.size());
        verify(jobRepository, times(2)).save(any(Job.class));
    }

    @Test
    void updateJobShouldUpdateNameWhenProvided() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setName("old-name");
        job.setCronExpression("0 * * * * *");
        job.setJobType("HTTP");
        job.setPayload("{\"url\":\"http://example.com\"}");
        job.setMaxRetries(3);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        JobUpdateDto updateDto = new JobUpdateDto();
        updateDto.setName("new-name");

        jobService.updateJob(id, updateDto);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals("new-name", captor.getValue().getName());
    }

    @Test
    void updateJobShouldUpdateCronExpressionAndRecalculateNextFireAt() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setCronExpression("0 * * * * *");
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        LocalDateTime newNextFire = LocalDateTime.of(2026, 8, 15, 10, 0, 0);
        when(cronCalculator.getNextFireTime(eq("0 0 * * * *"), any(LocalDateTime.class))).thenReturn(newNextFire);
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        JobUpdateDto updateDto = new JobUpdateDto();
        updateDto.setCronExpression("0 0 * * * *");

        jobService.updateJob(id, updateDto);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals("0 0 * * * *", captor.getValue().getCronExpression());
        assertEquals(newNextFire, captor.getValue().getNextFireAt());
    }

    @Test
    void updateJobShouldNotModifyFieldsWhenNull() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setName("original-name");
        job.setCronExpression("0 * * * * *");
        job.setJobType("HTTP");
        job.setPayload("{\"url\":\"http://example.com\"}");
        job.setMaxRetries(3);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        JobUpdateDto updateDto = new JobUpdateDto();

        jobService.updateJob(id, updateDto);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals("original-name", captor.getValue().getName());
        assertEquals("0 * * * * *", captor.getValue().getCronExpression());
        assertEquals("HTTP", captor.getValue().getJobType());
        assertEquals(3, captor.getValue().getMaxRetries());
    }

    @Test
    void updateJobShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        JobUpdateDto updateDto = new JobUpdateDto();
        updateDto.setName("new-name");

        assertThrows(JobNotFoundException.class, () -> jobService.updateJob(id, updateDto));
    }

    @Test
    void updateJobShouldUpdateUpdatedAtTimestamp() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setUpdatedAt(LocalDateTime.of(2026, 8, 1, 0, 0, 0));
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        JobUpdateDto updateDto = new JobUpdateDto();
        updateDto.setName("updated");

        jobService.updateJob(id, updateDto);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertTrue(captor.getValue().getUpdatedAt().isAfter(LocalDateTime.of(2026, 8, 1, 0, 0, 0)));
    }

    @Test
    void pauseJobShouldSetStatusToPaused() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        jobService.pauseJob(id);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(JobStatus.PAUSED, captor.getValue().getStatus());
    }

    @Test
    void pauseJobShouldSkipIfAlreadyPaused() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PAUSED);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        jobService.pauseJob(id);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void pauseJobShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.pauseJob(id));
    }

    @Test
    void resumeJobShouldSetStatusToPendingAndRecalculateNextFire() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PAUSED);
        job.setCronExpression("0 * * * * *");
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        LocalDateTime nextFire = LocalDateTime.of(2026, 8, 16, 10, 0, 0);
        when(cronCalculator.getNextFireTime(eq("0 * * * * *"), any(LocalDateTime.class))).thenReturn(nextFire);
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        jobService.resumeJob(id);

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        assertEquals(JobStatus.PENDING, captor.getValue().getStatus());
        assertEquals(nextFire, captor.getValue().getNextFireAt());
    }

    @Test
    void resumeJobShouldSkipIfNotPaused() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        job.setStatus(JobStatus.PENDING);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));
        when(jobMapper.toJobResponse(any(Job.class))).thenReturn(new JobResponseDto());

        jobService.resumeJob(id);

        verify(jobRepository, never()).save(any());
    }

    @Test
    void resumeJobShouldThrowWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(jobRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(JobNotFoundException.class, () -> jobService.resumeJob(id));
    }

    @Test
    void createJobShouldThrowOnInvalidCronExpression() {
        JobRequestDto dto = createRequestDto();
        dto.setCronExpression("not-a-cron");

        assertThrows(InvalidJobRequestException.class, () -> jobService.createJob(dto));
    }

    @Test
    void updateJobShouldThrowOnInvalidCronExpression() {
        UUID id = UUID.randomUUID();
        Job job = new Job();
        job.setId(id);
        when(jobRepository.findById(id)).thenReturn(Optional.of(job));

        JobUpdateDto updateDto = new JobUpdateDto();
        updateDto.setCronExpression("invalid");

        assertThrows(InvalidJobRequestException.class, () -> jobService.updateJob(id, updateDto));
    }
}
