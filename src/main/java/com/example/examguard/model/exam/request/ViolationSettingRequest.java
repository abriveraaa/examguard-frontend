package com.example.examguard.model.exam.request;


public class ViolationSettingRequest {

    private String violationType;
    private Boolean enabled;
    private String severity;
    private Integer maxAllowedCount;


    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public void setEnabled(Boolean enabled){
        this.enabled = enabled;
    }

    public void setSeverity(String severity){
        this.severity = severity;
    }

    public void setMaxAllowedCount(Integer maxAllowedCount){
        this.maxAllowedCount = maxAllowedCount;
    }

    public String getViolationType() { return violationType; }

    public String getSeverity() { return severity; }

    public int getMaxAllowedCount() { return maxAllowedCount; }

    public Boolean getEnabled() { return enabled; }
}