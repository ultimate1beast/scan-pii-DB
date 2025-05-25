package com.privsense.api.dto.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.Builder;
import org.springframework.hateoas.Link;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all REST API response DTOs.
 * Provides standardized metadata structure for all API responses.
 * Enhanced with HATEOAS support for hypermedia links.
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseResponseDTO {

    @Builder.Default
    private Map<String, Object> meta = new HashMap<>();
    
    /**
     * HATEOAS links for resource navigation.
     * Only included in the JSON response when not empty.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Builder.Default
    private List<Link> links = new ArrayList<>();
    
    /**
     * Initialize metadata with default values.
     * This method is called by constructors to ensure metadata is properly initialized.
     */
    protected void initMeta() {
        if (meta == null) {
            meta = new HashMap<>();
        }
        if (!meta.containsKey("timestamp")) {
            meta.put("timestamp", LocalDateTime.now());
        }
    }
    
    /**
     * Default constructor with initialized metadata.
     * This constructor is called by Lombok's no-args constructor.
     */
    {
        initMeta();
    }
    
    /**
     * Add metadata to the response.
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    public void addMeta(String key, Object value) {
        initMeta(); // Ensure meta is initialized
        meta.put(key, value);
    }
    
    /**
     * Add pagination metadata to the response.
     * 
     * @param page Current page number (0-based)
     * @param size Page size
     * @param totalElements Total number of elements
     * @param totalPages Total number of pages
     */
    public void addPaginationMeta(int page, int size, long totalElements, int totalPages) {
        initMeta(); // Ensure meta is initialized
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", page);
        pagination.put("size", size);
        pagination.put("totalElements", totalElements);
        pagination.put("totalPages", totalPages);
        
        meta.put("pagination", pagination);
    }
    
    /**
     * Add a link to this response.
     * 
     * @param rel Relation type (e.g., "self", "next")
     * @param href URI or URL of the link
     */
    public void addLink(String rel, String href) {
        if (links == null) {
            links = new ArrayList<>();
        }
        links.add(Link.of(href, rel));
    }
    
    /**
     * Add a templated link to this response.
     * 
     * @param rel Relation type
     * @param href URI template
     * @param templated Whether the link is templated
     */
    public void addLink(String rel, String href, boolean templated) {
        if (links == null) {
            links = new ArrayList<>();
        }
        
        Link link = Link.of(href, rel);
        if (templated) {
            link = link.withType("application/hal+json");
        }
        
        links.add(link);
    }
    
    /**
     * Determine if the response indicates a successful operation.
     * This method should be overridden by subclasses to provide specific success criteria.
     * 
     * @return true if the response indicates success, false otherwise
     */
    public boolean isSuccess() {
        initMeta(); // Ensure meta is initialized
        // Default implementation - subclasses should override with specific criteria
        return meta.containsKey("status") && "SUCCESS".equals(meta.get("status"));
    }
}