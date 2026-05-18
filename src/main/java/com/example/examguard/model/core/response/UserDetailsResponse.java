package com.example.examguard.model.core.response;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UserDetailsResponse {

    private String schoolId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private String collegeName;
    private String programName;
    private String yearLevel;
    private String sectionName;
    private String accountStatus;
    private String systemAccess;
    private Integer failedAttempts;
    private String lastLogin;

    public String getSchoolId() { return schoolId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getAccountStatus() { return accountStatus; }
    public String getSystemAccess() { return systemAccess; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public String getLastLogin() {
        return formatDateTime(lastLogin);
    }

    private String formatDateTime(String isoDate) {
        try {

            System.out.println(isoDate);
            if (isoDate == null || isoDate.isBlank()) {
                return "Never logged in";
            }

            String cleaned = isoDate.trim().replace("\"", "");

            OffsetDateTime date = OffsetDateTime.parse(cleaned);
            ZonedDateTime phTime = date.atZoneSameInstant(ZoneId.of("Asia/Manila"));

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

            return phTime.format(formatter);

        } catch (Exception e) {
            return "Invalid date";
        }
    }
    public String getCollegeName() { return collegeName; }
    public String getProgramName() { return programName; }
    public String getYearLevel() { return yearLevel; }
    public String getSectionName() { return sectionName; }
}