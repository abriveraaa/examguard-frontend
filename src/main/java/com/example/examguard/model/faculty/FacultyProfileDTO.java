package com.example.examguard.model.faculty;

public class FacultyProfileDTO {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String emailAddress;

    public String getEmployeeId() { return employeeId; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmailAddress() { return emailAddress; }

    public String getFullName() {
        return ((firstName == null ? "" : firstName) + " " +
                (lastName == null ? "" : lastName)).trim();
    }
}