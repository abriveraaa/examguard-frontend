package com.example.examguard.model.faculty.response;

import com.example.examguard.model.faculty.FacultyClassDTO;
import java.time.OffsetDateTime;
import java.util.List;

public class FacultyExamDetailResponse {
    private Long examId;
    private String title;
    private String description;
    private String status;
    private Integer timeLimitMinutes;
    private OffsetDateTime startDateTime;
    private OffsetDateTime endDateTime;
    private Boolean shuffleQuestions;
    private Boolean shuffleChoices;
    private Long totalStudents;
    private Long submittedCount;
    private Long notSubmittedCount;
    private Long studentsWithViolations;
    private Long totalViolations;
    private Boolean resultsReleased;
    private List<FacultyClassDTO> assignedClasses;

    public Long getExamId() { return examId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getStatus() { return status; }
    public Integer getTimeLimitMinutes() { return timeLimitMinutes; }
    public OffsetDateTime getStartDateTime() { return startDateTime; }
    public OffsetDateTime getEndDateTime() { return endDateTime; }
    public Boolean getShuffleQuestions() { return shuffleQuestions; }
    public Boolean getShuffleChoices() { return shuffleChoices; }
    public Long getTotalStudents() { return totalStudents; }
    public Long getSubmittedCount() { return submittedCount; }
    public Long getNotSubmittedCount() { return notSubmittedCount; }
    public Long getStudentsWithViolations() { return studentsWithViolations; }
    public Long getTotalViolations() { return totalViolations; }
    public Boolean getResultsReleased() { return resultsReleased; }
    public List<FacultyClassDTO> getAssignedClasses() { return assignedClasses; }
}
