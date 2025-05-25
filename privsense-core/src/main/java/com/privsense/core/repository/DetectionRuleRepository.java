package com.privsense.core.repository;

import com.privsense.core.model.DetectionRule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing detection rules.
 */
@Repository
public interface DetectionRuleRepository extends JpaRepository<DetectionRule, String> {

    /**
     * Find a rule by its name.
     * 
     * @param name The rule name
     * @return Optional containing the rule, if found
     */
    Optional<DetectionRule> findByName(String name);
    
    /**
     * Find all enabled rules.
     * 
     * @return List of enabled rules
     */
    List<DetectionRule> findByEnabledTrue();
    
    /**
     * Find all rules for a specific PII type.
     * 
     * @param piiType The PII type
     * @return List of rules for the specified PII type
     */
    List<DetectionRule> findByPiiType(String piiType);
    
    /**
     * Find all rules of a specific type.
     * 
     * @param ruleType The rule type
     * @return List of rules of the specified type
     */
    List<DetectionRule> findByRuleType(DetectionRule.RuleType ruleType);
    
    /**
     * Find all enabled rules of a specific type.
     * 
     * @param ruleType The rule type
     * @return List of enabled rules of the specified type
     */
    List<DetectionRule> findByEnabledTrueAndRuleType(DetectionRule.RuleType ruleType);
    
    /**
     * Check if a rule with the given name exists.
     * 
     * @param name The rule name
     * @return True if a rule with the name exists, false otherwise
     */
    boolean existsByName(String name);
}