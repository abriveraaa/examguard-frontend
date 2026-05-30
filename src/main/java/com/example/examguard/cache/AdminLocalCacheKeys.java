package com.example.examguard.cache;

public class AdminLocalCacheKeys {

    public static String dashboardOverview(String adminId, String range, String groupBy) {
        return "admin-dashboard-overview-"
                + safe(adminId)
                + "-"
                + safe(range)
                + "-"
                + safe(groupBy);
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.trim()
                .replace(" ", "_")
                .replace("/", "_")
                .replace("\\", "_")
                .replace(":", "_");
    }

    public static String users(String adminId, String role) {
        return "admin-users-" + safe(adminId) + "-" + safe(role);
    }

    public static String eligibleReactivationUsers(String adminId) {
        return "admin-eligible-reactivation-" + safe(adminId);
    }

    public static String lastSuccessfulSync(String adminId) {
        return "admin-last-successful-sync-" + safe(adminId);
    }

    public static String monitoringLogs(String adminId, String source) {
        return "admin-monitoring-"
                + safe(adminId)
                + "-"
                + safe(source);
    }
}