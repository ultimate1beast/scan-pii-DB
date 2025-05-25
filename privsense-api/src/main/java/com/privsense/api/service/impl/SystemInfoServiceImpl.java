package com.privsense.api.service.impl;

import com.privsense.api.dto.SystemHealthResponse;
import com.privsense.api.dto.SystemInfoResponse;
import com.privsense.api.service.SystemInfoService;
import com.privsense.core.config.PrivSenseConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Service implementation for retrieving system information and health status.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SystemInfoServiceImpl implements SystemInfoService {

    private final DataSource dataSource;
    private final PrivSenseConfigProperties configProperties;
    private final Optional<BuildProperties> buildProperties;
    private final RestTemplate restTemplate;
    
    @Value("${spring.profiles.active:development}")
    private String activeProfile;
    
    @Value("${ner.service.url:http://localhost:5000}")
    private String nerServiceUrl;
    
    private final Instant startTime = Instant.now();

    @Override
    public SystemHealthResponse getHealthStatus() {
        SystemHealthResponse response = new SystemHealthResponse();
        
        // Check API health (always UP if we can execute this code)
        response.addComponent("api", "UP");
        
        // Check database health
        String dbStatus = checkDatabaseHealth();
        response.addComponent("database", dbStatus);
        
        // Check NER service health
        String nerStatus = checkNerServiceHealth();
        response.addComponent("ner-service", nerStatus);
        
        // Determine overall health status
        if ("DOWN".equals(dbStatus) || "DOWN".equals(nerStatus)) {
            response.setStatus("DOWN");
            response.setDetails("One or more critical components are down.");
        } else if ("DEGRADED".equals(dbStatus) || "DEGRADED".equals(nerStatus)) {
            response.setStatus("DEGRADED");
            response.setDetails("System is operating with degraded functionality.");
        } else {
            response.setStatus("UP");
            response.setDetails("All components are healthy.");
        }
        
        return response;
    }
    
    @Override
    public SystemInfoResponse getSystemInfo() {
        SystemInfoResponse response = new SystemInfoResponse();
        
        // Get version from build properties if available
        buildProperties.ifPresent(build -> {
            response.setVersion(build.getVersion());
            response.setBuildTime(build.getTime().toString());
        });
        
        // If build properties not available, use default values
        if (response.getVersion() == null) {
            response.setVersion("1.0.0-SNAPSHOT");
            response.setBuildTime(Instant.now().toString());
        }
        
        // Calculate uptime
        Duration uptime = Duration.between(startTime, Instant.now());
        response.setUptime(formatDuration(uptime));
        
        // Get environment
        response.setEnvironment(activeProfile);
        
        // Set available features
        response.getFeatures().put("nerModel", configProperties.getNer().isEnabled());
        response.getFeatures().put("pdfExport", configProperties.getReporting().isPdfEnabled());
        response.getFeatures().put("entropyAnalysis", configProperties.getDetection().isEntropyEnabled());
        
        // Add additional info
        response.getAdditionalInfo().put("javaVersion", System.getProperty("java.version"));
        response.getAdditionalInfo().put("osName", System.getProperty("os.name"));
        
        return response;
    }
    
    /**
     * Checks if the database connection is healthy.
     *
     * @return Status string: UP, DOWN, or DEGRADED
     */
    private String checkDatabaseHealth() {
        try {
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            Integer result = jdbc.queryForObject("SELECT 1", Integer.class);
            
            if (result != null && result == 1) {
                return "UP";
            } else {
                log.warn("Database health check failed: unexpected result");
                return "DEGRADED";
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return "DOWN";
        }
    }
    
    /**
     * Checks if the NER service is responsive.
     *
     * @return Status string: UP, DOWN, or DEGRADED
     */
    private String checkNerServiceHealth() {
        try {
            // Try to reach the NER service health endpoint
            String healthEndpoint = nerServiceUrl + "/detect-pii/health";
            restTemplate.getForObject(healthEndpoint, String.class);
            return "UP";
        } catch (Exception e) {
            log.warn("NER service health check failed: {}", e.getMessage());
            
            // If NER service is disabled in config, it's not critical
            if (!configProperties.getNer().isEnabled()) {
                return "DEGRADED";
            }
            return "DOWN";
        }
    }
    
    /**
     * Formats a duration into a human-readable string.
     *
     * @param duration The duration to format
     * @return A human-readable representation of the duration
     */
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes, seconds);
    }
}