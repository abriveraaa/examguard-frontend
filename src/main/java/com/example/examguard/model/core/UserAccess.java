package com.example.examguard.model.core;

public class UserAccess {

    private String username;
    private String schoolId;
    private String role;
    private boolean mustChangePassword;

    public String getUsername() {
        return username;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public String getRole() {
        return role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }
}
