package com.example.examguard.model.exam.take;

public class ExamTakeChoice {

    private Long choiceId;
    private String label;
    private String choiceText;
    private Integer choiceOrder;
    private String choiceImageUrl;

    public Long getChoiceId() {
        return choiceId;
    }

    public String getLabel() {
        return label;
    }

    public String getChoiceText() {
        return choiceText;
    }

    public Integer getChoiceOrder() {
        return choiceOrder;
    }

    public String getChoiceImageUrl() {
        return choiceImageUrl;
    }

    public void setChoiceId(Long choiceId) {
        this.choiceId = choiceId;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setChoiceText(String choiceText) {
        this.choiceText = choiceText;
    }

    public void setChoiceOrder(Integer choiceOrder) {
        this.choiceOrder = choiceOrder;
    }

    public void setChoiceImageUrl(String choiceImageUrl) {
        this.choiceImageUrl = choiceImageUrl;
    }
}
