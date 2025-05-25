package com.privsense.api.dto;

import java.util.List;

/**
 * Generic DTO for paginated results.
 * @param <T> The type of elements in the page
 */
public class PagedResultDTO<T> {

    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    /**
     * Default constructor
     */
    public PagedResultDTO() {
    }

    /**
     * Constructor with all fields
     *
     * @param content list of items in the current page
     * @param pageNumber current page number (0-based)
     * @param pageSize size of the page
     * @param totalElements total number of elements across all pages
     * @param totalPages total number of pages
     * @param first whether this is the first page
     * @param last whether this is the last page
     */
    public PagedResultDTO(List<T> content, int pageNumber, int pageSize, 
                         long totalElements, int totalPages, boolean first, boolean last) {
        this.content = content;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    // Getters and setters
    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }
}