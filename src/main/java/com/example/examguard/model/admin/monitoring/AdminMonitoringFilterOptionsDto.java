package com.example.examguard.model.admin.monitoring;

import java.util.List;

public class AdminMonitoringFilterOptionsDto {

    private List<String> roles;
    private List<String> statuses;
    private List<String> actions;
    private List<String> modules;
    private List<String> severities;
    private List<String> violationTypes;

    private List<String> cameraStatuses;
    private List<String> cameraDeviceTypes;
    private List<String> cameraStreamRoles;

    public List<String> getRoles() { return roles; }
    public List<String> getStatuses() { return statuses; }
    public List<String> getActions() { return actions; }
    public List<String> getModules() { return modules; }
    public List<String> getSeverities() { return severities; }
    public List<String> getViolationTypes() { return violationTypes; }
    public List<String> getCameraStatuses() { return cameraStatuses; }
    public List<String> getCameraDeviceTypes() { return cameraDeviceTypes; }
    public List<String> getCameraStreamRoles() { return cameraStreamRoles; }
}