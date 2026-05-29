package com.example.examguard.cache;

public final class FacultyLocalCacheKeys {

    private FacultyLocalCacheKeys() {}

    public static final String VERSION = "v1";

    public static String dashboardProfile(String facultyId) {
        return "faculty/dashboard/profile/" + facultyId;
    }

    public static String dashboardStats(String facultyId) {
        return "faculty/dashboard/stats/" + facultyId;
    }

    public static String dashboardActiveExams(String facultyId) {
        return "faculty/dashboard/active-exams/" + facultyId;
    }

    public static String dashboardNeedsReview(String facultyId) {
        return "faculty/dashboard/needs-review/" + facultyId;
    }

    public static String dashboardClasses(String facultyId) {
        return "faculty/dashboard/classes/" + facultyId;
    }

    public static String exams(String facultyId) {
        return "faculty/exams/" + facultyId;
    }

    public static String studentAcademicPeriods(String facultyId) {
        return "faculty/students/academic-periods/" + facultyId;
    }

    public static String studentsByPeriod(
            String facultyId,
            String academicYear,
            String term
    ) {
        return "faculty/students/by-period/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term);
    }

    public static String reportsAcademicPeriods(String facultyId) {
        return "faculty/reports/academic-periods/" + facultyId;
    }

    public static String reportsStudentsByPeriod(
            String facultyId,
            String academicYear,
            String term
    ) {
        return "faculty/reports/students/by-period/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term);
    }

    public static String reportsSummary(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return "faculty/reports/summary/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId);
    }

    public static String reportsParticipation(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return "faculty/reports/participation/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId);
    }

    public static String reportsSubmissionStatus(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId,
            Long examId
    ) {
        return "faculty/reports/submission-status/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId) + "/"
                + safe(examId == null ? null : String.valueOf(examId));
    }

    public static String reportsSubmissionBreakdown(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return "faculty/reports/submission-breakdown/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId);
    }

    public static String reportsViolations(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return "faculty/reports/violations/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId);
    }

    public static String reportsExamOptions(
            String facultyId,
            String academicYear,
            String term,
            String courseCode,
            String classOfferingId
    ) {
        return "faculty/reports/exam-options/"
                + facultyId + "/"
                + safe(academicYear) + "/"
                + safe(term) + "/"
                + safe(courseCode) + "/"
                + safe(classOfferingId);
    }

    private static String safe(String value) {
        if (value == null) {
            return "none";
        }

        return value
                .trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}