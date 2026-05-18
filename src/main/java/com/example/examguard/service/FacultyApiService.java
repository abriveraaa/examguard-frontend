package com.example.examguard.service;

import com.example.examguard.model.exam.dto.ExamActivityLogDTO;
import com.example.examguard.model.exam.dto.ExamLeaderboardDTO;
import com.example.examguard.model.exam.dto.ExamStudentViolationDTO;
import com.example.examguard.model.exam.request.EssayReviewRequest;
import com.example.examguard.model.exam.result.ExamResult;
import com.example.examguard.model.faculty.dto.*;
import com.example.examguard.model.faculty.request.FacultyUpdateAnswerScoreRequest;
import com.example.examguard.model.faculty.response.*;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class FacultyApiService {

    public static final String BASE_URL = "http://localhost:8080";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(
                    OffsetDateTime.class,
                    new OffsetDateTimeAdapter()
            )
            .create();


    public FacultyDashboardResponse getDashboard() throws Exception {
        return get("/faculty/dashboard", FacultyDashboardResponse.class);
    }

    public FacultyProfileDTO getDashboardProfile() throws Exception {
        return get("/faculty/dashboard/profile", FacultyProfileDTO.class);
    }

    public FacultyDashboardStatsDTO getDashboardStats() throws Exception {
        return get("/faculty/dashboard/stats", FacultyDashboardStatsDTO.class);
    }

    public List<FacultyExamSummaryDTO> getDashboardActiveExams() throws Exception {
        return getList(
                "/faculty/dashboard/active-exams",
                FacultyExamSummaryDTO[].class
        );
    }

    public List<FacultyViolationReviewDTO> getDashboardNeedsReview() throws Exception {
        return getList(
                "/faculty/dashboard/needs-review",
                FacultyViolationReviewDTO[].class
        );
    }

    public List<FacultyClassDTO> getDashboardClasses() throws Exception {
        return getList(
                "/faculty/dashboard/classes",
                FacultyClassDTO[].class
        );
    }

    public List<FacultySubmissionSummaryDTO> getDashboardRecentSubmissions() throws Exception {
        return getList(
                "/faculty/dashboard/recent-submissions",
                FacultySubmissionSummaryDTO[].class
        );
    }

    public List<FacultyClassDTO> getClasses() throws Exception {
        FacultyClassDTO[] result = get(
                "/faculty/classes",
                FacultyClassDTO[].class
        );

        return Arrays.asList(result);
    }

    public List<FacultyExamSummaryDTO> getExams() throws Exception {
        FacultyExamSummaryDTO[] result = get(
                "/faculty/exams",
                FacultyExamSummaryDTO[].class
        );

        return Arrays.asList(result);
    }

    private <T> T get(String endpoint, Class<T> responseType) throws Exception {
        HttpURLConnection connection = createConnection(endpoint, "GET");

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

    private HttpURLConnection createConnection(
            String endpoint,
            String method
    ) throws Exception {

        URL url = new URL(BASE_URL + endpoint);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-User-Id", Session.getSchoolId());
        connection.setRequestProperty("X-Role", Session.getRole());

        if (Session.getSessionToken() != null) {
            connection.setRequestProperty(
                    "Authorization",
                    "Bearer " + Session.getSessionToken()
            );
        }

        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);

        return connection;
    }
}
