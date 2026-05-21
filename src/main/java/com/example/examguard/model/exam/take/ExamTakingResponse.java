package com.example.examguard.model.exam.take;

import com.example.examguard.model.exam.request.ViolationSettingRequest;
import com.example.examguard.model.enums.ExamMode;

import java.time.OffsetDateTime;
import java.util.List;

public class ExamTakingResponse {

    private Long attemptId;
    private Long examId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;

    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;

    private ExamMode examMode;

    private OffsetDateTime serverNow;
    private OffsetDateTime lobbyOpenAt;
    private OffsetDateTime attemptStartedAt;

    private Boolean canBeginExam;
    private Long remainingSeconds;

    private List<ExamTakeQuestion> questions;
    private List<ViolationSettingRequest> violationSettings;

    public Long getAttemptId() {
        return attemptId;
    }

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

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public ExamMode getExamMode() {
        return examMode;
    }

    public OffsetDateTime getServerNow() {
        return serverNow;
    }

    public OffsetDateTime getLobbyOpenAt() {
        return lobbyOpenAt;
    }

    public OffsetDateTime getAttemptStartedAt() {
        return attemptStartedAt;
    }

    public Boolean getCanBeginExam() {
        return canBeginExam;
    }

    public Long getRemainingSeconds() {
        return remainingSeconds;
    }

    public List<ExamTakeQuestion> getQuestions() {
        return questions;
    }

    public List<ViolationSettingRequest> getViolationSettings() {
        return violationSettings;
    }
}