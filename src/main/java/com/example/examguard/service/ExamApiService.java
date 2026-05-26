package com.example.examguard.service;

import com.example.examguard.config.AppConfig;
import com.example.examguard.model.core.ClassOffering;
import com.example.examguard.model.exam.dto.ExamActivityLogDTO;
import com.example.examguard.model.exam.dto.ExamLeaderboardDTO;
import com.example.examguard.model.exam.dto.ExamStudentViolationDTO;
import com.example.examguard.model.exam.request.EssayReviewRequest;
import com.example.examguard.model.exam.request.ExamRequest;
import com.example.examguard.model.exam.request.ViolationLogRequest;
import com.example.examguard.model.exam.response.ExamResponse;
import com.example.examguard.model.exam.response.ImageUploadResponse;
import com.example.examguard.model.exam.response.UploadExamTemplateResponse;
import com.example.examguard.model.exam.result.ExamResult;
import com.example.examguard.model.exam.request.ExamActivityRequest;
import com.example.examguard.model.exam.take.ExamTakingResponse;
import com.example.examguard.model.faculty.dto.FacultyExamStudentDTO;
import com.example.examguard.model.faculty.dto.FacultySubmissionSummaryDTO;
import com.example.examguard.model.faculty.request.FacultyUpdateAnswerScoreRequest;
import com.example.examguard.model.faculty.response.FacultyAttemptReviewResponse;
import com.example.examguard.model.faculty.response.FacultyExamDetailResponse;
import com.example.examguard.model.faculty.response.SimpleMessageResponse;
import com.example.examguard.model.camera.CreateCameraSessionRequest;
import com.example.examguard.model.camera.CreateCameraSessionResponse;
import com.example.examguard.model.camera.CameraSessionStatusResponse;
import com.example.examguard.model.exam.dto.AnswerReviewTimelineDTO;

import com.google.gson.Gson;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import com.example.examguard.utility.Session;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExamApiService {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

    private HttpURLConnection createConnection(
            String endpoint,
            String method
    ) throws Exception {

        URL url = new URL(AppConfig.BASE_URL + endpoint);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");

        if (Session.getSessionToken() != null) {
            connection.setRequestProperty("Authorization", "Bearer " + Session.getSessionToken());
        }

        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);

        return connection;
    }

    // ===================
    // EXAM ACTIONS
    // ===================

    public void downloadTemplate(File destinationFile) throws Exception {
        URL url = new URL(AppConfig.BASE_URL + "/exams/template/download");
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

    public List<ClassOffering> fetchClassOfferings() throws Exception {
        String endpoint = AppConfig.BASE_URL + "/exams/class-offerings";

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

    public ImageUploadResponse uploadExamImage(File file) throws Exception {
        String boundary = "----ExamGuardImageBoundary" + System.currentTimeMillis();

        URL url = new URL(AppConfig.BASE_URL + "/exams/images/upload");
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

    public ExamResult cancelExam(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/cancel");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    public ExamResult restoreExam(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/restore");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    public ExamResponse getExamPreview(Long examId) throws Exception {
        String endpoint = AppConfig.BASE_URL + "/exams/" + examId ;

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

    public UploadExamTemplateResponse previewExamTemplate(File file) throws Exception {
        String boundary = "----ExamGuardBoundary" + System.currentTimeMillis();

        URL url = new URL(AppConfig.BASE_URL + "/exams/template/preview");
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

    public ExamResult publishExamById(Long examId) throws Exception {
        String responseBody = sendPutRequest("/exams/" + examId + "/publish");
        return gson.fromJson(responseBody, ExamResult.class);
    }

    public List<ExamResponse> fetchExams() throws Exception {
        String endpoint = AppConfig.BASE_URL + "/exams";

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

    public SimpleMessageResponse applyViolationDecision(
            Long answerId,
            Long questionId,
            Long attemptId,
            String decision,
            BigDecimal deduction,
            String feedback
    ) throws Exception {

        Map<String, Object> body = new HashMap<>();
        body.put("answerId", answerId);
        body.put("questionId", questionId);
        body.put("attemptId", attemptId);
        body.put("decision", decision);
        body.put("deduction", deduction);
        body.put("feedback", feedback);

        return post(
                "/exams/review/violations/decision",
                body,
                SimpleMessageResponse.class
        );
    }


    // ===================
    // EXAM TAKING
    // ===================

    public ExamTakingResponse beginExam(Long examId) throws Exception {
        String endpoint = AppConfig.BASE_URL + "/exams/" + examId + "/begin";

        HttpURLConnection conn = null;

        try {
            URL url = new URL(endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Content-Type", "application/json");

            applyAuthHeaders(conn);

            conn.setDoOutput(true);
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);

            String responseBody = getResponseBody(conn);

            if (isSuccess(conn.getResponseCode())) {
                return gson.fromJson(responseBody, ExamTakingResponse.class);
            }

            throw new RuntimeException(
                    "Failed to begin exam. Status: "
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

    public CreateCameraSessionResponse createCameraSession(
            Long attemptId,
            Long examId,
            String studentId
    ) {
        try {
            URL url = new URL(AppConfig.BASE_URL + "/exam-camera/sessions");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            CreateCameraSessionRequest request =
                    new CreateCameraSessionRequest(attemptId, examId, studentId);

            String json = gson.toJson(request);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }

            int statusCode = conn.getResponseCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to create camera session. HTTP " + statusCode);
            }

            String responseBody = new String(
                    conn.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            return gson.fromJson(responseBody, CreateCameraSessionResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Camera session creation failed: " + e.getMessage(), e);
        }
    }

    public CameraSessionStatusResponse getCameraSessionStatus(String token) {
        try {
            URL url = new URL(AppConfig.BASE_URL + "/exam-camera/sessions/" + token);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            int statusCode = conn.getResponseCode();

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to get camera session status. HTTP " + statusCode);
            }

            String responseBody = new String(
                    conn.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            return gson.fromJson(responseBody, CameraSessionStatusResponse.class);

        } catch (Exception e) {
            throw new RuntimeException("Camera session status check failed: " + e.getMessage(), e);
        }
    }

    public void endPhoneCameraSession(String token) {
        try {
            URL url = new URL(
                    AppConfig.BASE_URL + "/exam-camera/sessions/" + token + "/end"
            );

            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            int statusCode = conn.getResponseCode();

            if (statusCode == 400 || statusCode == 404 || statusCode == 409) {
                System.out.println("Phone camera session already ended or unavailable. HTTP " + statusCode);
                return;
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "Failed to end phone camera session. HTTP "
                                + statusCode
                );
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getCameraPreviewFrame(String token) {
        try {
            URL url = new URL(AppConfig.BASE_URL + "/exam-camera/sessions/" + token + "/preview-frame");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "image/jpeg");

            int statusCode = conn.getResponseCode();

            if (statusCode == 404) {
                return null;
            }

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to get camera preview frame. HTTP " + statusCode);
            }

            return conn.getInputStream().readAllBytes();

        } catch (Exception e) {
            return null;
        }
    }

    public long checkBackendLatency() throws Exception {
        long start = System.currentTimeMillis();

        HttpURLConnection conn = null;

        try {
            URL url = new URL(AppConfig.BASE_URL + "/system/health");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Accept", "application/json");

            int status = conn.getResponseCode();

            if (status < 200 || status >= 300) {
                throw new RuntimeException("Backend health check failed. Status: " + status);
            }

            return System.currentTimeMillis() - start;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public ExamTakingResponse getExamForTaking(Long examId) throws Exception {
        String endpoint = AppConfig.BASE_URL + "/exams/" + examId + "/taking";

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

    public ImageUploadResponse uploadViolationEvidence(File file) throws Exception {
        String boundary = "----ExamGuardEvidenceBoundary" + System.currentTimeMillis();

        URL url = new URL(AppConfig.BASE_URL + "/exams/evidence/upload");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        applyAuthHeaders(conn);

        try (
                OutputStream output = conn.getOutputStream();
                PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8), true)
        ) {
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                    .append(file.getName())
                    .append("\"\r\n");
            writer.append("Content-Type: image/jpeg\r\n");
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
            URL url = new URL(AppConfig.BASE_URL + "/student/exams/answers/save");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setRequestProperty("Authorization", Session.getSessionToken());

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
            URL url = new URL(AppConfig.BASE_URL + "/student/exams/submit");
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);
            conn.setRequestProperty("Authorization", Session.getSessionToken());

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


    // ===================
    // EXAM WORKSPACE
    // ===================

    public FacultyAttemptReviewResponse getStudentAttemptReview(
            Long examId,
            String studentId
    ) throws Exception {
        return get(
                "/exams/" + examId + "/students/" + studentId + "/review",
                FacultyAttemptReviewResponse.class
        );
    }

    public List<ExamActivityLogDTO> getExamActivityLogs(Long examId) throws Exception {
        ExamActivityLogDTO[] result = get(
                "/exams/" + examId + "/activity",
                ExamActivityLogDTO[].class
        );

        return Arrays.asList(result);
    }

    public SimpleMessageResponse updateAnswerScore(
            Long answerId,
            double pointsAwarded
    ) throws Exception {

        FacultyUpdateAnswerScoreRequest request =
                new FacultyUpdateAnswerScoreRequest(
                        java.math.BigDecimal.valueOf(pointsAwarded)
                );

        return post(
                "/answers/" + answerId + "/score",
                request,
                SimpleMessageResponse.class
        );
    }

    public List<FacultyExamStudentDTO> getExamStudents(Long examId) throws Exception {
        FacultyExamStudentDTO[] result = get(
                "/exams/" + examId + "/students",
                FacultyExamStudentDTO[].class
        );

        return Arrays.asList(result);
    }

    public List<FacultySubmissionSummaryDTO> getExamSubmissions(Long examId) throws Exception {
        FacultySubmissionSummaryDTO[] result = get(
                "/exams/" + examId + "/submissions",
                FacultySubmissionSummaryDTO[].class
        );

        return Arrays.asList(result);
    }

    public List<ExamStudentViolationDTO> getExamViolations(Long examId) throws Exception {
        ExamStudentViolationDTO[] result = get(
                "/exams/" + examId + "/violations",
                ExamStudentViolationDTO[].class
        );

        return Arrays.asList(result);
    }

    public List<ExamLeaderboardDTO> getExamLeaderboard(Long examId) throws Exception {
        ExamLeaderboardDTO[] result = get(
                "/exams/" + examId + "/leaderboard",
                ExamLeaderboardDTO[].class
        );

        return Arrays.asList(result);
    }

    public SimpleMessageResponse markAttemptReviewed(Long attemptId) throws Exception {
        return post(
                "/exams/attempts/" + attemptId + "/mark-reviewed",
                SimpleMessageResponse.class
        );
    }

    public SimpleMessageResponse releaseResults(Long examId) throws Exception {

        String responseBody =
                sendPutRequest("/exams/" + examId + "/results/release");

        return gson.fromJson(
                responseBody,
                SimpleMessageResponse.class
        );
    }

    public ExamResult saveEssayReview(EssayReviewRequest request) throws Exception {
        return post(
                "/exams/answers/essay-review",
                request,
                ExamResult.class
        );
    }

    public FacultyExamDetailResponse getExamDetail(Long examId) throws Exception {
        return get(
                "/exams/" + examId + "/workspace",
                FacultyExamDetailResponse.class
        );
    }

    public List<AnswerReviewTimelineDTO> getAnswerReviewTimeline(
            Long answerId
    ) throws Exception {

        AnswerReviewTimelineDTO[] result = get(
                "/exams/review/answers/" + answerId + "/timeline",
                AnswerReviewTimelineDTO[].class
        );

        return Arrays.asList(result);
    }

    public File downloadExamPortfolioReport(
            Long examId,
            String mode
    ) throws Exception {

        String normalizedMode = mode == null || mode.isBlank()
                ? "MERGED"
                : mode.toUpperCase();

        return downloadFile(
                "/exams/" + examId + "/portfolio?mode=" + normalizedMode
        );
    }

    // ===================
    // LOGS
    // ===================

    public void logViolation(ViolationLogRequest request) throws Exception {
        if (request == null) {
            return;
        }

        String json = gson.toJson(request);

        HttpURLConnection conn = null;

        try {
            URL url = new URL(AppConfig.BASE_URL + "/exams/violations");
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

    public void logActivity(ExamActivityRequest request) throws Exception {

        if (request == null) {
            return;
        }

        String json = gson.toJson(request);

        HttpURLConnection conn = null;

        try {
            URL url = new URL(AppConfig.BASE_URL + "/student/exams/activity");

            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            applyAuthHeaders(conn);

            conn.setRequestProperty("Authorization", Session.getSessionToken());

            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input);
            }

            String responseBody = getResponseBody(conn);

            if (!isSuccess(conn.getResponseCode())) {
                throw new RuntimeException(
                        "Failed to log activity. Status: "
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

    // ===================
    // HEADERS AND HELPERS
    // ===================

    private String sendPostRequest(String endpoint, String json) throws Exception {
        URL url = new URL(AppConfig.BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setConnectTimeout(60000);
        conn.setReadTimeout(60000);

        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", Session.getSessionToken());

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
            URL url = new URL(AppConfig.BASE_URL + endpointPath);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", Session.getSessionToken());

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
            URL url = new URL(AppConfig.BASE_URL + endpointPath);
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
        String token = Session.getSessionToken();

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


    private <T> T get(String endpoint, Class<T> responseType) throws Exception {
        HttpURLConnection connection = createConnection(endpoint, "GET");

        int status = connection.getResponseCode();

        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();

        try (InputStreamReader reader = new InputStreamReader(stream)) {
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Request failed: " + status);
            }

            return gson.fromJson(reader, responseType);
        }
    }

    private <T> List<T> getList(String endpoint, Class<T[]> responseType) throws Exception {
        T[] result = get(endpoint, responseType);
        return Arrays.asList(result);
    }

    private <T> T post(String endpoint, Class<T> responseType) throws Exception {
        HttpURLConnection connection = createConnection(endpoint, "POST");
        connection.setDoOutput(true);

        int status = connection.getResponseCode();

        InputStream stream = status >= 200 && status < 300
                ? connection.getInputStream()
                : connection.getErrorStream();

        try (InputStreamReader reader = new InputStreamReader(stream)) {
            if (status < 200 || status >= 300) {
                throw new RuntimeException("Request failed: " + status);
            }

            return gson.fromJson(reader, responseType);
        }
    }

    private <T> T post(
            String endpoint,
            Object requestBody,
            Class<T> responseType
    ) throws Exception {

        HttpURLConnection connection =
                createConnection(endpoint, "POST");

        connection.setDoOutput(true);

        try (OutputStreamWriter writer =
                     new OutputStreamWriter(connection.getOutputStream())) {

            gson.toJson(requestBody, writer);
            writer.flush();
        }

        int status = connection.getResponseCode();

        InputStream stream =
                status >= 200 && status < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

        try (InputStreamReader reader =
                     new InputStreamReader(stream)) {

            if (status < 200 || status >= 300) {
                throw new RuntimeException(
                        "Request failed: " + status
                );
            }

            return gson.fromJson(reader, responseType);
        }
    }

    private File downloadFile(String endpoint) throws Exception {

        HttpURLConnection conn = null;

        try {
            URL url = new URL(AppConfig.BASE_URL + endpoint);
            conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/pdf, application/zip, application/octet-stream");

            applyAuthHeaders(conn);

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);

            int status = conn.getResponseCode();

            if (status < 200 || status >= 300) {
                String responseBody = getResponseBody(conn);
                throw new RuntimeException("Failed to download report. Status: " + status + ". Response: " + responseBody);
            }

            String fileName = "report.pdf";
            String disposition = conn.getHeaderField("Content-Disposition");

            if (disposition != null && disposition.contains("filename=")) {
                fileName = disposition
                        .replaceFirst("(?i)^.*filename=\"?", "")
                        .replaceAll("\"", "")
                        .trim();
            }

            File destination = new File(System.getProperty("user.home") + "/Downloads", fileName);

            try (InputStream inputStream = conn.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(destination)) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            return destination;

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }


}