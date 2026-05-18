package com.example.examguard.model.exam.dto;


import java.time.OffsetDateTime;

public class ExamLeaderboardDTO {
    private Integer rank;
    private Long attemptId;
    private String studentId;
    private String studentName;
    private String sectionName;
    private Double totalScore;
    private Double totalPossibleScore;
    private Double scorePercentage;
    private Long violationCount;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private String integrityStatus;

    public Integer getRank() { return rank; }
    public Long getAttemptId() { return attemptId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getSectionName() { return sectionName; }
    public Double getTotalScore() { return totalScore; }
    public Double getTotalPossibleScore() { return totalPossibleScore; }
    public Double getScorePercentage() { return scorePercentage; }
    public Long getViolationCount() { return violationCount; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public String getIntegrityStatus() { return integrityStatus; }
}
