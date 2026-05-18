package com.example.examguard.model.student.result;

public class StudentExamResultRubricResponse {

    private Long rubricId;
    private String criterionName;
    private Double weightPercentage;
    private String description;
    private Integer displayOrder;
    private Double scoreAwarded;
    private Double scorePercentage;
    private String feedback;

    public Long getRubricId() { return rubricId; }
    public String getCriterionName() { return criterionName; }
    public Double getWeightPercentage() { return weightPercentage; }
    public String getDescription() { return description; }
    public Integer getDisplayOrder() { return displayOrder; }
    public Double getScoreAwarded() { return scoreAwarded; }
    public Double getScorePercentage() { return scorePercentage; }
    public String getFeedback() { return feedback; }
}