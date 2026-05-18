package com.example.examguard.model.faculty.dto;

import java.time.OffsetDateTime;

public class FacultyViolationReviewDTO {
    private Long examId;
    private String examTitle;
    private String courseCode;
    private Long studentCount;
    private Long violationCount;
    private OffsetDateTime latestViolationAt;

    public Long getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getCourseCode() { return courseCode; }
    public Long getStudentCount() { return studentCount; }
    public Long getViolationCount() { return violationCount; }
    public OffsetDateTime getLatestViolationAt() { return latestViolationAt; }
}
