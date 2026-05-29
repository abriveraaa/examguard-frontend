package com.example.examguard.service;

import com.example.examguard.config.AppConfig;
import com.example.examguard.model.ai.AiAssetDto;
import com.example.examguard.model.ai.AiAssetManifestDto;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

public class AiAssetSyncService {

    private static final String BASE_URL = AppConfig.BASE_URL;

    private static final Path AI_ROOT = Paths.get(System.getProperty("user.home"), "ExamGuard", "ai");

    private static final Path LOCAL_MANIFEST = AI_ROOT.resolve("manifest.json");

    private final Gson gson = new Gson();

    public void syncAssets() throws Exception {
        Files.createDirectories(AI_ROOT);

        AiAssetManifestDto manifest = fetchManifest();

        if (manifest == null || manifest.getAssets() == null) {
            return;
        }

        for (AiAssetDto asset : manifest.getAssets()) {
            syncAsset(asset);
        }

        Files.writeString(
                LOCAL_MANIFEST,
                gson.toJson(manifest),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    private AiAssetManifestDto fetchManifest() throws Exception {
        URL url = new URL(BASE_URL + "/ai/assets/manifest");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(30000);

        int status = conn.getResponseCode();

        InputStream stream = status >= 200 && status < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        String body = new String(stream.readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("===== AI MANIFEST =====");
        System.out.println(body);
        System.out.println("=======================");

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Failed to fetch AI manifest. HTTP " + status + ": " + body);
        }

        return gson.fromJson(body, AiAssetManifestDto.class);
    }

    private void syncAsset(AiAssetDto asset) throws Exception {
        if (asset == null || asset.getKey() == null || asset.getFileName() == null) {
            return;
        }

        if (asset.getSha256() == null || asset.getSha256().isBlank()) {
            System.out.println("[AI SYNC] Skipping missing server asset: " + asset.getKey());
            return;
        }

        Path localFile = AI_ROOT.resolve(asset.getFileName()).normalize();

        if (!localFile.startsWith(AI_ROOT)) {
            throw new SecurityException("Invalid AI asset path: " + asset.getFileName());
        }

        if (Files.exists(localFile)) {
            String localSha = sha256(localFile);

            if (localSha.equalsIgnoreCase(asset.getSha256())) {
                return;
            }
        }

        downloadAsset(asset, localFile);

        String downloadedSha = sha256(localFile);

        if (!downloadedSha.equalsIgnoreCase(asset.getSha256())) {
            Files.deleteIfExists(localFile);
            throw new RuntimeException("SHA-256 mismatch for AI asset: " + asset.getKey());
        }
    }

    private void downloadAsset(AiAssetDto asset, Path localFile) throws Exception {
        URL url = new URL(asset.getDownloadUrl());

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(120000);

        int status = conn.getResponseCode();

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Failed to download AI asset: " + asset.getKey() + ". HTTP " + status);
        }

        Files.copy(
                conn.getInputStream(),
                localFile,
                StandardCopyOption.REPLACE_EXISTING
        );
    }

    public static Path getRequiredAssetPath(String key) throws Exception {
        return getOptionalAssetPath(key)
                .orElseThrow(() -> new IllegalStateException("Required AI asset missing: " + key));
    }

    public static Optional<Path> getOptionalAssetPath(String key) {
        try {
            if (!Files.exists(LOCAL_MANIFEST)) {
                return Optional.empty();
            }

            Gson gson = new Gson();

            String json = Files.readString(LOCAL_MANIFEST, StandardCharsets.UTF_8);

            AiAssetManifestDto manifest = gson.fromJson(json, AiAssetManifestDto.class);

            if (manifest == null || manifest.getAssets() == null) {
                return Optional.empty();
            }

            Optional<AiAssetDto> assetOpt = manifest.getAssets()
                    .stream()
                    .filter(item -> key.equals(item.getKey()))
                    .findFirst();

            if (assetOpt.isEmpty()) {
                return Optional.empty();
            }

            Path path = AI_ROOT.resolve(assetOpt.get().getFileName()).normalize();

            if (!path.startsWith(AI_ROOT) || !Files.exists(path)) {
                return Optional.empty();
            }

            return Optional.of(path);

        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public static Path getAiRoot() {
        return AI_ROOT;
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream is = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;

            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }
}