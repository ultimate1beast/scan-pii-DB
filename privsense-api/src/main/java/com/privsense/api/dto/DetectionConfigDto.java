package com.privsense.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for configuring the PII detection process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectionConfigDto {
    
    @Min(value = 0, message = "Heuristic threshold must be between 0.0 and 1.0")
    @Max(value = 1, message = "Heuristic threshold must be between 0.0 and 1.0")
    private Double heuristicThreshold;
    
    @Min(value = 0, message = "Regex threshold must be between 0.0 and 1.0")
    @Max(value = 1, message = "Regex threshold must be between 0.0 and 1.0")
    private Double regexThreshold;
    
    @Min(value = 0, message = "NER threshold must be between 0.0 and 1.0")
    @Max(value = 1, message = "NER threshold must be between 0.0 and 1.0")
    private Double nerThreshold;
    
    @Min(value = 0, message = "Reporting threshold must be between 0.0 and 1.0")
    @Max(value = 1, message = "Reporting threshold must be between 0.0 and 1.0")
    private Double reportingThreshold;
    
    private Boolean stopPipelineOnHighConfidence;
}