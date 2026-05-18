package com.example.examguard.model.student.dashboard;

public class StudentViolationSummary {

    private Long violationId;
    private Long examId;
    private String courseCode;
    private String examTitle;
    private String status;

    public Long getViolationId() {
        return violationId;
    }

    public Long getExamId() {
        return examId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getExamTitle() {
        return examTitle;
    }

    public String getStatus() {
        return status;
    }
}