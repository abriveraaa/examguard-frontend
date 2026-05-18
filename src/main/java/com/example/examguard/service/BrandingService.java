package com.example.examguard.service;

import com.example.examguard.model.core.response.BrandingResponse;
import com.example.examguard.utility.OffsetDateTimeAdapter;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.OffsetDateTime;


public class BrandingService {
    public static final String BASE_URL = "http://localhost:8080";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .create();

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

    public BrandingResponse getBranding() throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
                new URL(BASE_URL + "/public/branding").openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Branding API failed: HTTP " + conn.getResponseCode());
        }

        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream())) {
            return gson.fromJson(reader, BrandingResponse.class);
        }
    }
}