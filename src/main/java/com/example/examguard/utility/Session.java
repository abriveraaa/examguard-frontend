package com.example.examguard.utility;

public class Session {

    private static String schoolId;
    private static String username;
    private static String firstName;
    private static String lastName;
    private static String role;
    private static String email;
    private static String sessionToken;
    private static boolean mustChangePassword;

    public static String getSchoolId() {
        return schoolId;
    }

    public static void setSchoolId(String schoolId) {
        Session.schoolId = schoolId;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Session.username = username;
    }

    public static String getFirstName() { return firstName; }

    public static void setFirstName(String firstName) { Session.firstName = firstName; }

    public static String getLastName() { return lastName; }

    public static void setLastName(String lastName) { Session.lastName = lastName; }

    public static String getRole() {
        return role;
    }

    public static void setRole(String role) {
        Session.role = role;
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String email) {
        Session.email = email;
    }

    public static String getSessionToken() {
        return sessionToken;
    }

    public static void setSessionToken(String sessionToken) {
        Session.sessionToken = sessionToken;
    }

    public static boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public static void setMustChangePassword(boolean mustChangePassword) {
        Session.mustChangePassword = mustChangePassword;
    }

    public static void clear() {
        schoolId = null;
        username = null;
        role = null;
        email = null;
        sessionToken = null;
        mustChangePassword = false;
    }
}