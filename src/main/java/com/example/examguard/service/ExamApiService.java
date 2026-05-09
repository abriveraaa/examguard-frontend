package com.example.examguard.service;

import com.example.examguard.model.exam.*;
import com.example.examguard.model.exam.take.ExamTakingResponse;
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
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExamApiService {

    public static final String BASE_URL = "http://localhost:8080";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    public void downloadTemplate(File destinationFile) throws Exception {
        URL url = new URL(BASE_URL + "/exams/template/download");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            throw new RuntimeException("Failed to download template. Status: " + responseCode);
        }

        try (InputStream inputStream = conn.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            conn.disconnect();
        }
    }

    public ExamTakingResponse getExamForTaking(Long examId) throws Exception {
        String endpoint = BASE_URL + "/exams/" + examId + "/taking";

        HttpURLConnection conn = null;

        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            if (Session.getSchoolId() == null || Session.getSchoolId().isBlank()) {
                throw new RuntimeException("Session school ID is missing. Please login again.");
            }

            conn.setRequestProperty("X-User-Id", Session.getSchoolId());

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                return gson.fromJson(responseBody, ExamTakingResponse.class);
            }

            throw new RuntimeException(
                    "Failed to fetch exam for taking. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    public void saveAnswer(
            Long attemptId,
            Long questionId,
            Long selectedChoiceId,
            String answerText
    ) throws Exception {

        Map<String, Object> body = new HashMap<>();
        body.put("attemptId", attemptId);
        body.put("questionId", questionId);
        body.put("selectedChoiceId", selectedChoiceId);
        body.put("answerText", answerText);

        String json = gson.toJson(body);

        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE_URL + "/student/exams/answers/save");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setRequestProperty("X-User-Id", Session.getSchoolId());

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            String responseBody = getResponseBody(conn);

            if (!isSuccess(conn.getResponseCode())) {
                throw new RuntimeException(
                        "Failed to save answer. Status: "
                                + conn.getResponseCode()
                                + ". Response: "
                                + responseBody
                );
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public ExamResult submitExam(Long attemptId) throws Exception {

        Map<String, Object> body = new HashMap<>();
        body.put("attemptId", attemptId);

        String json = gson.toJson(body);

        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE_URL + "/student/exams/submit");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);
            conn.setRequestProperty("X-User-Id", Session.getSchoolId());

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            String responseBody = getResponseBody(conn);

            if (!isSuccess(conn.getResponseCode())) {
                throw new RuntimeException(
                        "Failed to submit exam. Status: "
                                + conn.getResponseCode()
                                + ". Response: "
                                + responseBody
                );
            }

            return gson.fromJson(responseBody, ExamResult.class);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public void logViolation(ViolationLogRequest request) throws Exception {
        if (request == null) {
            return;
        }

        String json = gson.toJson(request);

        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE_URL + "/exams/violations");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            String responseBody = getResponseBody(conn);

            if (!isSuccess(conn.getResponseCode())) {
                throw new RuntimeException(
                        "Failed to log violation. Status: "
                                + conn.getResponseCode()
                                + ". Response: "
                                + responseBody
                );
            }

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public UploadExamTemplateResponse previewExamTemplate(File file) throws Exception {
        String boundary = "----ExamGuardBoundary" + System.currentTimeMillis();

        URL url = new URL(BASE_URL + "/exams/template/preview");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName())
                    .append("\"\r\n");
            writer.append("Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet\r\n");
            writer.append("\r\n").flush();

            Files.copy(file.toPath(), output);
            output.flush();

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--").append("\r\n").flush();
        }

        int responseCode = conn.getResponseCode();

        InputStream stream = responseCode >= 200 && responseCode < 300
                ? conn.getInputStream()
                : conn.getErrorStream();

        System.out.println("Preview template response code: " + responseCode);

        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            UploadExamTemplateResponse response =
                    gson.fromJson(reader, UploadExamTemplateResponse.class);

            System.out.println("Preview template response code: " + responseCode);

            if (responseCode < 200 || responseCode >= 300) {
                String message = response != null && response.getMessage() != null
                        ? response.getMessage()
                        : "Upload failed. HTTP Status: " + responseCode;

                throw new RuntimeException(message);
            }

            return response;
        } finally {
            conn.disconnect();
        }


    }

    public ExamResult examDraft(ExamRequest request) throws Exception {
        String json = gson.toJson(request);
        String response = sendPostRequest("/exams/draft", json);

        return gson.fromJson(response, ExamResult.class);
    }

    public ExamResult examPublish(ExamRequest request) throws Exception {
        String json = gson.toJson(request);
        String response = sendPostRequest("/exams/publish", json);

        return gson.fromJson(response, ExamResult.class);
    }

    public List<ExamResponse> fetchExams() throws Exception {
        String endpoint = BASE_URL + "/exams";

        HttpURLConnection conn = null;

        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                Type listType = new TypeToken<List<ExamResponse>>() {}.getType();
                return gson.fromJson(responseBody, listType);
            }

            throw new RuntimeException(
                    "Failed to fetch exams. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public List<ClassOffering> fetchClassOfferings() throws Exception {
        String endpoint = BASE_URL + "/exams/class-offerings";

        HttpURLConnection conn = null;

        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                Type listType = new TypeToken<List<ClassOffering>>() {}.getType();
                return gson.fromJson(responseBody, listType);
            }

            throw new RuntimeException(
                    "Failed to fetch class offerings. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public ExamResponse getExamPreview(Long examId) throws Exception {
        String endpoint = BASE_URL + "/exams/" + examId;

        HttpURLConnection conn = null;

        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                return gson.fromJson(responseBody, ExamResponse.class);
            }

            throw new RuntimeException(
                    "Failed to fetch exam preview. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public ExamResult updateExam(Long examId, ExamRequest request) throws Exception {
        String json = gson.toJson(request);
        String responseBody = sendPutRequestWithBody("/exams/" + examId, json);

        return gson.fromJson(responseBody, ExamResult.class);
    }

    public ImageUploadResponse uploadExamImage(File file) throws Exception {
        String boundary = "----ExamGuardImageBoundary" + System.currentTimeMillis();

        URL url = new URL(BASE_URL + "/exams/images/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        applyAuthHeaders(conn);

        try (OutputStream output = conn.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)) {

            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName())
                    .append("\"\r\n");
            writer.append("Content-Type: image/png\r\n");
            writer.append("\r\n").flush();

            Files.copy(file.toPath(), output);
            output.flush();

            writer.append("\r\n").flush();
            writer.append("--").append(boundary).append("--").append("\r\n").flush();
        }

        String responseBody = getResponseBody(conn);

        if (!isSuccess(conn.getResponseCode())) {
            throw new RuntimeException(responseBody);
        }

        return gson.fromJson(responseBody, ImageUploadResponse.class);
    }

    public ExamResult publishExamById(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/publish");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    public ExamResult cancelExam(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/cancel");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    public ExamResult restoreExam(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/restore");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    private String sendPostRequest(String endpoint, String json) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("X-User-Id", Session.getUsername());
        conn.setRequestProperty("X-Role", Session.getRole());

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

    private String sendPutRequest(String endpointPath) throws Exception {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE_URL + endpointPath);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setConnectTimeout(60000);
            conn.setReadTimeout(60000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                return responseBody;
            }

            throw new RuntimeException(
                    "Request failed. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String sendPutRequestWithBody(String endpointPath, String json) throws Exception {
        HttpURLConnection conn = null;

        try {
            URL url = new URL(BASE_URL + endpointPath);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setConnectTimeout(60000);
            conn.setReadTimeout(60000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                return responseBody;
            }

            throw new RuntimeException(
                    "Request failed. Status: "
                            + conn.getResponseCode()
                            + ". Response: "
                            + responseBody
            );

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void applyAuthHeaders(HttpURLConnection conn) {
        String userId = Session.getUsername();
        String role = Session.getRole();
        String token = Session.getSessionToken();

        if (userId == null || userId.isBlank()) {
            throw new RuntimeException("Session username is missing. Please login again.");
        }

        if (role == null || role.isBlank()) {
            throw new RuntimeException("Session role is missing. Please login again.");
        }

        conn.setRequestProperty("X-User-Id", userId);
        conn.setRequestProperty("X-Role", role);

        if (token != null && !token.isBlank()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
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

    private boolean isSuccess(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }

    private String readResponse(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8)
        )) {
            return reader.lines().collect(Collectors.joining());
        }
    }
}