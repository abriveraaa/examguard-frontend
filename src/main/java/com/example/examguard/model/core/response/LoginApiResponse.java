package com.example.examguard.model.core.response;

public class LoginApiResponse {

    private boolean success;
    private String message;
    private String username;
    private String schoolId;
    private String firstName;
    private String lastName;
    private String role;
    private boolean mustChangePassword;
    private String sessionToken;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getSchoolId() { return schoolId; }
    public String getRole() { return role; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public String getSessionToken() { return sessionToken; }
}