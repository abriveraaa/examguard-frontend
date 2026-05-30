package com.example.examguard.controller.profile;

import com.example.examguard.model.core.response.LoginApiResponse;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.LoadingSpinner;
import com.example.examguard.utility.SceneManager;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ChangePasswordController {

   @FXML private PasswordField currentPasswordField;
   @FXML private PasswordField newPasswordField;
   @FXML private PasswordField confirmPasswordField;

   @FXML private TextField newPasswordVisibleField;
   @FXML private TextField confirmPasswordVisibleField;

   @FXML private Label errorLabel;
   @FXML private Label mustChangeLabel;
   @FXML private Button updatePasswordButton;
   @FXML private ProgressIndicator loadingSpinner;

   @FXML private ProgressBar passwordStrengthBar;
   @FXML private Label passwordStrengthLabel;
   @FXML private Label passwordMatchLabel;

   @FXML private Label ruleLengthLabel;
   @FXML private Label ruleUpperLabel;
   @FXML private Label ruleLowerLabel;
   @FXML private Label ruleDigitLabel;
   @FXML private Label ruleSpecialLabel;

    private final AuthApiService authApiService = new AuthApiService();
    private final Gson gson = new Gson();

    private boolean showNewPassword = false;
    private boolean showConfirmPassword = false;
    private boolean isLoading = false;

    @FXML
    public void initialize() {
        boolean mustChange = Session.isMustChangePassword();
        mustChangeLabel.setVisible(mustChange);
        mustChangeLabel.setManaged(mustChange);

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        newPasswordVisibleField.setVisible(false);
        newPasswordVisibleField.setManaged(false);
        confirmPasswordVisibleField.setVisible(false);
        confirmPasswordVisibleField.setManaged(false);

        bindPasswordVisibility();
        attachLiveValidation();
        updatePasswordStrengthUI("");
        updatePasswordMatchUI();
        updateSubmitButtonState();

        currentPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updateSubmitButtonState());
    }

    private void bindPasswordVisibility() {
        newPasswordVisibleField.textProperty().bindBidirectional(newPasswordField.textProperty());
        confirmPasswordVisibleField.textProperty().bindBidirectional(confirmPasswordField.textProperty());
    }

    private void attachLiveValidation() {
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrengthUI(newVal);
            updatePasswordMatchUI();
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordMatchUI();
        });
    }

    @FXML
    private void handleToggleNewPassword() {
        showNewPassword = !showNewPassword;
        newPasswordVisibleField.setVisible(showNewPassword);
        newPasswordVisibleField.setManaged(showNewPassword);
        newPasswordField.setVisible(!showNewPassword);
        newPasswordField.setManaged(!showNewPassword);
    }

    @FXML
    private void handleToggleConfirmPassword() {
        showConfirmPassword = !showConfirmPassword;
        confirmPasswordVisibleField.setVisible(showConfirmPassword);
        confirmPasswordVisibleField.setManaged(showConfirmPassword);
        confirmPasswordField.setVisible(!showConfirmPassword);
        confirmPasswordField.setManaged(!showConfirmPassword);
    }

    @FXML
    public void handleChangePassword() {
        String username = Session.getUsername();
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        String validationMessage = validatePasswordInput(currentPassword, newPassword, confirmPassword);

        if (validationMessage != null) {
            errorLabel.setStyle("-fx-text-fill: #b91c1c;");
            errorLabel.setText(validationMessage);
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        if (username == null || username.isBlank()) {
            errorLabel.setStyle("-fx-text-fill: #b91c1c;");
            errorLabel.setText("No active session found. Please log in again.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);
            return;
        }

        errorLabel.setStyle("-fx-text-fill: #666666;");
        errorLabel.setText("Updating password...");
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);

        setLoadingState(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return authApiService.changePassword(username, currentPassword, newPassword, confirmPassword);
            }
        };

        task.setOnSucceeded(event -> {
            setLoadingState(false);

            try {
                String response = task.getValue();
                LoginApiResponse apiResponse = gson.fromJson(response, LoginApiResponse.class);

                if (apiResponse == null) {
                    errorLabel.setStyle("-fx-text-fill: #b91c1c;");
                    errorLabel.setText("Invalid server response.");
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                    return;
                }

                if (apiResponse.isSuccess()) {
                    errorLabel.setStyle("-fx-text-fill: green;");
                    errorLabel.setText(apiResponse.getMessage());

                    Session.setMustChangePassword(false);
                    mustChangeLabel.setVisible(false);
                    mustChangeLabel.setManaged(false);

                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();

                    showNewPassword = false;
                    showConfirmPassword = false;
                    newPasswordVisibleField.setVisible(false);
                    newPasswordVisibleField.setManaged(false);
                    newPasswordField.setVisible(true);
                    newPasswordField.setManaged(true);

                    confirmPasswordVisibleField.setVisible(false);
                    confirmPasswordVisibleField.setManaged(false);
                    confirmPasswordField.setVisible(true);
                    confirmPasswordField.setManaged(true);

                    updatePasswordStrengthUI("");
                    updatePasswordMatchUI();
                    updateSubmitButtonState();

                    showSuccessLogoutPopup();
                } else {
                    errorLabel.setStyle("-fx-text-fill: #b91c1c;");
                    errorLabel.setText(apiResponse.getMessage());
                    errorLabel.setVisible(true);
                    errorLabel.setManaged(true);
                    updateSubmitButtonState();
                }

            } catch (Exception e) {
                errorLabel.setStyle("-fx-text-fill: #b91c1c;");
                errorLabel.setText("Failed to process server response.");
                errorLabel.setVisible(true);
                errorLabel.setManaged(true);
                e.printStackTrace();
                updateSubmitButtonState();
            }
        });

        task.setOnFailed(event -> {
            setLoadingState(false);
            errorLabel.setStyle("-fx-text-fill: #b91c1c;");
            errorLabel.setText("Cannot connect to backend.");
            errorLabel.setVisible(true);
            errorLabel.setManaged(true);

            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }

            updateSubmitButtonState();
        });

        Platform.runLater(() -> {
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });
    }

    private String validatePasswordInput(String currentPassword, String newPassword, String confirmPassword) {
        if (currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            return "Please fill in all password fields.";
        }

        if (newPassword.equals(currentPassword)) {
            return "New password must be different from current password.";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "New password and confirm password do not match.";
        }

        if (newPassword.length() < 8) {
            return "Password must be at least 8 characters.";
        }

        if (!newPassword.matches(".*[A-Z].*")) {
            return "Password must include at least one uppercase letter.";
        }

        if (!newPassword.matches(".*[a-z].*")) {
            return "Password must include at least one lowercase letter.";
        }

        if (!newPassword.matches(".*\\d.*")) {
            return "Password must include at least one number.";
        }

        if (!newPassword.matches(".*[^A-Za-z0-9].*")) {
            return "Password must include at least one special character.";
        }


        return null;
    }

    private void updatePasswordStrengthUI(String password) {
        if (password == null) {
            password = "";
        }

        boolean hasLength = password.length() >= 8;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");

        updateRuleLabel(ruleLengthLabel, hasLength, "At least 8 characters");
        updateRuleLabel(ruleUpperLabel, hasUpper, "One uppercase letter");
        updateRuleLabel(ruleLowerLabel, hasLower, "One lowercase letter");
        updateRuleLabel(ruleDigitLabel, hasDigit, "One number");
        updateRuleLabel(ruleSpecialLabel, hasSpecial, "One special character");

        int score = 0;
        if (hasLength) score++;
        if (hasUpper) score++;
        if (hasLower) score++;
        if (hasDigit) score++;
        if (hasSpecial) score++;

        double progress = score / 5.0;
        passwordStrengthBar.setProgress(progress);

        if (score <= 2) {
            passwordStrengthBar.setStyle("-fx-accent: #dc2626;");
            passwordStrengthLabel.setText("Weak password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        } else if (score <= 4) {
            passwordStrengthBar.setStyle("-fx-accent: #d97706;");
            passwordStrengthLabel.setText("Medium password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #d97706; -fx-font-size: 12px;");
        } else {
            passwordStrengthBar.setStyle("-fx-accent: #16a34a;");
            passwordStrengthLabel.setText("Strong password");
            passwordStrengthLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px;");
        }
    }

    private void updateRuleLabel(Label label, boolean valid, String text) {
        if (valid) {
            label.setText("✓ " + text);
            label.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px;");
        } else {
            label.setText("✗ " + text);
            label.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        }
    }

    private void updatePasswordMatchUI() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (confirmPassword == null || confirmPassword.isBlank()) {
            passwordMatchLabel.setText("");
            passwordMatchLabel.setStyle("-fx-font-size: 12px;");
            return;
        }

        if (newPassword.equals(confirmPassword)) {
            passwordMatchLabel.setText("✓ Passwords match");
            passwordMatchLabel.setStyle("-fx-text-fill: #16a34a; -fx-font-size: 12px;");
        } else {
            passwordMatchLabel.setText("✗ Passwords do not match");
            passwordMatchLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
        }
    }

    private void updateSubmitButtonState() {
        if (isLoading) {
            updatePasswordButton.setDisable(true);
            return;
        }

        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        boolean filled = currentPassword != null && !currentPassword.isBlank()
                && newPassword != null && !newPassword.isBlank()
                && confirmPassword != null && !confirmPassword.isBlank();

        boolean strongEnough = newPassword != null
                && newPassword.length() >= 8
                && newPassword.matches(".*[A-Z].*")
                && newPassword.matches(".*[a-z].*")
                && newPassword.matches(".*\\d.*")
                && newPassword.matches(".*[^A-Za-z0-9].*");

        boolean matches = newPassword != null && newPassword.equals(confirmPassword);
        boolean differentFromCurrent = currentPassword != null
                && newPassword != null
                && !newPassword.equals(currentPassword);

        boolean canSubmit = filled && strongEnough && matches && differentFromCurrent;

        updatePasswordButton.setDisable(!canSubmit);

        if (canSubmit) {
            updatePasswordButton.setStyle(
                    "-fx-background-color: #650000;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 22;" +
                            "-fx-cursor: hand;"
            );
        } else {
            updatePasswordButton.setStyle(
                    "-fx-background-color: #b0b0b0;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-weight: bold;" +
                            "-fx-background-radius: 22;" +
                            "-fx-cursor: default;"
            );
        }
    }

    private void showSuccessLogoutPopup() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Password Updated");
        alert.setHeaderText("Password changed successfully");

        Label countdownLabel = new Label();
        countdownLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #444;");

        alert.getDialogPane().setContent(countdownLabel);

        final int[] seconds = {3};

        countdownLabel.setText("You will be logged out in 3 seconds.");

        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> {
                    seconds[0]--;
                    if (seconds[0] > 0) {
                        countdownLabel.setText("You will be logged out in " + seconds[0] + " seconds.");
                    }
                })
        );

        timeline.setCycleCount(3);

        timeline.setOnFinished(event -> {
            alert.close();

            try {
                String token = Session.getSessionToken();
                if (token != null) {
                    new AuthApiService().logout(token);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            Session.clear();
            SceneManager.switchScene("auth/login.fxml");
        });

        timeline.play();
        alert.show();
    }

    private void setLoadingState(boolean loading) {
        isLoading = loading;

        LoadingSpinner.setLoading(
                updatePasswordButton,
                loading,
                "Updating...",
                "Update Password"
        );

        updateSubmitButtonState();
    }
}