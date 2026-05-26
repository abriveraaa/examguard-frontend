package com.example.examguard.model.admin.monitoring;

import java.util.List;

public class AdminMonitoringLogsResponse {

    private List<AdminLogRowDto> content;
    private AdminMonitoringFilterOptionsDto filterOptions;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;

    public List<AdminLogRowDto> getContent() {
        return content;
    }

    public AdminMonitoringFilterOptionsDto getFilterOptions() {
        return filterOptions;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}