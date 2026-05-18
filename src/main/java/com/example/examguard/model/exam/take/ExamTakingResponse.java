package com.example.examguard.model.exam.take;

import com.example.examguard.model.exam.request.ViolationSettingRequest;

import java.util.List;

public class ExamTakingResponse {

    private Long attemptId;
    private Long examId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private List<ExamTakeQuestion> questions;
    private List<ViolationSettingRequest> violationSettings;

    public Long getAttemptId() { return attemptId; }

    public Long getExamId() {
        return examId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public List<ExamTakeQuestion> getQuestions() {
        return questions;
    }

    public List<ViolationSettingRequest> getViolationSettings() { return violationSettings; }

}
