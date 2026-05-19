package com.example.examguard.model.camera;

public class CameraSessionStatusResponse {

    private Long cameraSessionId;
    private Long attemptId;
    private Long examId;
    private String studentId;
    private String pairingToken;
    private String status;
    private String deviceLabel;
    private String pairedAt;
    private String lastSeenAt;
    private String expiresAt;

    public Long getCameraSessionId() {
        return cameraSessionId;
    }

    public void setCameraSessionId(Long cameraSessionId) {
        this.cameraSessionId = cameraSessionId;
    }

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Long getExamId() {
        return examId;
    }

    public void setExamId(Long examId) {
        this.examId = examId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPairingToken() {
        return pairingToken;
    }

    public void setPairingToken(String pairingToken) {
        this.pairingToken = pairingToken;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }

    public String getPairedAt() {
        return pairedAt;
    }

    public void setPairedAt(String pairedAt) {
        this.pairedAt = pairedAt;
    }

    public String getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(String lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}