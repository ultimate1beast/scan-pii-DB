package com.privsense.core.service.impl;

import com.privsense.core.config.PrivSenseConfigProperties;
import com.privsense.core.model.DetectionRule;
import com.privsense.core.repository.DetectionRuleRepository;
import com.privsense.core.service.ConfigurationService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the ConfigurationService interface.
 * Manages application configuration and detection rules.
 */
@Service
@Primary
@Slf4j
public class ConfigurationServiceImpl implements ConfigurationService {

    private final PrivSenseConfigProperties configProperties;
    private final DetectionRuleRepository ruleRepository;

    /**
     * Constructor for ConfigurationServiceImpl.
     * 
     * @param configProperties The application configuration properties
     * @param ruleRepository The detection rule repository
     */
    @Autowired
    public ConfigurationServiceImpl(PrivSenseConfigProperties configProperties, DetectionRuleRepository ruleRepository) {
        this.configProperties = configProperties;
        this.ruleRepository = ruleRepository;
    }

    @Override
    public Map<String, Object> getConfiguration() {
        Map<String, Object> config = new HashMap<>();
        
        // Detection configuration
        Map<String, Object> detection = new HashMap<>();
        detection.put("heuristicThreshold", configProperties.getDetection().getHeuristicThreshold());
        detection.put("regexThreshold", configProperties.getDetection().getRegexThreshold());
        detection.put("nerThreshold", configProperties.getDetection().getNerThreshold());
        detection.put("reportingThreshold", configProperties.getDetection().getReportingThreshold());
        detection.put("stopPipelineOnHighConfidence", configProperties.getDetection().isStopPipelineOnHighConfidence());
        detection.put("entropyEnabled", configProperties.getDetection().isEntropyEnabled());
        config.put("detection", detection);
        
        // Sampling configuration
        Map<String, Object> sampling = new HashMap<>();
        sampling.put("defaultSize", configProperties.getSampling().getDefaultSize());
        sampling.put("maxConcurrentDbQueries", configProperties.getSampling().getMaxConcurrentDbQueries());
        sampling.put("entropyCalculationEnabled", configProperties.getSampling().isEntropyCalculationEnabled());
        sampling.put("defaultMethod", configProperties.getSampling().getDefaultMethod());
        config.put("sampling", sampling);
        
        // NER configuration
        Map<String, Object> ner = new HashMap<>();
        ner.put("enabled", configProperties.getNer().isEnabled());
        ner.put("serviceUrl", configProperties.getNer().getService().getUrl());
        ner.put("timeoutSeconds", configProperties.getNer().getService().getTimeoutSeconds());
        ner.put("maxSamples", configProperties.getNer().getService().getMaxSamples());
        ner.put("retryAttempts", configProperties.getNer().getService().getRetryAttempts());
        
        // NER circuit breaker configuration
        Map<String, Object> circuitBreaker = new HashMap<>();
        circuitBreaker.put("enabled", configProperties.getNer().getService().getCircuitBreaker().isEnabled());
        circuitBreaker.put("failureThreshold", configProperties.getNer().getService().getCircuitBreaker().getFailureThreshold());
        circuitBreaker.put("resetTimeoutSeconds", configProperties.getNer().getService().getCircuitBreaker().getResetTimeoutSeconds());
        ner.put("circuitBreaker", circuitBreaker);
        
        config.put("ner", ner);
        
        // Reporting configuration
        Map<String, Object> reporting = new HashMap<>();
        reporting.put("pdfEnabled", configProperties.getReporting().isPdfEnabled());
        reporting.put("csvEnabled", configProperties.getReporting().isCsvEnabled());
        reporting.put("textEnabled", configProperties.getReporting().isTextEnabled());
        reporting.put("reportOutputPath", configProperties.getReporting().getReportOutputPath());
        config.put("reporting", reporting);
        
        // Database configuration
        Map<String, Object> database = new HashMap<>();
        database.put("driverDir", configProperties.getDb().getJdbc().getDriverDir());
        
        // Connection pool configuration
        Map<String, Object> pool = new HashMap<>();
        pool.put("connectionTimeout", configProperties.getDb().getPool().getConnectionTimeout());
        pool.put("idleTimeout", configProperties.getDb().getPool().getIdleTimeout());
        pool.put("maxLifetime", configProperties.getDb().getPool().getMaxLifetime());
        pool.put("minimumIdle", configProperties.getDb().getPool().getMinimumIdle());
        pool.put("maximumPoolSize", configProperties.getDb().getPool().getMaximumPoolSize());
        database.put("pool", pool);
        
        config.put("database", database);
        
        return config;
    }

    @Override
    @Transactional
    public Map<String, Object> updateConfiguration(Map<String, Object> configMap) {
        log.info("Updating application configuration");
        
        // Update detection configuration
        if (configMap.containsKey("detection")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> detection = (Map<String, Object>) configMap.get("detection");
            updateDetectionConfig(detection);
        }
        
        // Update sampling configuration
        if (configMap.containsKey("sampling")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> sampling = (Map<String, Object>) configMap.get("sampling");
            updateSamplingConfig(sampling);
        }
        
        // Update NER configuration
        if (configMap.containsKey("ner")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ner = (Map<String, Object>) configMap.get("ner");
            updateNerConfig(ner);
        }
        
        // Update reporting configuration
        if (configMap.containsKey("reporting")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reporting = (Map<String, Object>) configMap.get("reporting");
            updateReportingConfig(reporting);
        }
        
        // Update database configuration
        if (configMap.containsKey("database")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> database = (Map<String, Object>) configMap.get("database");
            updateDatabaseConfig(database);
        }
        
        log.debug("Configuration updated successfully");
        return getConfiguration();
    }

    private void updateDetectionConfig(Map<String, Object> detection) {
        if (detection.containsKey("heuristicThreshold")) {
            configProperties.getDetection().setHeuristicThreshold(getDouble(detection, "heuristicThreshold"));
        }
        if (detection.containsKey("regexThreshold")) {
            configProperties.getDetection().setRegexThreshold(getDouble(detection, "regexThreshold"));
        }
        if (detection.containsKey("nerThreshold")) {
            configProperties.getDetection().setNerThreshold(getDouble(detection, "nerThreshold"));
        }
        if (detection.containsKey("reportingThreshold")) {
            configProperties.getDetection().setReportingThreshold(getDouble(detection, "reportingThreshold"));
        }
        if (detection.containsKey("stopPipelineOnHighConfidence")) {
            configProperties.getDetection().setStopPipelineOnHighConfidence(getBoolean(detection, "stopPipelineOnHighConfidence"));
        }
        if (detection.containsKey("entropyEnabled")) {
            configProperties.getDetection().setEntropyEnabled(getBoolean(detection, "entropyEnabled"));
        }
    }

    private void updateSamplingConfig(Map<String, Object> sampling) {
        if (sampling.containsKey("defaultSize")) {
            configProperties.getSampling().setDefaultSize(getInt(sampling, "defaultSize"));
        }
        if (sampling.containsKey("maxConcurrentDbQueries")) {
            configProperties.getSampling().setMaxConcurrentDbQueries(getInt(sampling, "maxConcurrentDbQueries"));
        }
        if (sampling.containsKey("entropyCalculationEnabled")) {
            configProperties.getSampling().setEntropyCalculationEnabled(getBoolean(sampling, "entropyCalculationEnabled"));
        }
        if (sampling.containsKey("defaultMethod")) {
            configProperties.getSampling().setDefaultMethod(getString(sampling, "defaultMethod"));
        }
    }

    private void updateNerConfig(Map<String, Object> ner) {
        if (ner.containsKey("enabled")) {
            configProperties.getNer().setEnabled(getBoolean(ner, "enabled"));
        }
        if (ner.containsKey("serviceUrl")) {
            configProperties.getNer().getService().setUrl(getString(ner, "serviceUrl"));
        }
        if (ner.containsKey("timeoutSeconds")) {
            configProperties.getNer().getService().setTimeoutSeconds(getInt(ner, "timeoutSeconds"));
        }
        if (ner.containsKey("maxSamples")) {
            configProperties.getNer().getService().setMaxSamples(getInt(ner, "maxSamples"));
        }
        if (ner.containsKey("retryAttempts")) {
            configProperties.getNer().getService().setRetryAttempts(getInt(ner, "retryAttempts"));
        }
        if (ner.containsKey("circuitBreaker")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> circuitBreaker = (Map<String, Object>) ner.get("circuitBreaker");
            if (circuitBreaker.containsKey("enabled")) {
                configProperties.getNer().getService().getCircuitBreaker().setEnabled(getBoolean(circuitBreaker, "enabled"));
            }
            if (circuitBreaker.containsKey("failureThreshold")) {
                configProperties.getNer().getService().getCircuitBreaker().setFailureThreshold(getInt(circuitBreaker, "failureThreshold"));
            }
            if (circuitBreaker.containsKey("resetTimeoutSeconds")) {
                configProperties.getNer().getService().getCircuitBreaker().setResetTimeoutSeconds(getInt(circuitBreaker, "resetTimeoutSeconds"));
            }
        }
    }

    private void updateReportingConfig(Map<String, Object> reporting) {
        if (reporting.containsKey("pdfEnabled")) {
            configProperties.getReporting().setPdfEnabled(getBoolean(reporting, "pdfEnabled"));
        }
        if (reporting.containsKey("csvEnabled")) {
            configProperties.getReporting().setCsvEnabled(getBoolean(reporting, "csvEnabled"));
        }
        if (reporting.containsKey("textEnabled")) {
            configProperties.getReporting().setTextEnabled(getBoolean(reporting, "textEnabled"));
        }
        if (reporting.containsKey("reportOutputPath")) {
            configProperties.getReporting().setReportOutputPath(getString(reporting, "reportOutputPath"));
        }
    }

    private void updateDatabaseConfig(Map<String, Object> database) {
        if (database.containsKey("driverDir")) {
            configProperties.getDb().getJdbc().setDriverDir(getString(database, "driverDir"));
        }
        if (database.containsKey("pool")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pool = (Map<String, Object>) database.get("pool");
            if (pool.containsKey("connectionTimeout")) {
                configProperties.getDb().getPool().setConnectionTimeout(getLong(pool, "connectionTimeout"));
            }
            if (pool.containsKey("idleTimeout")) {
                configProperties.getDb().getPool().setIdleTimeout(getLong(pool, "idleTimeout"));
            }
            if (pool.containsKey("maxLifetime")) {
                configProperties.getDb().getPool().setMaxLifetime(getLong(pool, "maxLifetime"));
            }
            if (pool.containsKey("minimumIdle")) {
                configProperties.getDb().getPool().setMinimumIdle(getInt(pool, "minimumIdle"));
            }
            if (pool.containsKey("maximumPoolSize")) {
                configProperties.getDb().getPool().setMaximumPoolSize(getInt(pool, "maximumPoolSize"));
            }
        }
    }

    @Override
    public void reloadConfiguration() {
        log.info("Reloading application configuration is not supported in this implementation");
    }

    @Override
    @Transactional
    public DetectionRule createDetectionRule(DetectionRule rule) {
        log.info("Creating new detection rule: {}", rule.getName());
        
        // Validate rule
        validateRule(rule);
        
        // Check if rule with same name already exists
        if (ruleRepository.existsByName(rule.getName())) {
            throw new IllegalArgumentException("Detection rule with name '" + rule.getName() + "' already exists");
        }
        
        // Set defaults and pre-persist
        rule.prePersist();
        
        return ruleRepository.save(rule);
    }

    @Override
    @Transactional
    public DetectionRule updateDetectionRule(String ruleId, DetectionRule rule) {
        log.info("Updating detection rule: {}", ruleId);
        
        // Validate rule
        validateRule(rule);
        
        // Check if rule exists
        DetectionRule existingRule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Detection rule not found: " + ruleId));
        
        // Check if renaming would cause a conflict
        if (!existingRule.getName().equals(rule.getName()) && ruleRepository.existsByName(rule.getName())) {
            throw new IllegalArgumentException("Detection rule with name '" + rule.getName() + "' already exists");
        }
        
        // Update fields
        existingRule.setName(rule.getName());
        existingRule.setPattern(rule.getPattern());
        existingRule.setPiiType(rule.getPiiType());
        existingRule.setConfidenceScore(rule.getConfidenceScore());
        existingRule.setDescription(rule.getDescription());
        existingRule.setEnabled(rule.isEnabled());
        existingRule.setRuleType(rule.getRuleType());
        existingRule.setUpdatedAt(System.currentTimeMillis());
        
        return ruleRepository.save(existingRule);
    }

    @Override
    @Transactional
    public boolean deleteDetectionRule(String ruleId) {
        log.info("Deleting detection rule: {}", ruleId);
        
        if (ruleRepository.existsById(ruleId)) {
            ruleRepository.deleteById(ruleId);
            return true;
        }
        
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetectionRule> getDetectionRule(String ruleId) {
        return ruleRepository.findById(ruleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetectionRule> getDetectionRuleByName(String name) {
        return ruleRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionRule> getAllDetectionRules() {
        return ruleRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionRule> getEnabledDetectionRules() {
        return ruleRepository.findByEnabledTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionRule> getDetectionRulesByPiiType(String piiType) {
        return ruleRepository.findByPiiType(piiType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionRule> getDetectionRulesByType(DetectionRule.RuleType ruleType) {
        return ruleRepository.findByRuleType(ruleType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DetectionRule> getEnabledDetectionRulesByType(DetectionRule.RuleType ruleType) {
        return ruleRepository.findByEnabledTrueAndRuleType(ruleType);
    }
    
    /**
     * Validate a detection rule.
     * 
     * @param rule The rule to validate
     * @throws IllegalArgumentException if the rule is invalid
     */
    private void validateRule(DetectionRule rule) {
        if (rule.getName() == null || rule.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule name is required");
        }
        
        if (rule.getPattern() == null || rule.getPattern().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule pattern is required");
        }
        
        if (rule.getPiiType() == null || rule.getPiiType().trim().isEmpty()) {
            throw new IllegalArgumentException("PII type is required");
        }
        
        if (rule.getConfidenceScore() < 0 || rule.getConfidenceScore() > 1) {
            throw new IllegalArgumentException("Confidence score must be between 0 and 1");
        }
        
        if (rule.getRuleType() == null) {
            throw new IllegalArgumentException("Rule type is required");
        }
    }
    
    private boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }
    
    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        return 0;
    }
    
    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return 0L;
    }
    
    private double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            return Double.parseDouble((String) value);
        }
        return 0.0;
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}