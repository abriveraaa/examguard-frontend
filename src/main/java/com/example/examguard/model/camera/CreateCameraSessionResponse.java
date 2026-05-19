package com.example.examguard.model.camera;

public class CreateCameraSessionResponse {

    private Long cameraSessionId;
    private String pairingToken;
    private String pairingUrl;
    private String status;
    private String expiresAt;

    public Long getCameraSessionId() {
        return cameraSessionId;
    }

    public void setCameraSessionId(Long cameraSessionId) {
        this.cameraSessionId = cameraSessionId;
    }

    public String getPairingToken() {
        return pairingToken;
    }

    public void setPairingToken(String pairingToken) {
        this.pairingToken = pairingToken;
    }

    public String getPairingUrl() {
        return pairingUrl;
    }

    public void setPairingUrl(String pairingUrl) {
        this.pairingUrl = pairingUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }
}