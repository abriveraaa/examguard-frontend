package com.example.examguard.model.core.response;

public class ReactivationCandidateResponse {

    private String schoolId;
    private String username;
    private String role;
    private Boolean active;
    private Boolean eligibleForReactivation;
    private String deactivationReason;

    public String getSchoolId() {
        return schoolId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public Boolean getActive() {
        return active;
    }

    public Boolean getEligibleForReactivation() {
        return eligibleForReactivation;
    }

    public String getDeactivationReason() {
        return deactivationReason;
    }
}