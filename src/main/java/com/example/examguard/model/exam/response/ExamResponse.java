package com.example.examguard.model.exam.response;

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
    private String term;
    private String academicYear;
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
    private final List<String> classOfferingIds = new ArrayList<>();
    private final List<QuestionPreview> questions = new ArrayList<>();

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
    public String getTerm() { return term; }
    public String getAcademicYear() { return academicYear; }
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
    public List<String> getClassOfferingIds() { return classOfferingIds; }

    public List<QuestionPreview> getQuestions() {
        return questions;
    }

    public static class QuestionPreview {

        private Long questionId;
        private String questionType;
        private String questionText;
        private String questionImageUrl;
        private BigDecimal points;
        private Integer questionOrder;
        private String correctAnswer;
        private String questionInstruction;
        private List<EssayRubricResponse> rubrics;
        private final List<ChoicePreview> choices = new ArrayList<>();

        public QuestionPreview() {
        }

        public Long getQuestionId() { return questionId; }
        public String getQuestionType() { return questionType; }
        public String getQuestionText() { return questionText; }
        public String getQuestionImageUrl() { return questionImageUrl; }
        public BigDecimal getPoints() { return points; }
        public Integer getQuestionOrder() { return questionOrder; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getQuestionInstruction() { return questionInstruction; }
        public List<EssayRubricResponse> getRubrics() { return rubrics; }
        public List<ChoicePreview> getChoices() {
            return choices;
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

    public static class EssayRubricResponse {

        private Long rubricId;
        private String criterionName;
        private BigDecimal weightPercentage;
        private String description;
        private Integer displayOrder;

        public EssayRubricResponse() {
        }

        public EssayRubricResponse(
                Long rubricId,
                String criterionName,
                BigDecimal weightPercentage,
                String description,
                Integer displayOrder
        ) {
            this.rubricId = rubricId;
            this.criterionName = criterionName;
            this.weightPercentage = weightPercentage;
            this.description = description;
            this.displayOrder = displayOrder;
        }

        public Long getRubricId() {
            return rubricId;
        }

        public String getCriterionName() {
            return criterionName;
        }

        public BigDecimal getWeightPercentage() {
            return weightPercentage;
        }

        public String getDescription() {
            return description;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }
    }
}