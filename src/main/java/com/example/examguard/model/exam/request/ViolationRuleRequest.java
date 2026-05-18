package com.example.examguard.model.exam.request;

public class ViolationRuleRequest {

    private boolean enabled;
    private String severity;
    private int allowedAttempts;

    public ViolationRuleRequest() {
    }

    public ViolationRuleRequest(boolean enabled, String severity, int allowedAttempts) {
        this.enabled = enabled;
        this.severity = severity;
        this.allowedAttempts = allowedAttempts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getAllowedAttempts() {
        return allowedAttempts;
    }

    public void setAllowedAttempts(int allowedAttempts) {
        this.allowedAttempts = allowedAttempts;
    }
}
