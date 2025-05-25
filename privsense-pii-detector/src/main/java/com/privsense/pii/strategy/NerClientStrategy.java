package com.privsense.pii.strategy;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.privsense.core.model.ColumnInfo;
import com.privsense.core.model.PiiCandidate;
import com.privsense.core.model.SampleData;
import com.privsense.core.service.PiiDetectionStrategy;
import com.privsense.core.config.PrivSenseConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategy that calls an external NER service to detect PII entities in text data.
 * Uses Spring WebClient for non-blocking HTTP calls.
 */
@Component
public class NerClientStrategy implements PiiDetectionStrategy {

    private static final Logger logger = LoggerFactory.getLogger(NerClientStrategy.class);
    private static final String STRATEGY_NAME = "NER";
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final PrivSenseConfigProperties configProperties;
    
    // Configuration properties from PrivSenseConfigProperties
    private String nerServiceUrl;
    private int requestTimeoutMs;
    private int maxSamples;
    private int retryAttempts;
    private boolean nerServiceEnabled;
    
    private boolean serviceAvailable = true;
    
    /**
     * Constructor with dependency injection
     */
    @Autowired
    public NerClientStrategy(WebClient.Builder webClientBuilder, ObjectMapper objectMapper, PrivSenseConfigProperties configProperties) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
        this.configProperties = configProperties;
    }
    
    @PostConstruct
    public void init() {
        // Extract configuration from the centralized PrivSenseConfigProperties
        this.nerServiceUrl = configProperties.getNer().getService().getUrl();
        this.requestTimeoutMs = configProperties.getNer().getService().getTimeoutSeconds() * 1000;
        this.maxSamples = configProperties.getNer().getService().getMaxSamples();
        this.retryAttempts = configProperties.getNer().getService().getRetryAttempts();
        this.nerServiceEnabled = configProperties.getNer().getService().getCircuitBreaker().isEnabled();
        
        logger.info("Initialized NerClientStrategy with service URL: {}, enabled: {}", nerServiceUrl, nerServiceEnabled);
        
        // Test connection to NER service on startup
        if (nerServiceEnabled) {
            try {
                webClient.get()
                    .uri(nerServiceUrl + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .block();
                logger.info("Successfully connected to NER service at {}", nerServiceUrl);
            } catch (Exception e) {
                serviceAvailable = false;
                logger.warn("Failed to connect to NER service at {}: {}. Will continue without NER detection.", 
                    nerServiceUrl, e.getMessage());
            }
        } else {
            serviceAvailable = false;
            logger.info("NER service is disabled by configuration");
        }
    }

    @Override
    public List<PiiCandidate> detect(ColumnInfo columnInfo, SampleData sampleData) {
        if (columnInfo == null || sampleData == null || sampleData.getSamples() == null) {
            logger.warn("Column info or sample data is null, skipping NER detection");
            return Collections.emptyList();
        }
        
        // Skip NER detection if service is disabled or unavailable
        if (!nerServiceEnabled || !serviceAvailable) {
            logger.debug("Skipping NER detection for column {} - service disabled or unavailable", 
                columnInfo.getColumnName());
            return Collections.emptyList();
        }
        
        // Convert samples to strings
        List<String> stringSamples = sampleData.getSamples().stream()
                .filter(s -> s != null)
                .map(Object::toString)
                .collect(Collectors.toList());
        
        if (stringSamples.isEmpty()) {
            logger.debug("No string samples to analyze for column: {}", columnInfo.getColumnName());
            return Collections.emptyList();
        }
        
        // Limit the number of samples sent to the NER service
        if (stringSamples.size() > maxSamples) {
            logger.debug("Limiting samples from {} to {} for NER analysis", 
                    stringSamples.size(), maxSamples);
            stringSamples = stringSamples.subList(0, maxSamples);
        }
        
        try {
            // Create the request object
            NerRequest request = new NerRequest(stringSamples);
            
            // Call the NER service
            NerResponse response = callNerService(request);
            
            if (response == null || response.getResults() == null) {
                logger.warn("Received null response from NER service for column: {}", columnInfo.getColumnName());
                return Collections.emptyList();
            }
            
            return processNerResponse(response, columnInfo, stringSamples.size());
            
        } catch (Exception e) {
            serviceAvailable = false; // Mark service as unavailable after failure
            logger.error("Error calling NER service for column {}: {}", 
                    columnInfo.getColumnName(), e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Calls the NER service with the given request
     */
    private NerResponse callNerService(NerRequest request) throws JsonProcessingException {
        logger.debug("Calling NER service with {} samples", request.getSamples().size());
        
        try {
            return webClient.post()
                .uri(nerServiceUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .retrieve()
                .bodyToMono(NerResponse.class)
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .retryWhen(Retry.fixedDelay(retryAttempts, Duration.ofMillis(1000))
                    .filter(e -> !(e instanceof WebClientResponseException.NotFound)) // Don't retry 404s
                )
                .onErrorResume(e -> {
                    logger.error("Error from NER service: {}", e.getMessage());
                    return Mono.empty();
                })
                .block();
        } catch (Exception e) {
            logger.error("Exception calling NER service: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * Processes the NER service response and converts it to PII candidates
     */
    private List<PiiCandidate> processNerResponse(NerResponse response, ColumnInfo columnInfo, int totalSamples) {
        List<PiiCandidate> candidates = new ArrayList<>();
        
        // Group entities by type and count occurrences
        Map<String, List<NerEntity>> entitiesByType = response.getResults().stream()
                .flatMap(List::stream)
                .collect(Collectors.groupingBy(NerEntity::getType));
        
        for (Map.Entry<String, List<NerEntity>> entry : entitiesByType.entrySet()) {
            String entityType = entry.getKey();
            List<NerEntity> entities = entry.getValue();
            
            // Calculate average confidence score
            double avgScore = entities.stream()
                    .mapToDouble(NerEntity::getScore)
                    .average()
                    .orElse(0.0);
            
            // Adjust score based on percentage of samples with this entity type
            int samplesWithEntityType = (int) response.getResults().stream()
                    .filter(sampleEntities -> sampleEntities.stream()
                            .anyMatch(entity -> entity.getType().equals(entityType)))
                    .count();
            
            double matchPercentage = (double) samplesWithEntityType / totalSamples;
            double finalScore = avgScore * matchPercentage;
            
            // Map NER entity type to PII type
            String piiType = mapEntityTypeToPiiType(entityType);
            
            // Create evidence string with examples (masked for privacy)
            String examples = entities.stream()
                    .limit(3)
                    .map(entity -> maskText(entity.getText()))
                    .collect(Collectors.joining(", "));
            
            String evidence = String.format("NER detected %d entities of type '%s' in %d of %d samples (%.1f%%). Examples: %s", 
                    entities.size(), entityType, samplesWithEntityType, totalSamples, matchPercentage * 100, examples);
            
            // Add candidate if score is meaningful
            if (finalScore > 0.2) {
                candidates.add(new PiiCandidate(
                        columnInfo,
                        piiType,
                        finalScore,
                        STRATEGY_NAME,
                        evidence
                ));
                
                logger.debug("Added PII candidate from NER: {} (score={})", piiType, finalScore);
            }
        }
        
        return candidates;
    }
    
    /**
     * Maps NER entity types to standard PII types
     */
    private String mapEntityTypeToPiiType(String entityType) {
        switch (entityType.toUpperCase()) {
            case "PER":
            case "PERSON":
                return "PERSON_NAME";
            case "LOC":
            case "LOCATION":
            case "ADDRESS":
                return "ADDRESS";
            case "ORG":
            case "ORGANIZATION":
                return "ORGANIZATION";
            case "PHONE":
            case "PHONE_NUMBER":
            case "MOBILE_PHONE_NUMBER":
            case "LANDLINE_PHONE_NUMBER":
                return "PHONE_NUMBER";
            case "EMAIL":
            case "EMAIL_ADDRESS":
                return "EMAIL";
            case "SSN":
            case "SOCIAL_SECURITY_NUMBER":
                return "SSN";
            case "CREDIT_CARD":
            case "CREDIT_CARD_NUMBER":
                return "CREDIT_CARD_NUMBER";
            case "CREDIT_CARD_EXPIRATION_DATE":
                return "CREDIT_CARD_EXPIRATION_DATE";
            case "CREDIT_CARD_BRAND":
                return "CREDIT_CARD_BRAND";
            case "CVV":
            case "CVC":
                return "CARD_SECURITY_CODE";
            case "PASSPORT":
            case "PASSPORT_NUMBER":
                return "PASSPORT_NUMBER";
            case "PASSPORT_EXPIRATION_DATE":
                return "PASSPORT_EXPIRATION_DATE";
            case "DRIVERS_LICENSE":
            case "DRIVERS_LICENSE_NUMBER":
            case "DRIVER'S_LICENSE_NUMBER":
                return "DRIVERS_LICENSE_NUMBER";
            case "IDENTITY_CARD_NUMBER":
            case "ID_CARD":
                return "IDENTITY_CARD_NUMBER";
            case "NATIONAL_ID":
            case "NATIONAL_ID_NUMBER":
                return "NATIONAL_ID_NUMBER";
            case "IDENTITY_DOCUMENT_NUMBER":
                return "IDENTITY_DOCUMENT_NUMBER";
            case "TAX_IDENTIFICATION_NUMBER":
                return "TAX_ID_NUMBER";
            case "CPF":
                return "CPF_NUMBER";
            case "CNPJ":
                return "CNPJ_NUMBER";
            case "BANK_ACCOUNT":
            case "BANK_ACCOUNT_NUMBER":
                return "BANK_ACCOUNT_NUMBER";
            case "IBAN":
                return "IBAN";
            case "DATE_OF_BIRTH":
                return "DATE_OF_BIRTH";
            case "HEALTH_INSURANCE_ID_NUMBER":
            case "HEALTH_INSURANCE_NUMBER":
            case "INSURANCE_NUMBER":
            case "NATIONAL_HEALTH_INSURANCE_NUMBER":
                return "HEALTH_INSURANCE_NUMBER";
            case "INSURANCE_COMPANY":
                return "INSURANCE_COMPANY";
            case "IP_ADDRESS":
                return "IP_ADDRESS";
            case "USERNAME":
                return "USERNAME";
            case "DIGITAL_SIGNATURE":
                return "DIGITAL_SIGNATURE";
            case "SOCIAL_MEDIA_HANDLE":
                return "SOCIAL_MEDIA_HANDLE";
            case "MEDICATION":
                return "MEDICATION";
            case "MEDICAL_CONDITION":
                return "MEDICAL_CONDITION";
            case "BLOOD_TYPE":
                return "BLOOD_TYPE";
            case "REGISTRATION_NUMBER":
                return "REGISTRATION_NUMBER";
            case "STUDENT_ID_NUMBER":
                return "STUDENT_ID_NUMBER";
            case "FLIGHT_NUMBER":
                return "FLIGHT_NUMBER";
            case "RESERVATION_NUMBER":
                return "RESERVATION_NUMBER";
            case "TRANSACTION_NUMBER":
                return "TRANSACTION_NUMBER";
            case "LICENSE_PLATE_NUMBER":
                return "LICENSE_PLATE_NUMBER";
            case "VEHICLE_REGISTRATION_NUMBER":
                return "VEHICLE_REGISTRATION_NUMBER";
            case "POSTAL_CODE":
                return "POSTAL_CODE";
            case "FAX_NUMBER":
                return "FAX_NUMBER";
            case "VISA_NUMBER":
                return "VISA_NUMBER";
            case "SERIAL_NUMBER":
                return "SERIAL_NUMBER";
            case "BIRTH_CERTIFICATE_NUMBER":
                return "BIRTH_CERTIFICATE_NUMBER";
            case "TRAIN_TICKET_NUMBER":
                return "TRAIN_TICKET_NUMBER";
            default:
                return "GENERIC_PII";
        }
    }
    
    /**
     * Masks text for privacy when logging
     */
    private String maskText(String text) {
        if (text == null || text.length() <= 4) {
            return "****";
        }
        
        int charsToShow = Math.max(2, text.length() / 4);
        return text.substring(0, charsToShow) + 
               "*".repeat(text.length() - (2 * charsToShow)) + 
               text.substring(text.length() - charsToShow);
    }

    @Override
    public String getStrategyName() {
        return STRATEGY_NAME;
    }
    
    /**
     * Request class for NER service
     */
    private static class NerRequest {
        private final List<String> samples;
        
        public NerRequest(List<String> samples) {
            this.samples = samples;
        }
        
        public List<String> getSamples() {
            return samples;
        }
    }
    
    /**
     * Response class from NER service
     */
    private static class NerResponse {
        @JsonProperty("results")
        private List<List<NerEntity>> results;
        
        public List<List<NerEntity>> getResults() {
            return results;
        }
        
        public void setResults(List<List<NerEntity>> results) {
            this.results = results;
        }
    }
    
    /**
     * Entity class from NER service
     */
    private static class NerEntity {
        private String text;
        private String type;
        private double score;
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public double getScore() {
            return score;
        }
        
        public void setScore(double score) {
            this.score = score;
        }
    }
}