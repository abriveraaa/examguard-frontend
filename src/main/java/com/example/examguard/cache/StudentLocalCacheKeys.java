package com.example.examguard.cache;

public class StudentLocalCacheKeys {

    public static String profile(String schoolId) {
        return "student-profile-" + schoolId;
    }

    public static String dashboardProfile(String schoolId) {
        return "student-dashboard-profile-" + schoolId;
    }

    public static String dashboardUpcoming(String schoolId) {
        return "student-dashboard-upcoming-" + schoolId;
    }

    public static String dashboardResults(String schoolId) {
        return "student-dashboard-results-" + schoolId;
    }

    public static String dashboardStats(String schoolId) {
        return "student-dashboard-stats-" + schoolId;
    }

    public static String exams(String schoolId) {
        return "student-exams-" + schoolId;
    }
}