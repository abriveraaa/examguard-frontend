package com.example.examguard.model.exam.result;

public class ExamResult {

    private boolean success;
    private String message;
    private Long examId;
    private int questionCount;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Long getExamId() {
        return examId;
    }

    public int getQuestionCount() {
        return questionCount;
    }
}