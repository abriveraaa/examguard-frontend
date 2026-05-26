package com.example.examguard.model.core;

import com.example.examguard.model.admin.AdminUserResponse;
import com.example.examguard.model.core.response.FacultyUserResponse;
import com.example.examguard.model.core.response.StudentProfileResponse;

public class UserManagementRow {

    private final String schoolId;
    private final String username;
    private final String fullName;
    private final String email;
    private final String role;
    private final String birthDate;
    private final String collegeName;
    private final String programName;
    private final String yearLevel;
    private final String sectionName;
    private final String registrarStatus;
    private final String systemAccess;

    public UserManagementRow(String schoolId,
                             String username,
                             String fullName,
                             String email,
                             String role,
                             String birthDate,
                             String collegeName,
                             String programName,
                             String yearLevel,
                             String sectionName,
                             String registrarStatus,
                             String systemAccess) {
        this.schoolId = safe(schoolId);
        this.username = safe(username);
        this.fullName = safe(fullName);
        this.email = safe(email);
        this.role = safe(role);
        this.birthDate = safe(birthDate);
        this.collegeName = safe(collegeName);
        this.programName = safe(programName);
        this.yearLevel = safe(yearLevel);
        this.sectionName = safe(sectionName);
        this.registrarStatus = safe(registrarStatus);
        this.systemAccess = safe(systemAccess);
    }

    public String getSchoolId() {
        return schoolId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getBirthDate() {
        return birthDate;
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

    public String getProgramName() {
        return programName;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public String getSectionName() {
        return sectionName;
    }

    public static UserManagementRow from(Object obj, String role) {
        if (obj == null || role == null) {
            return null;
        }

        return switch (role.toUpperCase()) {
            case "ADMIN" -> fromAdmin((AdminUserResponse) obj);
            case "STUDENT" -> fromStudent((StudentProfileResponse) obj);
            case "FACULTY" -> fromFaculty((FacultyUserResponse) obj);
            default -> null;
        };
    }

    private static UserManagementRow fromAdmin(AdminUserResponse admin) {
        if (admin == null) {
            return null;
        }

        return new UserManagementRow(
                admin.getSchoolId(),
                admin.getUsername(),
                admin.getFullName(),
                admin.getEmail(),
                "ADMIN",
                admin.getBirthDate(),
                "-",
                "-",
                "-",
                "-",
                safe(admin.getRegistrarStatus()),
                safe(admin.getSystemAccess())
        );
    }

    private static UserManagementRow fromStudent(StudentProfileResponse student) {
        if (student == null) {
            return null;
        }

        return new UserManagementRow(
                student.getSchoolId(),
                student.getUsername(),
                student.getFullName(),
                student.getEmail(),
                "STUDENT",
                null,
                student.getCollegeName(),
                student.getProgramName(),
                student.getYearLevel(),
                student.getSectionName(),
                safe(student.getRegistrarStatus()),
                safe(student.getSystemAccess())
        );
    }

    private static UserManagementRow fromFaculty(FacultyUserResponse faculty) {
        if (faculty == null) {
            return null;
        }

        return new UserManagementRow(
                faculty.getSchoolId(),
                faculty.getUsername(),
                faculty.getFullName(),
                faculty.getEmail(),
                "FACULTY",
                null,
                faculty.getCollegeName(),
                "-",
                "-",
                "-",
                safe(faculty.getRegistrarStatus()),
                safe(faculty.getSystemAccess())
        );
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}