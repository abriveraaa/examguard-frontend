package com.example.examguard.model.exam.request;

import java.math.BigDecimal;

public class EssayRubricRequest {

    private Long rubricId;
    private String criterionName;
    private BigDecimal weightPercentage;
    private String description;
    private Integer displayOrder;

    public Long getRubricId() {
        return rubricId;
    }

    public void setRubricId(Long rubricId) {
        this.rubricId = rubricId;
    }

    public String getCriterionName() {
        return criterionName;
    }

    public void setCriterionName(String criterionName) {
        this.criterionName = criterionName;
    }

    public BigDecimal getWeightPercentage() {
        return weightPercentage;
    }

    public void setWeightPercentage(BigDecimal weightPercentage) {
        this.weightPercentage = weightPercentage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}