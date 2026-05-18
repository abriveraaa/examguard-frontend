package com.example.examguard.model.student;

import java.time.OffsetDateTime;

public class StudentExamResponse {

    private Long examId;
    private String title;
    private String courseCode;
    private String courseDescription;
    private String term;
    private String academicYear;
    private String classOfferingStatus;
    private String mode;
    private String faculty;
    private Integer durationMinutes;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private String attemptStatus;
    private String reviewStatus;
    private Boolean resultsReleased;
    private String status;
    private Long questionCount;
    private Boolean actionable;

    public Long getExamId() { return examId; }
    public String getTitle() { return title; }
    public String getCourseCode() { return courseCode; }
    public String getCourseDescription() { return courseDescription; }
    public String getTerm() { return term; }
    public String getAcademicYear() { return academicYear; }
    public String getClassOfferingStatus() { return classOfferingStatus; }
    public String getMode() { return mode; }
    public String getFaculty() { return faculty; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public OffsetDateTime getEndDateTime() { return endDateTime; }
    public String getAttemptStatus() { return attemptStatus; }
    public String getReviewStatus() { return reviewStatus; }
    public Boolean getResultsReleased() { return resultsReleased; }
    public String getStatus() { return status; }
    public Long getQuestionCount() { return questionCount; }
    public Boolean getActionable() { return actionable; }
}