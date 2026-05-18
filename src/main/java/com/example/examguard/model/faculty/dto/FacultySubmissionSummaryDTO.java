package com.example.examguard.model.faculty.dto;

import java.time.OffsetDateTime;

public class FacultySubmissionSummaryDTO {
    private Long attemptId;
    private Long examId;
    private String examTitle;
    private String studentId;
    private String studentName;
    private String courseCode;
    private String sectionName;
    private String attemptStatus;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private Double scorePercentage;
    private Long violationCount;
    private Boolean needsChecking;

    public Long getAttemptId() { return attemptId; }
    public Long getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getCourseCode() { return courseCode; }
    public String getSectionName() { return sectionName; }
    public String getAttemptStatus() { return attemptStatus; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public Double getScorePercentage() { return scorePercentage; }
    public Long getViolationCount() { return violationCount; }
    public Boolean getNeedsChecking() { return needsChecking; }
}
