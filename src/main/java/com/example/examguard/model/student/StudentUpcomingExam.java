package com.example.examguard.model.student;

import java.time.OffsetDateTime;

public class StudentUpcomingExam {

    private Long examId;
    private String title;
    private String faculty;
    private String courseCode;
    private String courseTitle;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Integer timeLimitMinutes;
    private String examMode;
    private String status;
    private Long questionCount;
    private String attemptStatus;

    public Long getExamId() { return examId; }
    public String getTitle() { return title; }
    public String getFaculty() { return faculty; }
    public String getCourseCode() { return courseCode; }
    public String getCourseTitle() { return courseTitle; }
    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public OffsetDateTime getEndDateTime() { return endDateTime; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public String getExamMode() { return examMode; }
    public String getStatus() { return status; }
    public Long getQuestionCount() { return questionCount; }
    public String getAttemptStatus() { return attemptStatus; }
}
