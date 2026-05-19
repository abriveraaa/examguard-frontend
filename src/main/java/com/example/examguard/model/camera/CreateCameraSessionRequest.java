package com.example.examguard.model.camera;

public class CreateCameraSessionRequest {

    private Long attemptId;
    private Long examId;
    private String studentId;

    public CreateCameraSessionRequest() {
    }

    public CreateCameraSessionRequest(Long attemptId, Long examId, String studentId) {
        this.attemptId = attemptId;
        this.examId = examId;
        this.studentId = studentId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}