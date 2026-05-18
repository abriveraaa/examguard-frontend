package com.example.examguard.utility;

public class LobbyCheckResult {

    private final boolean passed;
    private final String message;

    public LobbyCheckResult(boolean passed, String message) {
        this.passed = passed;
        this.message = message;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }

    public static LobbyCheckResult pass(String message) {
        return new LobbyCheckResult(true, message);
    }

    public static LobbyCheckResult fail(String message) {
        return new LobbyCheckResult(false, message);
    }
}