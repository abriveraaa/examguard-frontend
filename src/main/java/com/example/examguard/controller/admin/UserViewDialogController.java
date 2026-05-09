package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.ReactivationJustificationDialogController;
import com.example.examguard.model.UserDetailsResponse;
import com.example.examguard.model.UserManagementRow;
import com.example.examguard.service.AdminApiService;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public class UserViewDialogController {

    @FXML private ImageView photoView;
    @FXML private Label roleLabel;
    @FXML private Button reactivateButton;

    @FXML private GridPane accountBox, academicBox, accountStatus;
    @FXML private VBox academicSection;
    private String currentRole;


    private final AdminApiService adminApiService = new AdminApiService();
    private final Gson gson = new Gson();
    private UserManagementRow currentRow;
    private boolean eligibleForReactivation;

    public void setUserData(UserManagementRow row, String role, boolean eligibleForReactivation) {

        this.currentRow = row;
        this.currentRole = role;
        this.eligibleForReactivation = eligibleForReactivation;

        roleLabel.setText(role);

        if (reactivateButton != null) {
            boolean show =
                    eligibleForReactivation &&
                            ("STUDENT".equalsIgnoreCase(role) || "FACULTY".equalsIgnoreCase(role));

            reactivateButton.setVisible(show);
            reactivateButton.setManaged(show);
        }

        accountBox.getChildren().clear();
        academicBox.getChildren().clear();

        new Thread(() -> {
            try {
                String json = adminApiService.getUserDetails(row.getSchoolId(), role);

                if (json == null || json.isBlank()) {
                    Platform.runLater(() -> populateFallback(row, role));
                    return;
                }

                UserDetailsResponse details = gson.fromJson(json, UserDetailsResponse.class);
                Platform.runLater(() -> populate(details));

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> populateFallback(row, role));
            }
        }).start();
    }

    private void populate(UserDetailsResponse details) {
        accountBox.getChildren().clear();
        academicBox.getChildren().clear();
        accountStatus.getChildren().clear();

        roleLabel.setText(safe(details.getRole()));

        int row = 0;
        addRow(accountBox, row++, "ID", details.getSchoolId());
        addRow(accountBox, row++, "Username", details.getUsername());
        addRow(accountBox, row++, "Name", details.getFullName());
        addRow(accountBox, row++, "Email", details.getEmail());
        addRow(accountBox, row++, "Registrar Status", details.getAccountStatus());

        int actRow = 0;
        addRow(accountStatus, actRow++, "System Access", details.getSystemAccess());
        addRow(accountStatus, actRow++, "Failed Attempts", String.valueOf(safeInt(details.getFailedAttempts())));
        addRow(accountStatus, actRow++, "Last Login", details.getLastLogin());

        if ("ADMIN".equalsIgnoreCase(details.getRole())) {
            academicSection.setVisible(false);
            academicSection.setManaged(false);
            return;
        }

        academicSection.setVisible(true);
        academicSection.setManaged(true);

        int acadRow = 0;
        addRow(academicBox, acadRow++, "College", details.getCollegeName());

        if ("STUDENT".equalsIgnoreCase(details.getRole())) {
            addRow(academicBox, acadRow++, "Program", details.getProgramName());
            addRow(academicBox, acadRow++, "Year", details.getYearLevel());
            addRow(academicBox, acadRow++, "Section", details.getSectionName());
        }
    }

    private void populateFallback(UserManagementRow row, String role) {
        accountBox.getChildren().clear();
        academicBox.getChildren().clear();
        accountStatus.getChildren().clear();

        roleLabel.setText(role);

        int i = 0;
        addRow(accountBox, i++, "ID", row.getSchoolId());
        addRow(accountBox, i++, "Username", row.getUsername());
        addRow(accountBox, i++, "Name", row.getFullName());
        addRow(accountBox, i++, "Email", row.getEmail());
        addRow(accountBox, i++, "Registrar Status", row.getRegistrarStatus());

        int s = 0;
        addRow(accountStatus, s++, "System Access", row.getSystemAccess());

        if ("ADMIN".equalsIgnoreCase(role)) {
            academicSection.setVisible(false);
            academicSection.setManaged(false);
            return;
        }

        academicSection.setVisible(true);
        academicSection.setManaged(true);

        int j = 0;
        addRow(academicBox, j++, "College", row.getCollegeName());

        if ("STUDENT".equalsIgnoreCase(role)) {
            addRow(academicBox, j++, "Program", row.getProgramName());
            addRow(academicBox, j++, "Year", row.getYearLevel());
            addRow(academicBox, j++, "Section", row.getSectionName());
        }
    }

    private void addRow(GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText + ":");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #302C29;");

        Label value = new Label(safe(valueText));
        value.setWrapText(true);
        value.setStyle("-fx-text-fill: #4b5563;");

        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) roleLabel.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleReactivate() {

        if (currentRow == null || currentRole == null) { return; }

        String justification = openJustificationDialog(
                "Provide justification for deactivating this account",
                "Deactivate Reason"
        );

        if (justification == null || justification.isBlank()) { return; }

        reactivateButton.setDisable(true);
        reactivateButton.setText("Reactivating...");

        new Thread(() -> {
            try {
                String response = adminApiService.reactivateSingleUser(
                        currentRow.getSchoolId(),
                        currentRole,
                        justification
                );

                Platform.runLater(() -> {
                    reactivateButton.setDisable(false);
                    reactivateButton.setText("Reactivate Access");

                    new Alert(
                            Alert.AlertType.INFORMATION,
                            response != null ? response : "User reactivated successfully."
                    ).showAndWait();

                    // hide button after success
                    reactivateButton.setVisible(false);
                    reactivateButton.setManaged(false);

                    // optional: update local state
                    eligibleForReactivation = false;

                    Stage stage = (Stage) reactivateButton.getScene().getWindow();
                    stage.close();
                });

            } catch (Exception e) {
                e.printStackTrace();

                Platform.runLater(() -> {
                    reactivateButton.setDisable(false);
                    reactivateButton.setText("Reactivate Access");

                    new Alert(
                            Alert.AlertType.ERROR,
                            "Failed to reactivate user."
                    ).showAndWait();
                });
            }
        }).start();
    }

    private String openJustificationDialog(String description, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/reactivation-justification.fxml")
            );
            Parent root = loader.load();

            ReactivationJustificationDialogController controller = loader.getController();
            controller.setDescription(description);

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.setWidth(450);
            stage.setHeight(320);
            stage.setResizable(false);
            stage.showAndWait();

            if (controller.isConfirmed()) {
                return controller.getJustification();
            }

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to open justification dialog").showAndWait();
        }

        return null;
    }
}