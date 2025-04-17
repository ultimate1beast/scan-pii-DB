package com.privsense.pii.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the PII detection pipeline.
 */
@Configuration
@ConfigurationProperties(prefix = "privsense.detection")
public class DetectionConfig {

    private double heuristicThreshold = 0.7;
    private double regexThreshold = 0.8;
    private double nerThreshold = 0.6;
    private double minimumReportingThreshold = 0.5;
    private boolean stopPipelineOnHighConfidence = true;

    public double getHeuristicThreshold() {
        return heuristicThreshold;
    }

    public void setHeuristicThreshold(double heuristicThreshold) {
        this.heuristicThreshold = heuristicThreshold;
    }

    public double getRegexThreshold() {
        return regexThreshold;
    }

    public void setRegexThreshold(double regexThreshold) {
        this.regexThreshold = regexThreshold;
    }

    public double getNerThreshold() {
        return nerThreshold;
    }

    public void setNerThreshold(double nerThreshold) {
        this.nerThreshold = nerThreshold;
    }

    public double getMinimumReportingThreshold() {
        return minimumReportingThreshold;
    }

    public void setMinimumReportingThreshold(double minimumReportingThreshold) {
        this.minimumReportingThreshold = minimumReportingThreshold;
    }

    public boolean isStopPipelineOnHighConfidence() {
        return stopPipelineOnHighConfidence;
    }

    public void setStopPipelineOnHighConfidence(boolean stopPipelineOnHighConfidence) {
        this.stopPipelineOnHighConfidence = stopPipelineOnHighConfidence;
    }
}