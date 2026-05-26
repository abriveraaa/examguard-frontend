package com.example.examguard.service;

import com.example.examguard.config.AppConfig;
import com.example.examguard.model.student.StudentDashboardResponse;
import com.example.examguard.model.student.StudentExamResponse;
import com.example.examguard.model.student.StudentExamResultResponse;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class StudentApiService {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    // ========
    // REQUEST
    // ========

    public StudentDashboardResponse fetchDashboard() throws Exception {

        String response = sendGetRequest("/student/dashboard");

        return gson.fromJson(response, StudentDashboardResponse.class);
    }

    public List<StudentExamResponse> getStudentExams()
            throws Exception {

        String response = sendGetRequest("/student/exams");

        Type listType = new TypeToken<List<StudentExamResponse>>() {}.getType();

        return gson.fromJson(response, listType);
    }

    public StudentExamResultResponse getStudentExamResult(Long examId) throws Exception {
        String responseBody = sendGetRequest("/student/exams/" + examId + "/results");

        return gson.fromJson(
                responseBody,
                StudentExamResultResponse.class
        );
    }

    public void markResultViewed(Long examId) throws Exception {
        sendPostRequest("/student/dashboard/results/" + examId + "/view", "");
    }

    public void markViolationViewed(Long examId) throws Exception {
        sendPostRequest("/student/dashboard/violations/" + examId + "/view", "");
    }

    public FileDownloadResponse downloadStudentAnswerSheetReport(Long examId) throws Exception {
        return sendGetForFileDownload(
                "/student/exams/" + examId + "/answer-sheet-report"
        );
    }

    // =========
    // HELPER
    // =========

    private String sendGetRequest(String endpoint) throws Exception {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(AppConfig.BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + Session.getSessionToken())
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("GET " + endpoint + " failed: " + response.body());
        }

        return response.body();
    }

    private String sendPostRequest(String endpoint, String json) throws Exception {
        URL url = new URL(AppConfig.BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + Session.getSessionToken());

        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = json.getBytes(StandardCharsets.UTF_8);
            os.write(input);
        }

        String responseBody = getResponseBody(conn);
        int responseCode = conn.getResponseCode();

        if (responseCode < 200 || responseCode >= 300) {
            throw new RuntimeException(responseBody);
        }

        conn.disconnect();
        return responseBody;
    }

    private String getResponseBody(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();

        InputStream stream = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private FileDownloadResponse sendGetForFileDownload(String endpoint) throws Exception {
        URL url = new URL(AppConfig.BASE_URL + endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-User-Id", Session.getSchoolId());
        connection.setRequestProperty("X-Role", Session.getRole());
        connection.setRequestProperty("Authorization", "Bearer " + Session.getSessionToken());

        int status = connection.getResponseCode();

        InputStream inputStream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        byte[] response = inputStream.readAllBytes();

        if (status < 200 || status >= 300) {
            throw new RuntimeException(new String(response));
        }

        String filename = extractFilenameFromContentDisposition(
                connection.getHeaderField("Content-Disposition")
        );

        connection.disconnect();

        return new FileDownloadResponse(response, filename);
    }

    public static class FileDownloadResponse {

        private final byte[] data;
        private final String filename;

        public FileDownloadResponse(byte[] data, String filename) {
            this.data = data;
            this.filename = filename;
        }

        public byte[] getData() {
            return data;
        }

        public String getFilename() {
            return filename;
        }
    }

    private String extractFilenameFromContentDisposition(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return "student-answer-sheet-report.pdf";
        }

        String marker = "filename=\"";

        int start = contentDisposition.indexOf(marker);

        if (start >= 0) {
            start += marker.length();

            int end = contentDisposition.indexOf("\"", start);

            if (end > start) {
                return contentDisposition.substring(start, end);
            }
        }

        marker = "filename=";
        start = contentDisposition.indexOf(marker);

        if (start >= 0) {
            return contentDisposition
                    .substring(start + marker.length())
                    .replace("\"", "")
                    .trim();
        }

        return "student-answer-sheet-report.pdf";
    }

}