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

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );



        return response.body();
    }

    public void logout(String token) throws IOException, InterruptedException {

        String json = String.format("{\"sessionToken\":\"%s\"}", token);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/auth/logout"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public String activateAccount(String schoolId, String email, String birthday) throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"email\":\"%s\",\"birthday\":\"%s\"}",
                escape(schoolId),
                escape(email),
                escape(birthday)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/auth/activate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    public String changePassword(String schoolId, String currentPassword, String newPassword, String confirmPassword) throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"currentPassword\":\"%s\",\"newPassword\":\"%s\",\"confirmPassword\":\"%s\"}",
                escape(schoolId),
                escape(currentPassword),
                escape(newPassword),
                escape(confirmPassword)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/auth/change-password"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    public String forgotPassword(String schoolId, String email, String birthday) throws IOException, InterruptedException {

        String json = String.format(
                "{\"schoolId\":\"%s\",\"email\":\"%s\",\"birthday\":\"%s\"}",
                escape(schoolId),
                escape(email),
                escape(birthday)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/auth/forgot-password"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.body();
    }

    public ProfileResponseDTO getProfile() throws IOException, InterruptedException {
        String json = sendGetWithAuth(Session.getSessionToken());

        return gson.fromJson(json, ProfileResponseDTO.class);
    }

    public String uploadProfilePhoto(String token, File file)
            throws IOException, InterruptedException {

        String boundary = "----ExamGuardBoundary" + System.currentTimeMillis();

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "image/png";
        }

        String fileName = file.getName();

        File compressedFile = compressProfileImage(file);

        byte[] fileBytes = Files.readAllBytes(compressedFile.toPath());

        String partHeader =
                "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                        "Content-Type: " + contentType + "\r\n\r\n";

        String partFooter =
                "\r\n--" + boundary + "--\r\n";

        byte[] body = concatBytes(
                partHeader.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                fileBytes,
                partFooter.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/profile/upload-photo"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(
                    "Upload failed: " + response.statusCode() + " - " + response.body()
            );
        }

        return response.body();
    }

    public String getUsersByType(UserType type) throws IOException, InterruptedException {
        return sendGet("/admin/users/" + type.getPath());
    }

    private String sendGetWithAuth(String token) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/profile/me"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Request failed: " + response.statusCode() + " - " + response.body());
        }

        return response.body();
    }

    private byte[] concatBytes(byte[]... arrays) {
        int totalLength = 0;

        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        byte[] result = new byte[totalLength];

        int position = 0;

        for (byte[] array : arrays) {
            System.arraycopy(
                    array,
                    0,
                    result,
                    position,
                    array.length
            );

            position += array.length;
        }

        return result;
    }

    private String sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + path))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            throw new IOException("Request failed: " + path);
        }

        return response.body();
    }

    private File compressProfileImage(File originalFile) throws IOException {

        BufferedImage originalImage =
                ImageIO.read(originalFile);

        int targetWidth = 300;
        int targetHeight = 300;

        BufferedImage resizedImage =
                new BufferedImage(
                        targetWidth,
                        targetHeight,
                        BufferedImage.TYPE_INT_RGB
                );

        Graphics2D graphics =
                resizedImage.createGraphics();

        graphics.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        graphics.drawImage(
                originalImage,
                0,
                0,
                targetWidth,
                targetHeight,
                null
        );

        graphics.dispose();

        File tempFile =
                File.createTempFile(
                        "profile-",
                        ".jpg"
                );

        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();

        ImageOutputStream stream = ImageIO.createImageOutputStream(tempFile);

        writer.setOutput(stream);

        ImageWriteParam param = writer.getDefaultWriteParam();

        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);

        param.setCompressionQuality(0.75f);

        writer.write(
                null,
                new IIOImage(
                        resizedImage,
                        null,
                        null
                ),
                param
        );

        stream.close();
        writer.dispose();

        return tempFile;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}