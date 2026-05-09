package com.example.examguard.model.exam;

import java.time.OffsetDateTime;
import java.util.List;

public class ExamRequest {

    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;

    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;

    private String examMode;
    private List<String> classOfferingIds;
    private List<QuestionRequest> questions;
    private List<ViolationSettingRequest> violationSettings;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Integer timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Boolean getShuffleQuestions() {
        return shuffleQuestions;
    }

    public void setShuffleQuestions(Boolean shuffleQuestions) {
        this.shuffleQuestions = shuffleQuestions;
    }

    public Boolean getShuffleChoices() {
        return shuffleChoices;
    }

    public void setShuffleChoices(Boolean shuffleChoices) {
        this.shuffleChoices = shuffleChoices;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(OffsetDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getExamMode() {
        return examMode;
    }

    public void setExamMode(String examMode) {
        this.examMode = examMode;
    }

    public List<String> getClassOfferingIds() {
        return classOfferingIds;
    }

    public void setClassOfferingIds(List<String> classOfferingIds) {
        this.classOfferingIds = classOfferingIds;
    }

    public List<QuestionRequest> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionRequest> questions) {
        this.questions = questions;
    }

    public List<ViolationSettingRequest> getViolationSettings() {
        return violationSettings;
    }

    public void setViolationSettings(List<ViolationSettingRequest> violationSettings) {
        this.violationSettings = violationSettings;
    }
}