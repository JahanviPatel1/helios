package com.akshadip.helios.dtos;

import com.akshadip.helios.enums.JobStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class JobExecutionResponseDto {
    private String id;
    private String jobId;
    private JobStatus status;
    private Integer attempt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;
    private String errorMessage;
}
