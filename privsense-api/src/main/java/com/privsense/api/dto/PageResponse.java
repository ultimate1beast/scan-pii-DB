package com.privsense.api.dto;

import com.privsense.api.dto.base.BaseResponseDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic response DTO for paginated data.
 * Provides a standardized format for paginated responses across the API.
 *
 * @param <T> The type of elements in the page
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> extends BaseResponseDTO {
    
    /**
     * The content items for the current page.
     */
    @Builder.Default
    private List<T> content = new ArrayList<>();
    
    /**
     * The current page number (0-based).
     */
    private int pageNumber;
    
    /**
     * The number of items per page.
     */
    private int pageSize;
    
    /**
     * The total number of elements across all pages.
     */
    private long totalElements;
    
    /**
     * The total number of pages.
     */
    private int totalPages;
    
    /**
     * Whether this is the first page.
     */
    private boolean first;
    
    /**
     * Whether this is the last page.
     */
    private boolean last;
    
    /**
     * Whether there is a next page.
     */
    private boolean hasNext;
    
    /**
     * Whether there is a previous page.
     */
    private boolean hasPrevious;
}