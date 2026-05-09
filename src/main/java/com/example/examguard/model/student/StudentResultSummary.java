package com.example.examguard.model.student;

public class StudentResultSummary {

    private Long examId;
    private String courseCode;
    private String examTitle;
    private Double scorePercentage;
    private Integer totalScore;
    private Integer totalPoints;
    private String submittedAt;
    private String attemptStatus;

    public Long getExamId() { return examId; }
    public String getCourseCode() { return courseCode; }
    public String getExamTitle() { return examTitle; }
    public Double getScorePercentage() { return scorePercentage; }
    public Integer getTotalScore() { return totalScore; }
    public Integer getTotalPoints() { return totalPoints; }
    public String getSubmittedAt() { return submittedAt; }

    public String getAttemptStatus() { return attemptStatus;}
}
