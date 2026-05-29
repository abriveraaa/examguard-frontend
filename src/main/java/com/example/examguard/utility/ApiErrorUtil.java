package com.example.examguard.utility;

import com.example.examguard.model.core.response.LoginApiResponse;
import com.google.gson.Gson;

public final class ApiErrorUtil {

    private static final Gson GSON = new Gson();

    public static String extractMessage(Throwable ex) {

        if (ex == null || ex.getMessage() == null) {
            return "Request failed.";
        }

        try {
            int jsonStart = ex.getMessage().indexOf("{");

            if (jsonStart >= 0) {

                String json = ex.getMessage().substring(jsonStart);

                LoginApiResponse response =
                        GSON.fromJson(json, LoginApiResponse.class);

                if (response != null &&
                        response.getMessage() != null &&
                        !response.getMessage().isBlank()) {

                    return response.getMessage();
                }
            }

        } catch (Exception ignored) {
        }

        return "Request failed.";
    }
}
