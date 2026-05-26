package com.example.examguard.service;

import com.example.examguard.config.AppConfig;
import com.example.examguard.model.student.StudentDashboardResponse;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class StudentDashboardApiService {


    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final Gson gson = new Gson();

    public StudentDashboardResponse fetchDashboard() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + "/student/dashboard"))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Session.getSessionToken())
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Failed to load dashboard: " + response.body());
        }

        return gson.fromJson(response.body(), StudentDashboardResponse.class);
    }
}