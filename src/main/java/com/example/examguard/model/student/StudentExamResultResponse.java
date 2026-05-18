package com.example.examguard.model.student;

import com.example.examguard.model.student.result.StudentExamResultQuestionResponse;

import java.time.OffsetDateTime;
import java.util.List;

public class StudentExamResultResponse {

    private Long examId;
    private Long attemptId;

    private String title;
    private String courseCode;
    private String courseDescription;
    private String faculty;
    private String term;
    private String academicYear;

    private Integer durationMinutes;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;

    private Double totalScore;
    private Double totalPoints;
    private Double scorePercentage;

    private String attemptStatus;
    private String reviewStatus;
    private Boolean resultsReleased;

    private List<StudentExamResultQuestionResponse> questions;

    public Long getExamId() { return examId; }
    public Long getAttemptId() { return attemptId; }
    public String getTitle() { return title; }
    public String getCourseCode() { return courseCode; }
    public String getCourseDescription() { return courseDescription; }
    public String getFaculty() { return faculty; }
    public String getTerm() { return term; }
    public String getAcademicYear() { return academicYear; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public Double getTotalScore() { return totalScore; }
    public Double getTotalPoints() { return totalPoints; }
    public Double getScorePercentage() { return scorePercentage; }
    public String getAttemptStatus() { return attemptStatus; }
    public String getReviewStatus() { return reviewStatus; }
    public Boolean getResultsReleased() { return resultsReleased; }
    public List<StudentExamResultQuestionResponse> getQuestions() { return questions; }
}