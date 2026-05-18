package com.example.examguard.service;

import com.example.examguard.model.admin.monitoring.AdminMonitoringLogsResponse;
import com.example.examguard.model.admin.monitoring.MonitoringOverviewResponse;
import com.example.examguard.model.core.request.ReactivateUserRequest;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AdminApiService {

    private static final String BASE_URL = "http://localhost:8080";
    private final HttpClient httpClient;
    private final Gson gson;

    public AdminApiService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    public MonitoringOverviewResponse getOverview(Map<String, Object> body) throws Exception {
        String response = postJson("/admin/monitoring/overview", body);
        return gson.fromJson(response, MonitoringOverviewResponse.class);
    }

    public AdminMonitoringLogsResponse getLogs(Map<String, Object> body) throws Exception {
        String response = postJson("/admin/monitoring/logs", body);
        return gson.fromJson(response, AdminMonitoringLogsResponse.class);
    }

    public String createAdminProfile(String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/profiles")) // ✅ FIXED
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error creating admin profile.";
        }
    }

    public String updateAdminProfile(String employeeId, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/profiles/" + employeeId))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error updating admin profile.";
        }
    }

    public String deactivateAdmin(String employeeId, String currentAdminId, String reason) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("reason", reason);
            payload.put("currentAdminId", currentAdminId);

            String jsonBody = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/profiles/" + employeeId + "/deactivate"))
                    .header("Content-Type", "application/json")
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error deactivating admin.";
        }
    }

    public String reactivateAdmin(String employeeId, String reason) {
        try {
            Map<String, String> payload = new HashMap<>();
            payload.put("reason", reason);

            String jsonBody = new Gson().toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/profiles/" + employeeId + "/reactivate"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Session.getSessionToken())
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error reactivating admin.";
        }
    }

    public String refreshAndSyncRegistrarAccess() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/cache/refresh-and-sync"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Session.getSessionToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error syncing registrar access.";
        }
    }

    public String getLastSuccessfulSync() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/registrar-sync/last-sync"))
                    .header("Authorization", "Bearer " + Session.getSessionToken())
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getEligibleReactivationUsers() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/cache/eligible-reactivation"))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? response.body()
                    : "[]";

        } catch (Exception e) {
            e.printStackTrace();
            return "[]";
        }
    }

    public String reactivateSingleUser(String schoolId, String role, String justification) {
        try {
            ReactivateUserRequest payload =
                    new ReactivateUserRequest(schoolId, role, justification);

            String jsonBody = gson.toJson(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/admin/registrar-sync/reactivate-user"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + Session.getSessionToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.body();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error reactivating user.";
        }
    }

    public String getUserDetails(String schoolId, String role) {
        try {
            String url = BASE_URL + "/admin/users/details?schoolId=" + schoolId + "&role=" + role;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            return response.statusCode() >= 200 && response.statusCode() < 300
                    ? response.body()
                    : null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String postJson(String endpoint, Object body) throws Exception {
        URL url = new URL(BASE_URL + endpoint);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);

        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-User-Id", Session.getSchoolId());
        connection.setRequestProperty("X-Role", "ADMIN");

        String json = gson.toJson(body);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }

        int status = connection.getResponseCode();

        byte[] responseBytes = status >= 200 && status < 300
                ? connection.getInputStream().readAllBytes()
                : connection.getErrorStream().readAllBytes();

        String response = new String(responseBytes, StandardCharsets.UTF_8);

        if (status < 200 || status >= 300) {
            throw new RuntimeException("Monitoring API failed: HTTP " + status + " - " + response);
        }

        return response;
    }

}