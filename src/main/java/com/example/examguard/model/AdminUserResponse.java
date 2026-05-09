package com.example.examguard.model;

public class AdminUserResponse {

    private String schoolId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String birthDate;
    private String registrarStatus;   // profile status
    private String systemAccess;      // ExamGuard access

    public AdminUserResponse() {}

    public AdminUserResponse(String schoolId,
                             String username,
                             String firstName,
                             String lastName,
                             String email,
                             String registrarStatus,
                             String systemAccess) {

        this.schoolId = schoolId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = buildFullName(firstName, lastName);
        this.email = email;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
    }

    private String buildFullName(String first, String last) {
        String f = first == null ? "" : first;
        String l = last == null ? "" : last;
        String name = (f + " " + l).trim();
        return name.isBlank() ? "-" : name;
    }

    public String getSchoolId() { return schoolId; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getBirthDate() { return birthDate; }
    public String getRegistrarStatus() { return registrarStatus; }
    public String getSystemAccess() { return systemAccess; }
}