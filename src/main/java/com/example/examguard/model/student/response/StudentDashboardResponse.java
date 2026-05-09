package com.example.examguard.model.student.response;

import com.example.examguard.model.student.*;

import java.util.List;

public class StudentDashboardResponse {

    private StudentProfile profile;
    private List<StudentUpcomingExam> upcomingExams;
    private List<StudentResultSummary> resultSummary;
    private List<StudentViolationSummary> violations;
    private StudentDashboardStats stats;

    public StudentProfile getProfile() {
        return profile;
    }

    public List<StudentUpcomingExam> getUpcomingExams() {
        return upcomingExams;
    }

    public List<StudentResultSummary> getResultSummary() {
        return resultSummary;
    }

    public List<StudentViolationSummary> getViolations() {
        return violations;
    }

    public StudentDashboardStats getStats() {
        return stats;
    }
}