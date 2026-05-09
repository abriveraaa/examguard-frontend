package com.example.examguard.model.exam.take;

public class StudentExamRow {

    private Long examId;
    private String title;
    private String courseCode;
    private String schedule;
    private String status;
    private Long questionCount;
    private int timeLimitMinutes;

    public StudentExamRow() {

    }

    public StudentExamRow(Long examId, String title, String courseCode, String schedule,
                          String status, Long questionCount, int timeLimitMinutes) {
        this.examId = examId;
        this.title = title;
        this.courseCode = courseCode;
        this.schedule = schedule;
        this.status = status;
        this.questionCount = questionCount;
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public Long getExamId() {
        return examId;
    }
    public void setExamId(Long examId) { this.examId = examId; }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Long questionCount) {
        this.questionCount = questionCount;
    }

    public int getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(int timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

}
