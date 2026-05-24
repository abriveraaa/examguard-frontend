package com.example.examguard.service;

import com.example.examguard.model.faculty.dto.*;
import com.example.examguard.model.faculty.dto.reports.*;
import com.example.examguard.model.faculty.dto.students.FacultyAcademicPeriodDTO;
import com.example.examguard.model.faculty.dto.students.FacultyStudentDTO;
import com.example.examguard.model.faculty.response.*;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public List<FacultyExamSummaryDTO> getExams() throws Exception {
        FacultyExamSummaryDTO[] result = get(
                "/faculty/exams",
                FacultyExamSummaryDTO[].class
        );

        return Arrays.asList(result);
    }

    public List<FacultyStudentDTO> getStudentsByPeriod(
            String academicYear,
            String term
    ) throws Exception {

        return getList(
                "/faculty/students/period" +
                        "?academicYear=" + encode(academicYear) +
                        "&term=" + encode(term),
                FacultyStudentDTO[].class
        );
    }

    public List<FacultyAcademicPeriodDTO> getStudentAcademicPeriods()
            throws Exception {

        return getList(
                "/faculty/students/academic-periods",
                FacultyAcademicPeriodDTO[].class
        );
    }

    public FacultyReportSummaryDTO getFacultyReportSummary(
            FacultyReportFilter filter
    ) throws Exception {

        return get(
                "/faculty/reports/summary" + buildReportsQuery(filter),
                FacultyReportSummaryDTO.class
        );
    }

    public List<ExamParticipationDTO> getFacultyReportParticipation(
            FacultyReportFilter filter
    ) throws Exception {

        return getList(
                "/faculty/reports/participation" + buildReportsQuery(filter),
                ExamParticipationDTO[].class
        );
    }

    public List<SubmissionStatusDTO> getFacultyReportSubmissionStatus(
            FacultyReportFilter filter
    ) throws Exception {

        return getList(
                "/faculty/reports/submission-status" + buildReportsQuery(filter),
                SubmissionStatusDTO[].class
        );
    }

    public List<ViolationTypeDTO> getFacultyReportViolations(
            FacultyReportFilter filter
    ) throws Exception {

        return getList(
                "/faculty/reports/violations" + buildReportsQuery(filter),
                ViolationTypeDTO[].class
        );
    }

    public List<ReportExamOptionDTO> getFacultyReportExamOptions(
            FacultyReportFilter filter
    ) throws Exception {

        return getList(
                "/faculty/reports/exams" + buildReportsQuery(filter),
                ReportExamOptionDTO[].class
        );
    }

    public byte[] exportStudentsRoster(
            String academicYear,
            String term,
            String courseCode,
            String programCode,
            String yearLevel,
            String sectionName,
            String type
    ) throws IOException {

        StringBuilder url = new StringBuilder(BASE_URL +
                                "/faculty/students/export" +
                                "?academicYear=" + encode(academicYear) +
                                "&term=" + encode(term) +
                                "&type=" + type
        );

        if (courseCode != null && !courseCode.isBlank()) {
            url.append("&courseCode=").append(encode(courseCode));
        }

        if (programCode != null && !programCode.isBlank()) {
            url.append("&programCode=").append(encode(programCode));
        }

        if (yearLevel != null) {
            url.append("&yearLevel=").append(yearLevel);
        }

        if (sectionName != null && !sectionName.isBlank()) {
            url.append("&sectionName=").append(encode(sectionName));
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url.toString()).openConnection();

        connection.setRequestMethod("GET");

        connection.setRequestProperty(
                "Authorization",
                "Bearer " + Session.getSessionToken()
        );

        int status = connection.getResponseCode();

        if (status != 200) {
            InputStream errorStream = connection.getErrorStream();

            String errorBody = "";
            if (errorStream != null) {
                errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
            }

            throw new IOException("Export failed: " + status + " | " + errorBody);
        }

        return connection
                .getInputStream()
                .readAllBytes();
    }

    public byte[] exportExamPortfolio(
            Long examId,
            String mode,
            String classOfferingId
    ) throws Exception {

        String url =
                "/exams/" + examId +
                        "/portfolio?mode=" + encode(mode);

        if (classOfferingId != null && !classOfferingId.isBlank()) {
            url += "&classOfferingId=" + encode(classOfferingId);
        }

        return downloadBytes(url);
    }

    public byte[] exportExamResultSummary(
            Long examId,
            String classOfferingId
    ) throws Exception {

        String endpoint =
                "/faculty/reports/exams/"
                        + examId
                        + "/result-summary-report";

        if (classOfferingId != null && !classOfferingId.isBlank()) {
            endpoint += "?classOfferingId=" + encode(classOfferingId);
        }

        return downloadBytes(endpoint);
    }

    public byte[] exportViolationReport(
            FacultyReportFilter filter,
            String type
    ) throws Exception {

        String url = "/faculty/reports/violations/export"
                + "?academicYear=" + encode(filter.academicYear())
                + "&term=" + encode(filter.term())
                + "&type=" + encode(type);

        if (filter.courseCode() != null) {
            url += "&courseCode=" + encode(filter.courseCode());
        }

        if (filter.classOfferingId() != null) {
            url += "&classOfferingId=" + encode(filter.classOfferingId());
        }

        if (filter.examId() != null) {
            url += "&examId=" + filter.examId();
        }

        return downloadBytes(url);
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
            connection.setRequestProperty("Authorization", "Bearer " + Session.getSessionToken());
        }

        connection.setConnectTimeout(8000);
        connection.setReadTimeout(15000);

        return connection;
    }

    public byte[] downloadBytes(String endpoint) throws Exception {

        HttpURLConnection connection =
                createConnection(endpoint, "GET");

        int status = connection.getResponseCode();

        if (status != 200) {
            throw new RuntimeException(
                    "Request failed: " + status
            );
        }

        try (
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return outputStream.toByteArray();
        }
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }

        return java.net.URLEncoder.encode(
                value,
                java.nio.charset.StandardCharsets.UTF_8
        );
    }

    private String buildReportsQuery(
            FacultyReportFilter filter
    ) {
        StringBuilder query = new StringBuilder("?");

        appendQueryParam(
                query,
                "academicYear",
                filter.academicYear()
        );

        appendQueryParam(
                query,
                "term",
                filter.term()
        );

        appendQueryParam(
                query,
                "courseCode",
                filter.courseCode()
        );

        appendQueryParam(
                query,
                "classOfferingId",
                filter.classOfferingId()
        );

        if (filter.examId() != null) {
            appendQueryParam(
                    query,
                    "examId",
                    String.valueOf(filter.examId())
            );
        }

        return query.toString();
    }

    private void appendQueryParam(
            StringBuilder query,
            String key,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        if (value.startsWith("All")) {
            return;
        }

        if (query.length() > 1) {
            query.append("&");
        }

        query.append(key)
                .append("=")
                .append(encode(value));
    }
}
