package com.privsense.core.service;

import com.privsense.core.model.DetectionRule;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing application configuration and detection rules.
 */
public interface ConfigurationService {

    /**
     * Gets the complete application configuration.
     *
     * @return Map of configuration properties
     */
    Map<String, Object> getConfiguration();

    /**
     * Updates the application configuration.
     *
     * @param configMap Map of configuration values to update
     * @return The updated configuration as a Map
     */
    Map<String, Object> updateConfiguration(Map<String, Object> configMap);

    /**
     * Reloads the application configuration from the configuration source.
     */
    void reloadConfiguration();

    /**
     * Creates a new detection rule.
     *
     * @param rule The detection rule to create
     * @return The created detection rule
     * @throws IllegalArgumentException if the rule is invalid or a rule with the name already exists
     */
    DetectionRule createDetectionRule(DetectionRule rule);

    /**
     * Updates an existing detection rule.
     *
     * @param ruleId The ID of the rule to update
     * @param rule The updated rule data
     * @return The updated detection rule
     * @throws IllegalArgumentException if the rule is invalid or not found
     */
    DetectionRule updateDetectionRule(String ruleId, DetectionRule rule);

    /**
     * Deletes a detection rule.
     *
     * @param ruleId The ID of the rule to delete
     * @return true if the rule was deleted, false if it wasn't found
     */
    boolean deleteDetectionRule(String ruleId);

    /**
     * Gets a detection rule by ID.
     *
     * @param ruleId The ID of the rule to retrieve
     * @return Optional containing the rule if found
     */
    Optional<DetectionRule> getDetectionRule(String ruleId);

    /**
     * Gets a detection rule by name.
     *
     * @param name The name of the rule to retrieve
     * @return Optional containing the rule if found
     */
    Optional<DetectionRule> getDetectionRuleByName(String name);

    /**
     * Gets all detection rules.
     *
     * @return List of all detection rules
     */
    List<DetectionRule> getAllDetectionRules();

    /**
     * Gets all enabled detection rules.
     *
     * @return List of all enabled detection rules
     */
    List<DetectionRule> getEnabledDetectionRules();

    /**
     * Gets all detection rules for a specific PII type.
     *
     * @param piiType The PII type
     * @return List of detection rules for the PII type
     */
    List<DetectionRule> getDetectionRulesByPiiType(String piiType);

    /**
     * Gets all detection rules of a specific type.
     *
     * @param ruleType The rule type
     * @return List of detection rules of the specified type
     */
    List<DetectionRule> getDetectionRulesByType(DetectionRule.RuleType ruleType);

    /**
     * Gets all enabled detection rules of a specific type.
     *
     * @param ruleType The rule type
     * @return List of enabled detection rules of the specified type
     */
    List<DetectionRule> getEnabledDetectionRulesByType(DetectionRule.RuleType ruleType);
}