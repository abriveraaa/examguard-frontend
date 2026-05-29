package com.example.examguard.controller.auth;

import com.example.examguard.model.core.response.LoginApiResponse;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.ApiErrorUtil;
import com.example.examguard.utility.LoadingSpinner;
import com.example.examguard.utility.SceneManager;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ForgotPasswordController {

    @FXML private TextField userIdField;
    @FXML private TextField emailField;
    @FXML private DatePicker birthdayField;
    @FXML private Button forgotPasswordButton;
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private Label errorLabel;

    private final AuthApiService authApiService = new AuthApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        configureBirthdayPicker();
    }

    private void configureBirthdayPicker() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy");

        birthdayField.setPromptText("MM/dd/yyyy");

        birthdayField.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return (date != null) ? formatter.format(date) : "";
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }

                try {
                    errorLabel.setText("");
                    return LocalDate.parse(text.trim(), formatter);
                } catch (Exception e) {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Use birthday format MM/dd/yyyy");
                    return null;
                }
            }
        });
    }

    @FXML
    public void handleForgotPassword() {
        String userId = userIdField.getText().trim();
        String email = emailField.getText().trim();
        LocalDate birthday = birthdayField.getValue();

        if (userId.isEmpty() || email.isEmpty() || birthday == null) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        String formattedBirthday = birthday.toString(); // yyyy-MM-dd for backend

        errorLabel.setStyle("-fx-text-fill: white;");
        errorLabel.setText("Verifying your account...");

        setLoadingState(true);

        Task<String> forgotTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return authApiService.forgotPassword(userId, email, formattedBirthday);
            }
        };

        forgotTask.setOnSucceeded(event -> {
            setLoadingState(false);

            try {
                String response = forgotTask.getValue();
                System.out.println("RAW FORGOT PASSWORD RESPONSE: " + response);

                LoginApiResponse apiResponse = gson.fromJson(response, LoginApiResponse.class);

                if (apiResponse == null) {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Invalid server response.");
                    return;
                }

                if (apiResponse.isSuccess()) {
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText(apiResponse.getMessage());
                } else {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText(apiResponse.getMessage());
                }

            } catch (Exception e) {
                errorLabel.setStyle("-fx-text-fill: red;");
                errorLabel.setText("Failed to process server response.");
                e.printStackTrace();
            }
        });

        forgotTask.setOnFailed(event -> {
            setLoadingState(false);
            errorLabel.setStyle("-fx-text-fill: red;");


            Throwable ex = forgotTask.getException();
            errorLabel.setText(ApiErrorUtil.extractMessage(ex));
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Platform.runLater(() -> {
            Thread thread = new Thread(forgotTask);
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void setLoadingState(boolean loading) {
        LoadingSpinner.setLoading(
                forgotPasswordButton,
                loading,
                "Sending reset...",
                "FORGOT PASSWORD"
        );
    }

    @FXML
    public void goToLogin() {
        SceneManager.switchScene("auth/login.fxml");
    }
}