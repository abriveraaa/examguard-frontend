package com.example.examguard.cache;

public final class ExamLocalCacheKeys {

    private ExamLocalCacheKeys() {}

    public static final String VERSION = "v1";

    public static String exams(String role, String userId) {
        return "exams/list/" + safe(role) + "/" + safe(userId);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .trim()
                .toUpperCase()
                .replaceAll("[^A-Z0-9._-]", "_");
    }

    public static String workspaceOverview(String role, String userId, Long examId) {
        return "exams/workspace/overview/"
                + safe(role) + "/"
                + safe(userId) + "/"
                + examId;
    }

    public static String workspaceStudents(String role, String userId, Long examId) {
        return "exams/workspace/students/"
                + safe(role) + "/"
                + safe(userId) + "/"
                + examId;
    }
}