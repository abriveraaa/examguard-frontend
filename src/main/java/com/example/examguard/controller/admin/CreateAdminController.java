package com.example.examguard.controller.admin;

import com.example.examguard.service.AdminApiService;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CreateAdminController {

    @FXML private TextField employeeIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private DatePicker birthDatePicker;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;

    private final AdminApiService adminApiService = new AdminApiService();
    private final Gson gson = new Gson();

    private Runnable onSuccess;

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleSave() {
        errorLabel.setText("");

        String employeeId = safe(employeeIdField.getText());
        String firstName = safe(firstNameField.getText());
        String lastName = safe(lastNameField.getText());
        String email = safe(emailField.getText());
        LocalDate birthDate = birthDatePicker.getValue();

        if (employeeId.isBlank() || firstName.isBlank() || lastName.isBlank() || email.isBlank() || birthDate == null) {
            errorLabel.setText("Please complete all required fields.");
            return;
        }

        saveButton.setDisable(true);
        saveButton.setText("Saving...");

        Map<String, String> payload = new HashMap<>();
        payload.put("employeeId", employeeId);
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("email", email);
        payload.put("birthDate", birthDate.toString());

        String jsonBody = gson.toJson(payload);

        new Thread(() -> {
            try {
                String response = adminApiService.createAdminProfile(jsonBody);

                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save");

                    if (response != null && response.toLowerCase().contains("successfully")) {
                        if (onSuccess != null) {
                            onSuccess.run();
                        }
                        closeDialog();
                    } else {
                        errorLabel.setText(response != null ? response : "Failed to create admin profile.");
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    saveButton.setDisable(false);
                    saveButton.setText("Save");
                    errorLabel.setText("Error creating admin profile.");
                });
            }
        }).start();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}