package com.example.examguard.model.faculty;

import java.time.OffsetDateTime;

public class FacultyExamStudentDTO {
    private String studentId;
    private String studentName;
    private String emailAddress;
    private String programCode;
    private String sectionName;
    private String attemptStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private Double scorePercentage;
    private Long violationCount;
    private Boolean needsChecking;
    private String reviewStatus;

    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getEmailAddress() { return emailAddress; }
    public String getProgramCode() { return programCode; }
    public String getSectionName() { return sectionName; }
    public String getAttemptStatus() { return attemptStatus; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public Double getScorePercentage() { return scorePercentage; }
    public Long getViolationCount() { return violationCount; }
    public Boolean getNeedsChecking() { return needsChecking; }
    public String getReviewStatus() { return reviewStatus; }
}
