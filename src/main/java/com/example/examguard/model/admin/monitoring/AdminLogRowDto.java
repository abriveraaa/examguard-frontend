package com.example.examguard.model.admin.monitoring;

public class AdminLogRowDto {

    private String source;
    private String startedAt;
    private String endedAt;

    private String actorId;
    private String actorRole;

    private String targetUserId;
    private String targetRole;

    private Long durationMs;

    private String courseCode;
    private String examTitle;
    private Integer questionNumber;

    private String module;
    private String action;
    private String status;
    private String message;

    private Long examId;
    private Long attemptId;
    private Long questionId;

    private String programCode;
    private String programName;
    private String section;

    private String severity;

    public String getSource() { return source; }
    public String getStartedAt() { return startedAt; }
    public String getEndedAt() { return endedAt; }
    public String getActorId() { return actorId; }
    public String getActorRole() { return actorRole; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Long getExamId() { return examId; }
    public Long getAttemptId() { return attemptId; }
    public Long getQuestionId() { return questionId; }
    public String getProgramCode() { return programCode; }
    public String getProgramName() { return programName; }
    public String getSection() { return section; }
    public String getSeverity() { return severity; }
    public String getTargetUserId() { return targetUserId; }
    public String getTargetRole() { return targetRole; }
    public Long getDurationMs() { return durationMs; }
    public String getCourseCode() { return courseCode; }
    public String getExamTitle() { return examTitle; }
    public Integer getQuestionNumber() { return questionNumber; }
}