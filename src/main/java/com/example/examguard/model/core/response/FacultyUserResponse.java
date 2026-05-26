package com.example.examguard.model.core.response;

public class FacultyUserResponse {

    private String schoolId;
    private String username;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String collegeName;
    private String registrarStatus;
    private String systemAccess;

    public FacultyUserResponse() {
    }

    public FacultyUserResponse(String schoolId,
                               String username,
                               String firstName,
                               String lastName,
                               String email,
                               String collegeName,
                               String registrarStatus,
                               String systemAccess) {

        this.schoolId = schoolId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = buildFullName(firstName, lastName);
        this.email = email;
        this.collegeName = collegeName;
        this.registrarStatus = registrarStatus;
        this.systemAccess = systemAccess;
    }

    private String buildFullName(String first, String last) {
        String f = first == null ? "" : first;
        String l = last == null ? "" : last;
        String name = (f + " " + l).trim();
        return name.isBlank() ? "-" : name;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public String getUsername() {
        return username;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRegistrarStatus() {
        return registrarStatus;
    }

    public String getSystemAccess() {
        return systemAccess;
    }

    public String getCollegeName() {
        return collegeName;
    }
}