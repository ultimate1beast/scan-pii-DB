package com.privsense.api.util;

import com.privsense.api.dto.base.BaseResponseDTO;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Utility class for building HATEOAS links for responses.
 * Provides a centralized way to create standardized links for all API resources.
 */
public class LinkBuilder {

    private static final String BASE_PATH = "/api/v1";
    
    /**
     * Add standard links to a connection response.
     * 
     * @param response The response to enhance with links
     * @param connectionId The connection ID
     */
    public static void addConnectionLinks(BaseResponseDTO response, UUID connectionId) {
        String basePath = BASE_PATH + "/connections/" + connectionId;
        
        response.addLink("self", basePath);
        response.addLink("metadata", basePath + "/metadata");
        response.addLink("collection", BASE_PATH + "/connections");
        response.addLink("scans", BASE_PATH + "/scans?connectionId=" + connectionId);
    }
    
    /**
     * Add standard links to a scan response.
     * 
     * @param response The response to enhance with links
     * @param scanId The scan ID
     */
    public static void addScanLinks(BaseResponseDTO response, UUID scanId) {
        String basePath = BASE_PATH + "/scans/" + scanId;
        
        response.addLink("self", basePath);
        response.addLink("results", basePath + "/results");
        response.addLink("report", basePath + "/report");
        response.addLink("stats", basePath + "/stats");
        response.addLink("tables", basePath + "/tables");
        response.addLink("cancel", basePath);
        response.addLink("collection", BASE_PATH + "/scans");
    }
    
    /**
     * Add pagination links to a response.
     * 
     * @param response The response to enhance with links
     * @param page Current page number
     * @param size Page size
     * @param totalPages Total pages
     */
    public static void addPaginationLinks(BaseResponseDTO response, int page, int size, int totalPages) {
        String basePath = ServletUriComponentsBuilder.fromCurrentRequest()
                .replaceQueryParam("page", "{page}")
                .replaceQueryParam("size", "{size}")
                .build()
                .toUriString();
        
        // Self link (current page)
        response.addLink("self", 
                ServletUriComponentsBuilder.fromCurrentRequest().build().toUriString());
        
        // First page
        response.addLink("first", basePath.replace("{page}", "0").replace("{size}", String.valueOf(size)));
        
        // Previous page
        if (page > 0) {
            response.addLink("prev", basePath.replace("{page}", String.valueOf(page - 1))
                    .replace("{size}", String.valueOf(size)));
        }
        
        // Next page
        if (page < totalPages - 1) {
            response.addLink("next", basePath.replace("{page}", String.valueOf(page + 1))
                    .replace("{size}", String.valueOf(size)));
        }
        
        // Last page
        response.addLink("last", basePath.replace("{page}", String.valueOf(Math.max(0, totalPages - 1)))
                .replace("{size}", String.valueOf(size)));
    }
}