package com.akshadip.helios.dtos;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobUpdateDto {
    private String name;

    private String cronExpression;

    private String jobType;

    private String payload;

    @Min(value = 0, message = "Max retries must be non-negative")
    private Integer maxRetries;
}
