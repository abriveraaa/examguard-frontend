package com.example.examguard.cache;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalImageCacheService {

    private final Path avatarDir;

    public LocalImageCacheService() {
        this.avatarDir = Paths.get(
                System.getProperty("user.home"),
                ".examguard",
                "cache",
                "avatars"
        );

        try {
            Files.createDirectories(avatarDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Path getAvatarPath(String schoolId) {
        return avatarDir.resolve(schoolId + ".jpg");
    }

    public boolean hasAvatar(String schoolId) {
        return Files.exists(getAvatarPath(schoolId));
    }

    public String getAvatarUri(String schoolId) {
        return getAvatarPath(schoolId).toUri().toString();
    }

    public void saveAvatarFromUrl(String schoolId, String imageUrl) {
        try {
            if (schoolId == null || schoolId.isBlank()) return;
            if (imageUrl == null || imageUrl.isBlank()) return;

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();

            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return;
            }

            Files.copy(
                    response.body(),
                    getAvatarPath(schoolId),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteAvatar(String schoolId) {
        try {
            Files.deleteIfExists(getAvatarPath(schoolId));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}