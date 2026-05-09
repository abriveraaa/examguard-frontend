package com.example.examguard.model.exam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExamResponse {

    private Long examId;
    private String title;
    private String description;
    private String dateCreated;
    private String validUntil;
    private String status;
    private String duration;
    private String assigned;
    private String takers;
    private String startDateTime;
    private String endDateTime;
    private String examMode;
    private String createdBy;
    private String updatedBy;
    private Integer timeLimitMinutes;
    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;
    private OffsetDateTime rawStartDateTime;
    private OffsetDateTime rawEndDateTime;
    private List<String> classOfferingIds = new ArrayList<>();
    private List<QuestionPreview> questions = new ArrayList<>();

    public ExamResponse() {
    }

    public Long getExamId() { return examId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getDateCreated() { return dateCreated; }
    public String getValidUntil() { return validUntil; }
    public String getStatus() { return status; }
    public String getDuration() { return duration; }
    public String getAssigned() { return assigned; }
    public String getTakers() { return takers; }

    public String getStartDateTime() { return startDateTime; }
    public String getEndDateTime() { return endDateTime; }
    public String getExamMode() { return examMode; }

    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }

    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public Boolean getShuffleQuestions() { return shuffleQuestions; }
    public Boolean getShuffleChoices() { return shuffleChoices; }
    public OffsetDateTime getRawStartDateTime() { return rawStartDateTime; }
    public OffsetDateTime getRawEndDateTime() { return rawEndDateTime; }

    public List<String> getClassOfferingIds() {
        return classOfferingIds == null ? new ArrayList<>() : classOfferingIds;
    }

    public List<QuestionPreview> getQuestions() {
        return questions == null ? new ArrayList<>() : questions;
    }

    public static class QuestionPreview {

        private Long questionId;
        private String questionType;
        private String questionText;
        private String questionImageUrl;
        private BigDecimal points;
        private Integer questionOrder;
        private String correctAnswer;

        private List<ChoicePreview> choices = new ArrayList<>();

        public QuestionPreview() {
        }

        public Long getQuestionId() { return questionId; }
        public String getQuestionType() { return questionType; }
        public String getQuestionText() { return questionText; }
        public String getQuestionImageUrl() { return questionImageUrl; }
        public BigDecimal getPoints() { return points; }
        public Integer getQuestionOrder() { return questionOrder; }
        public String getCorrectAnswer() { return correctAnswer; }

        public List<ChoicePreview> getChoices() {
            return choices == null ? new ArrayList<>() : choices;
        }
    }

    public static class ChoicePreview {

        private Long choiceId;
        private String choiceLabel;
        private String choiceText;
        private String choiceImageUrl;
        private boolean correct;

        public ChoicePreview() {
        }

        public Long getChoiceId() { return choiceId; }
        public String getChoiceLabel() { return choiceLabel; }
        public String getChoiceText() { return choiceText; }
        public String getChoiceImageUrl() { return choiceImageUrl; }
        public boolean isCorrect() { return correct; }
    }
}