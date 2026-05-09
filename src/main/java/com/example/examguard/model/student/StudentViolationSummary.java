package com.example.examguard.model.student;

public class StudentViolationSummary {

    private Long violationId;
    private Long examId;
    private String courseCode;
    private String examTitle;
    private String violationType;
    private Long countViolation;
    private String detectedAt;
    private String status;
    private String proofUrl;

    public Long getViolationId() { return violationId; }
    public Long getExamId() { return examId; }
    public String getCourseCode() { return courseCode; }
    public String getExamTitle() { return examTitle; }
    public String getViolationType() { return violationType; }
    public Long getCountViolation() { return countViolation; }
    public String getDetectedAt() { return detectedAt; }
    public String getStatus() { return status; }
    public String getProofUrl() { return proofUrl; }
}
