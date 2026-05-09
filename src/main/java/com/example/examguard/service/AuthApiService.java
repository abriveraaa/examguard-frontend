package com.example.examguard.service;

import com.example.examguard.model.enums.UserType;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AuthApiService {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient;

    public AuthApiService() {
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
                .uri(URI.create(BASE_URL + "/auth/login"))
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
                .uri(URI.create(BASE_URL + "/auth/logout"))
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
                .uri(URI.create(BASE_URL + "/auth/activate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("ACTIVATE STATUS: " + response.statusCode());
        System.out.println("ACTIVATE BODY: " + response.body());

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
                .uri(URI.create(BASE_URL + "/auth/change-password"))
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
                .uri(URI.create(BASE_URL + "/auth/forgot-password"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        System.out.println("FORGOT PASSWORD STATUS: " + response.statusCode());
        System.out.println("FORGOT PASSWORD BODY: " + response.body());

        return response.body();
    }

    public String getUsersByType(UserType type) throws IOException, InterruptedException {
        return sendGet("/admin/users/" + type.getPath());
    }

    private String sendGet(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
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

    private String escape(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}