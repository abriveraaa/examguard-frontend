package com.example.examguard.model.exam.request;

import java.time.OffsetDateTime;

public class ViolationLogRequest {

    private Long attemptId;
    private Long examId;
    private Long questionId;

    private String violationType;
    private String severity;
    private String violationMessage;

    private Integer attemptNumber;
    private OffsetDateTime occurredAt;

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public void setViolationMessage(String violationMessage) {
        this.violationMessage = violationMessage;
    }

    public void setAttemptNumber(Integer attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setOccurredAt(OffsetDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }
}
