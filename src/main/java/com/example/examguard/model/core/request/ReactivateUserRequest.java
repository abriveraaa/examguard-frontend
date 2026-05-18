package com.example.examguard.model.core.request;

public class ReactivateUserRequest {

    private String schoolId;
    private String role;
    private String justification;

    public ReactivateUserRequest(String schoolId, String role, String justification) {
        this.schoolId = schoolId;
        this.role = role;
        this.justification = justification;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public String getRole() {
        return role;
    }

    public String getJustification() {
        return justification;
    }
}