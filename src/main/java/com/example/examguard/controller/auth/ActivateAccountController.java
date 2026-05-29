package com.example.examguard.controller.auth;

import com.example.examguard.model.core.response.LoginApiResponse;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.ApiErrorUtil;
import com.example.examguard.utility.LoadingSpinner;
import com.example.examguard.utility.SceneManager;
import com.google.gson.Gson;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ActivateAccountController {

    @FXML
    private TextField userIdField;
    @FXML
    private TextField emailField;
    @FXML
    private DatePicker birthdayField;
    @FXML
    private Button signupButton;
    @FXML
    private ProgressIndicator loadingSpinner;
    @FXML
    private Label errorLabel;

    private final AuthApiService authApiService = new AuthApiService();
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        configureBirthdayPicker();
    }

    @FXML
    public void handleSignup() {
        String userId = userIdField.getText().trim();
        String email = emailField.getText().trim();
        LocalDate birthday = birthdayField.getValue();

        if (userId.isEmpty() || email.isEmpty() || birthday == null) {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Please fill in all fields.");
            return;
        }

        String formattedBirthday = birthday.toString(); // yyyy-MM-dd

        errorLabel.setStyle("-fx-text-fill: white;");
        errorLabel.setText("Verifying your account...");

        setLoadingState(true);

        Task<String> activateTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                return authApiService.activateAccount(userId, email, formattedBirthday);
            }
        };

        activateTask.setOnSucceeded(event -> {
            setLoadingState(false);

            try {
                String response = activateTask.getValue();
                LoginApiResponse apiResponse = gson.fromJson(response, LoginApiResponse.class);

                if (apiResponse == null) {
                    errorLabel.setStyle("-fx-text-fill: red;");
                    errorLabel.setText("Invalid server response.");
                    return;
                }

                if (apiResponse.isSuccess()) {
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText(apiResponse.getMessage());

//                     optional redirect after success:
                    PauseTransition pause = new PauseTransition(Duration.seconds(2));
                    pause.setOnFinished(e -> SceneManager.switchScene("auth/login.fxml"));
                    pause.play();

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

        activateTask.setOnFailed(event -> {
            setLoadingState(false);
            errorLabel.setStyle("-fx-text-fill: red;");

            Throwable ex = activateTask.getException();
            errorLabel.setText(ApiErrorUtil.extractMessage(ex));

            if (ex != null) {
                ex.printStackTrace();
            }
        });

        javafx.application.Platform.runLater(() -> {
            Thread thread = new Thread(activateTask);
            thread.setDaemon(true);
            thread.start();
        });
    }

    private void setLoadingState(boolean loading) {
        LoadingSpinner.setLoading(
                signupButton,
                loading,
                "Activating...",
                "ACTIVATE MY ACCOUNT"
        );
    }

    private String extractApiErrorMessage(Throwable ex) {

        if (ex == null || ex.getMessage() == null) {
            return "Request failed.";
        }

        try {
            String error = ex.getMessage();

            int jsonStart = error.indexOf("{");

            if (jsonStart >= 0) {

                String json = error.substring(jsonStart);

                LoginApiResponse response =
                        gson.fromJson(json, LoginApiResponse.class);

                if (response != null
                        && response.getMessage() != null
                        && !response.getMessage().isBlank()) {

                    return response.getMessage();
                }
            }

        } catch (Exception ignored) {
        }

        return "Request failed.";
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

        birthdayField.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);

                if (empty) return;

                LocalDate today = LocalDate.now();

                if (date.isAfter(today) || date.isBefore(today.minusYears(100))) {
                    setDisable(true);
                }
            }
        });
    }

    @FXML
    public void goToLogin() {
        SceneManager.switchScene("auth/login.fxml");
    }
}