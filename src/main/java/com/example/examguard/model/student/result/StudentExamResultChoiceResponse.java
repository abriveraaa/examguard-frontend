package com.example.examguard.model.student.result;

public class StudentExamResultChoiceResponse {

    private Long choiceId;
    private String choiceLabel;
    private String choiceText;
    private String imageUrl;
    private Boolean correct;
    private Boolean selected;

    public Long getChoiceId() { return choiceId; }
    public String getChoiceLabel() { return choiceLabel; }
    public String getChoiceText() { return choiceText; }
    public String getImageUrl() { return imageUrl; }
    public Boolean getCorrect() { return correct; }
    public Boolean getSelected() { return selected; }
}