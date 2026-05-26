package com.example.examguard.model.profile;

import java.util.List;

public class ProfileResponseDTO {

    private String role;
    private String fullName;
    private String schoolEmail;
    private String schoolId;
    private String collegeOrOffice;
    private String programOrPosition;
    private String username;
    private String accountStatus;
    private String memberSince;
    private String tenureDuration;
    private String passwordStatus;
    private String passwordLastChanged;
    private String profileImageUrl;
    private String currentAcademicYear;
    private String currentTerm;

    private List<ProfileClassDTO> classes;
    private List<ProfileActivityDTO> recentActivities;

    public String getRole() { return role; }
    public String getFullName() { return fullName; }
    public String getSchoolEmail() { return schoolEmail; }
    public String getSchoolId() { return schoolId; }
    public String getCollegeOrOffice() { return collegeOrOffice; }
    public String getProgramOrPosition() { return programOrPosition; }
    public String getUsername() { return username; }
    public String getAccountStatus() { return accountStatus; }
    public String getMemberSince() { return memberSince; }
    public String getTenureDuration() { return tenureDuration; }
    public String getPasswordStatus() { return passwordStatus; }
    public String getPasswordLastChanged() { return passwordLastChanged; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getCurrentAcademicYear() { return currentAcademicYear; }
    public String getCurrentTerm() { return currentTerm; }
    public List<ProfileClassDTO> getClasses() { return classes; }
    public List<ProfileActivityDTO> getRecentActivities() { return recentActivities; }
}