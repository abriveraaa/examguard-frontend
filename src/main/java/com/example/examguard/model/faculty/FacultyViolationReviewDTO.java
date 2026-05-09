package com.example.examguard.model.faculty;

import java.time.OffsetDateTime;

public class FacultyViolationReviewDTO {
    private Long examId;
    private String examTitle;
    private String courseCode;
    private String sectionName;
    private Long studentCount;
    private Long violationCount;
    private OffsetDateTime latestViolationAt;

    public Long getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getCourseCode() { return courseCode; }
    public String getSectionName() { return sectionName; }
    public Long getStudentCount() { return studentCount; }
    public Long getViolationCount() { return violationCount; }
    public OffsetDateTime getLatestViolationAt() { return latestViolationAt; }
}
