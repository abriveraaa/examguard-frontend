package com.example.examguard.model.faculty.response;

import com.example.examguard.model.exam.dto.ExamAttemptAnswerReviewDTO;
import com.example.examguard.model.exam.dto.ExamAttemptViolationDTO;

import java.time.OffsetDateTime;
import java.util.List;

public class FacultyAttemptReviewResponse {

    private Long attemptId;
    private Long examId;
    private String studentId;
    private String studentName;
    private String attemptStatus;
    private Double scorePercentage;
    private Boolean needsChecking;
    private OffsetDateTime startedAt;
    private OffsetDateTime submittedAt;
    private List<ExamAttemptAnswerReviewDTO> answers;
    private List<ExamAttemptViolationDTO> generalViolations;

    public Long getAttemptId() { return attemptId; }
    public Long getExamId() { return examId; }
    public String getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getAttemptStatus() { return attemptStatus; }
    public Double getScorePercentage() { return scorePercentage; }
    public Boolean getNeedsChecking() { return needsChecking; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getSubmittedAt() { return submittedAt; }
    public List<ExamAttemptAnswerReviewDTO> getAnswers() { return answers; }
    public List<ExamAttemptViolationDTO> getGeneralViolations() { return generalViolations; }
}
