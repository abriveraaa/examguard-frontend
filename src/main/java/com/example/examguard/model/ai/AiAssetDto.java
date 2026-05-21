package com.example.examguard.model.ai;

public class AiAssetDto {

    private String key;
    private String version;
    private String type;
    private String fileName;
    private String sha256;
    private String downloadUrl;

    public String getKey() {
        return key;
    }

    public String getVersion() {
        return version;
    }

    public String getType() {
        return type;
    }

    public String getFileName() {
        return fileName;
    }

    public String getSha256() {
        return sha256;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}