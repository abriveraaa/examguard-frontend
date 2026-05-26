package com.example.examguard.model.exam.dto;

import java.time.OffsetDateTime;

public class ExamStudentViolationDTO {
    private Long attemptId;
    private Long examId;
    private String studentId;
    private String studentName;
    private String courseCode;
    private String sectionName;
    private Long violationCount;
    private String violationLabel;
    private String highestSeverity;
    private OffsetDateTime latestViolationAt;
    private String reviewStatus;

    public Long getAttemptId() {
        return attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getSectionName() {
        return sectionName;
    }

    public Long getViolationCount() {
        return violationCount;
    }

    public String getViolationLabel() {
        return violationLabel;
    }

    public String getHighestSeverity() {
        return highestSeverity;
    }

    public OffsetDateTime getLatestViolationAt() {
        return latestViolationAt;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }
}
