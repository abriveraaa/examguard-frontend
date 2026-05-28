package com.example.examguard.service;

import com.example.examguard.config.AppConfig;
import com.example.examguard.model.enums.UserType;
import com.example.examguard.model.profile.ProfileResponseDTO;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

public class AuthApiService {

    private final HttpClient httpClient;
    private final Gson gson;

    public AuthApiService() {
        this.gson = new GsonBuilder().create();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String login(String username, String password) throws IOException, InterruptedException {
        String json = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\"}",
                escape(username),
                escape(password)
        );

        return postJsonNoAuth("/auth/login", json);
    }

    public void logout(String token) throws IOException, InterruptedException {
        String json = String.format("{\"sessionToken\":\"%s\"}", escape(token));
        postJsonNoAuth("/auth/logout", json);
    }

    public String activateAccount(String schoolId, String email, String birthday)
            throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"email\":\"%s\",\"birthday\":\"%s\"}",
                escape(schoolId),
                escape(email),
                escape(birthday)
        );

        return postJsonNoAuth("/auth/activate", json);
    }

    public String changePassword(
            String schoolId,
            String currentPassword,
            String newPassword,
            String confirmPassword
    ) throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"currentPassword\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}",
                escape(schoolId),
                escape(currentPassword),
                escape(newPassword),
                escape(confirmPassword)
        );

        return postJsonNoAuth("/auth/change-password", json);
    }

    public String forgotPassword(String schoolId, String email, String birthday)
            throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"email\":\"%s\",\"birthday\":\"%s\"}",
                escape(schoolId),
                escape(email),
                escape(birthday)
        );

        return postJsonNoAuth("/auth/forgot-password", json);
    }

    public ProfileResponseDTO getProfile() throws IOException, InterruptedException {
        String json = getAuth("/profile/me");
        return gson.fromJson(json, ProfileResponseDTO.class);
    }

    public String getUsersByType(UserType type) throws IOException, InterruptedException {
        return getAuth("/admin/users/" + type.getPath());
    }

    public String uploadProfilePhoto(String token, File file)
            throws IOException, InterruptedException {

        String boundary = "----ExamGuardBoundary" + System.currentTimeMillis();

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "image/png";
        }

        File compressedFile = compressProfileImage(file);
        byte[] fileBytes = Files.readAllBytes(compressedFile.toPath());

        String partHeader =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n\r\n";

        String partFooter = "\r\n--" + boundary + "--\r\n";

        byte[] body = concatBytes(
                partHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                fileBytes,
                partFooter.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/profile/upload-photo"))
                .header("Authorization", bearer(token))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return sendExpectSuccess(request, "Upload profile photo failed");
    }

    private String getAuth(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + path))
                .header("Content-Type", "application/json")
                .header("Authorization", bearer(Session.getSessionToken()))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return sendExpectSuccess(request, "GET failed: " + path);
    }

    private String postJsonNoAuth(String path, String json)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return sendExpectSuccess(request, "POST failed: " + path);
    }

    private String sendExpectSuccess(HttpRequest request, String errorPrefix)
            throws IOException, InterruptedException {

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        int status = response.statusCode();

        if (status < 200 || status >= 300) {
            throw new IOException(errorPrefix + ": HTTP " + status + " - " + response.body());
        }

        return response.body();
    }

    private String bearer(String token) {
        if (token == null || token.isBlank()) {
            return "Bearer ";
        }

        return token.startsWith("Bearer ")
                ? token
                : "Bearer " + token;
    }

    private byte[] concatBytes(byte[]... arrays) {
        int totalLength = 0;

        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];
        int position = 0;

        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, position, array.length);
            position += array.length;
        }

        return result;
    }

    private File compressProfileImage(File originalFile) throws IOException {
        BufferedImage originalImage = ImageIO.read(originalFile);

        if (originalImage == null) {
            throw new IOException("Selected file is not a valid image.");
        }

        int targetWidth = 300;
        int targetHeight = 300;

        BufferedImage resizedImage = new BufferedImage(
                targetWidth,
                targetHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );
        graphics.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();

        File tempFile = File.createTempFile("profile-", ".jpg");

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        try (ImageOutputStream stream = ImageIO.createImageOutputStream(tempFile)) {
            writer.setOutput(stream);

            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.75f);

            writer.write(null, new IIOImage(resizedImage, null, null), param);
        } finally {
            writer.dispose();
        }

        return tempFile;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}