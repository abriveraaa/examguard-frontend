package com.example.examguard.model.faculty;

import java.time.OffsetDateTime;

public class FacultyExamSummaryDTO {
    private Long examId;
    private String title;
    private String courseCode;
    private String courseDescription;
    private String programCode;
    private String classSections;
    private String status;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Long totalAssigned;
    private Long submittedCount;
    private Long violationCount;

    public Long getExamId() { return examId; }
    public String getTitle() { return title; }
    public String getCourseCode() { return courseCode; }
    public String getCourseDescription() { return courseDescription; }
    public String getProgramCode() { return programCode; }
    public String getClassSections() { return classSections; }
    public String getStatus() { return status; }
    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public OffsetDateTime getEndDateTime() { return endDateTime; }
    public Long getTotalAssigned() { return totalAssigned; }
    public Long getSubmittedCount() { return submittedCount; }
    public Long getViolationCount() { return violationCount; }
}
