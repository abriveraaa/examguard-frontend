package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.ReactivationJustificationDialogController;
import com.example.examguard.model.core.UserManagementRow;
import com.example.examguard.service.AdminApiService;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class EditAdminController {

    @FXML private TextField employeeIdField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private DatePicker birthDatePicker;
    @FXML private Label errorLabel;
    @FXML private Button saveButton;
    @FXML private Button deactivateButton;
    @FXML private Button reactivateButton;

    private int totalActiveAdmins = 0;

    private final AdminApiService adminApiService = new AdminApiService();
    private final Gson gson = new Gson();

    private Runnable onSuccess;
    private String originalEmployeeId;
    private String currentLoggedInUsername;
    private UserManagementRow currentRow;

    private interface AdminAction {
        String execute();
    }

    public void configure(UserManagementRow row, String currentLoggedInAdminId, int totalActiveAdmins) {
        this.currentRow = row;
        this.currentLoggedInUsername = currentLoggedInAdminId;
        this.totalActiveAdmins = totalActiveAdmins;

        setAdminData(row);
        updateActionButtons();
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public void setAdminData(UserManagementRow row) {
        if (row == null) return;

        this.currentRow = row;
        this.originalEmployeeId = row.getSchoolId();

        employeeIdField.setText(row.getSchoolId());
        employeeIdField.setDisable(true);
        emailField.setText(row.getEmail());

        setNameFields(row.getFullName());
        setBirthDate(row.getBirthDate());

        updateActionButtons();
    }

    private void setNameFields(String fullName) {
        String name = safe(fullName);
        String[] parts = name.split(" ", 2);

        firstNameField.setText(parts.length > 0 ? parts[0] : "");
        lastNameField.setText(parts.length > 1 ? parts[1] : "");
    }

    private void setBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank() || "-".equals(birthDate)) {
            birthDatePicker.setValue(null);
            return;
        }

        birthDatePicker.setValue(LocalDate.parse(birthDate));
    }

    @FXML
    private void handleUpdate() {
        errorLabel.setText("");

        String firstName = safe(firstNameField.getText());
        String lastName = safe(lastNameField.getText());
        String email = safe(emailField.getText());
        LocalDate birthDate = birthDatePicker.getValue();

        if (firstName.isBlank() || lastName.isBlank() || email.isBlank()) {
            errorLabel.setText("Please complete all required fields.");
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("firstName", firstName);
        payload.put("lastName", lastName);
        payload.put("email", email);

        if (birthDate != null) {
            payload.put("birthDate", birthDate.toString());
        }

        setSavingState(true, "Updating...");

        runAdminAction(
                () -> adminApiService.updateAdminProfile(originalEmployeeId, gson.toJson(payload)),
                response -> {
                    setSavingState(false, "Update");

                    if (response != null && response.toLowerCase().contains("updated")) {
                        runSuccessAndClose();
                    } else {
                        errorLabel.setText(response != null ? response : "Failed to update admin profile.");
                    }
                },
                () -> {
                    setSavingState(false, "Update");
                    errorLabel.setText("Failed to update admin profile.");
                }
        );
    }

    @FXML
    private void handleDeactivate() {
        handleStatusAction(
                "Provide reason for deactivating this admin profile.",
                "Deactivate Reason",
                reason -> adminApiService.deactivateAdmin(
                        originalEmployeeId,
                        currentLoggedInUsername,
                        reason
                )
        );
    }

    @FXML
    private void handleReactivate() {
        handleStatusAction(
                "Provide justification for reactivating this admin profile.",
                "Reactivation Reason",
                reason -> adminApiService.reactivateAdmin(
                        originalEmployeeId,
                        reason
                )
        );
    }

    private void handleStatusAction(
            String description,
            String title,
            java.util.function.Function<String, String> action
    ) {
        String reason = askForJustification(description, title);

        if (reason == null || reason.isBlank()) {
            return;
        }

        saveButton.setDisable(true);

        runAdminAction(
                () -> action.apply(reason),
                response -> {
                    saveButton.setDisable(false);
                    showAlert(response != null ? response : "Action completed.");
                    runSuccessAndClose();
                },
                () -> {
                    saveButton.setDisable(false);
                    showAlert("Action failed.");
                }
        );
    }

    private void runAdminAction(
            AdminAction action,
            java.util.function.Consumer<String> onSuccess,
            Runnable onError
    ) {
        new Thread(() -> {
            try {
                String response = action.execute();
                Platform.runLater(() -> onSuccess.accept(response));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(onError);
            }
        }).start();
    }

    private void runSuccessAndClose() {
        if (onSuccess != null) {
            onSuccess.run();
        }

        closeDialog();
    }

    private void setSavingState(boolean loading, String text) {
        saveButton.setDisable(loading);
        saveButton.setText(text);
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }

    private String askForJustification(String description, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/reactivation-justification.fxml"));
            Parent root = loader.load();

            ReactivationJustificationDialogController controller = loader.getController();
            controller.setDescription(description);

            openDialog(root, title == null || title.isBlank() ? description : title, 450, 320);

            if (controller.isConfirmed()) {
                return controller.getJustification();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open justification dialog.");
        }

        return null;
    }

    private void openDialog(Parent root, String title, double width, double height) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.setScene(new Scene(root));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setResizable(false);
        stage.showAndWait();
    }

    private void updateActionButtons() {
        if (deactivateButton == null || reactivateButton == null || currentRow == null) {
            return;
        }

        boolean profileActive = isActive(currentRow.getRegistrarStatus());
        boolean systemActive = isActive(currentRow.getSystemAccess());
        boolean isSelf = isCurrentLoggedInAdmin();

        boolean showDeactivate = profileActive
                && systemActive
                && totalActiveAdmins > 1
                && !isSelf;

        boolean showReactivate = !profileActive;

        setButtonVisible(deactivateButton, showDeactivate);
        setButtonVisible(reactivateButton, showReactivate);
    }

    private boolean isCurrentLoggedInAdmin() {
        return currentLoggedInUsername != null
                && currentRow.getUsername() != null
                && currentLoggedInUsername.equalsIgnoreCase(currentRow.getUsername());
    }

    private boolean isActive(String value) {
        return "Active".equalsIgnoreCase(value);
    }

    private void setButtonVisible(Button button, boolean visible) {
        button.setVisible(visible);
        button.setManaged(visible);
    }

    public void setTotalActiveAdmins(int totalActiveAdmins) {
        this.totalActiveAdmins = totalActiveAdmins;
        updateActionButtons();
    }

    public void setCurrentLoggedInUsername(String username) {
        this.currentLoggedInUsername = username;
        updateActionButtons();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }
}