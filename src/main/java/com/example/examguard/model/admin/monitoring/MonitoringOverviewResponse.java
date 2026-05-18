package com.example.examguard.model.admin.monitoring;

import java.util.List;

public class MonitoringOverviewResponse {

    private List<MetricCardDto> summaryCards;

    private List<ChartPointDto> concurrentUsersByRole;
    private List<ChartPointDto> activityVolume;
    private List<ChartPointDto> violationsByType;
    private List<ChartPointDto> violationsByProgram;
    private List<ChartPointDto> activeSessionsByRole;

    private List<AdminLogRowDto> recentCriticalEvents;

    public List<MetricCardDto> getSummaryCards() { return summaryCards; }
    public List<ChartPointDto> getConcurrentUsersByRole() { return concurrentUsersByRole; }
    public List<ChartPointDto> getActivityVolume() { return activityVolume; }
    public List<ChartPointDto> getViolationsByType() { return violationsByType; }
    public List<ChartPointDto> getViolationsByProgram() { return violationsByProgram; }
    public List<ChartPointDto> getActiveSessionsByRole() { return activeSessionsByRole; }
    public List<AdminLogRowDto> getRecentCriticalEvents() { return recentCriticalEvents; }
}