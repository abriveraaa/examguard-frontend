package com.example.examguard.model.faculty;

public class FacultyDashboardStatsDTO {

    private Long activeExamCount;
    private Long classOfferingCount;
    private Long totalStudentCount;
    private Long reviewQueueCount;

    public Long getActiveExamCount() {
        return activeExamCount;
    }

    public Long getClassOfferingCount() {
        return classOfferingCount;
    }

    public Long getTotalStudentCount() {
        return totalStudentCount;
    }

    public Long getReviewQueueCount() {
        return reviewQueueCount;
    }
}
