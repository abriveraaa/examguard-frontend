package com.example.examguard.model.faculty.response;

import com.example.examguard.model.faculty.*;

import java.util.List;

public class FacultyDashboardResponse {
    private FacultyProfileDTO profile;
    private FacultyDashboardStatsDTO stats;
    private List<FacultyExamSummaryDTO> activeExams;
    private List<FacultyViolationReviewDTO> needsReview;
    private List<FacultySubmissionSummaryDTO> recentSubmissions;

    public FacultyProfileDTO getProfile() { return profile; }
    public FacultyDashboardStatsDTO getStats() { return stats; }
    public List<FacultyExamSummaryDTO> getActiveExams() { return activeExams; }
    public List<FacultyViolationReviewDTO> getNeedsReview() { return needsReview; }
    public List<FacultySubmissionSummaryDTO> getRecentSubmissions() { return recentSubmissions; }
}
