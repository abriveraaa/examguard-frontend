package com.example.examguard.controller.profile;

import com.example.examguard.config.AppConfig;
import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.model.profile.ProfileActivityDTO;
import com.example.examguard.model.profile.ProfileClassDTO;
import com.example.examguard.model.profile.ProfileResponseDTO;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.Session;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class ProfileController {

    private final AuthApiService authApiService = new AuthApiService();


    @FXML private ImageView profileImageView;

    @FXML private Label fullNameLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label statusBadgeLabel;
    @FXML private Label schoolEmailLabel;
    @FXML private Label schoolIdLabel;
    @FXML private Label collegeOrOfficeLabel;
    @FXML private Label programOrPositionLabel;

    @FXML private Label usernameLabel;
    @FXML private Label passwordStatusLabel;
    @FXML private Label passwordLastChangedLabel;

    @FXML private Label tenureStartLabel;
    @FXML private Label tenureDurationLabel;
    @FXML private Label accountStatusLabel;

    @FXML private Label classesSectionTitleLabel;

    @FXML private VBox classesList;
    @FXML private VBox recentActivityList;

    @FXML private Button refreshButton;
    @FXML private Button uploadPhotoButton;

    @FXML
    private void initialize() {
        loadProfile();
    }

    private void loadProfile() {
        setLoading(true);

        Task<ProfileResponseDTO> task = new Task<>() {
            @Override
            protected ProfileResponseDTO call() throws Exception {
                return authApiService.getProfile();
            }
        };

        task.setOnSucceeded(event -> {
            bindProfile(task.getValue());
            setLoading(false);
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            setLoading(false);
            showAlert("Profile Error", "Unable to load profile: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void bindProfile(ProfileResponseDTO profile) {
        if (profile == null) {
            return;
        }

        fullNameLabel.setText(value(profile.getFullName()));
        roleBadgeLabel.setText(value(profile.getRole()).toUpperCase());
        statusBadgeLabel.setText(value(profile.getAccountStatus()).toUpperCase());

        schoolEmailLabel.setText(value(profile.getSchoolEmail()));
        schoolIdLabel.setText(value(profile.getSchoolId()));
        collegeOrOfficeLabel.setText(value(profile.getCollegeOrOffice()));
        programOrPositionLabel.setText(value(profile.getProgramOrPosition()));

        usernameLabel.setText(value(profile.getUsername()));
        passwordStatusLabel.setText(value(profile.getPasswordStatus()));
        passwordLastChangedLabel.setText(value(profile.getPasswordLastChanged()));

        tenureStartLabel.setText(value(profile.getMemberSince()));
        tenureDurationLabel.setText(value(profile.getTenureDuration()));
        accountStatusLabel.setText(value(profile.getAccountStatus()));

        classesSectionTitleLabel.setText(
                "Enrolled Classes · " +
                        value(profile.getCurrentTerm()) +
                        " · " +
                        value(profile.getCurrentAcademicYear())
        );

        loadProfileImage(profile);
        renderClasses(profile);
        renderActivities(profile);
    }

    private void loadProfileImage(ProfileResponseDTO profile) {
        String imageUrl = profile.getProfileImageUrl();

        if (imageUrl == null || imageUrl.isBlank()) {
            profileImageView.setImage(null);
            return;
        }

        String finalUrl = imageUrl.startsWith("http")
                ? imageUrl
                : AppConfig.BASE_URL + imageUrl;

        profileImageView.setImage(
                new Image(finalUrl, true)
        );
    }

    private void renderClasses(ProfileResponseDTO profile) {
        classesList.getChildren().clear();

        if (profile.getClasses() == null || profile.getClasses().isEmpty()) {
            classesList.getChildren().add(emptyItem("No classes found."));
            return;
        }

        for (ProfileClassDTO item : profile.getClasses()) {
            classesList.getChildren().add(listItem(
                            item.getTitle(),
                            item.getSubtitle()
                    )
            );
        }
    }

    private void renderActivities(ProfileResponseDTO profile) {
        recentActivityList.getChildren().clear();

        if (profile.getRecentActivities() == null || profile.getRecentActivities().isEmpty()) {
            recentActivityList.getChildren().add(emptyItem("No recent activity."));
            return;
        }

        for (ProfileActivityDTO item : profile.getRecentActivities()) {
            recentActivityList.getChildren().add(
                    listItem(
                            item.getTitle(),
                            item.getSubtitle()
                    )
            );
        }
    }

    private VBox listItem(String title, String subtitle) {
        VBox box = new VBox(3);
        box.getStyleClass().add("profile-list-item");

        Label titleLabel = new Label(value(title));
        titleLabel.getStyleClass().add("profile-list-title");

        Label subtitleLabel = new Label(value(subtitle));
        subtitleLabel.getStyleClass().add("profile-list-subtitle");

        titleLabel.setWrapText(true);
        subtitleLabel.setWrapText(true);

        box.getChildren().addAll(titleLabel, subtitleLabel);

        return box;
    }

    private VBox emptyItem(String text) {
        VBox box = new VBox();
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("profile-list-item");

        Label label = new Label(text);
        label.getStyleClass().add("profile-list-subtitle");

        box.getChildren().add(label);

        return box;
    }

    @FXML
    private void handleRefresh() {
        loadProfile();
    }

    @FXML
    private void handleUploadPhoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose Profile Photo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png",
                        "*.jpg",
                        "*.jpeg",
                        "*.webp"
                )
        );

        File file = chooser.showOpenDialog(uploadPhotoButton.getScene().getWindow());

        if (file == null) {
            return;
        }

        uploadPhoto(file);
    }

    private void uploadPhoto(File file) {
        setLoading(true);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return authApiService.uploadProfilePhoto(
                        Session.getSessionToken(),
                        file
                );
            }
        };

        task.setOnSucceeded(event -> {
            loadProfile();
            showAlert("Upload Successful", "Profile photo updated successfully.");
        });

        task.setOnFailed(event -> {
            task.getException().printStackTrace();
            setLoading(false);
            showAlert("Upload Failed", "Unable to upload photo: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleChangePassword() {

        try {

            DashboardShellController shell = DashboardShellController.getInstance();

            shell.setGreeting("Change Password", "Update your account password securely.");

            shell.hideHeroCards();

            shell.loadContent("/fxml/common/change-password.fxml"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewAllActivity() {
        showAlert("Recent Activity", "You can connect this later to a full activity log page.");
    }

    @FXML
    private void handleViewAllClasses() {
        showAlert("Classes", "You can connect this later to the full classes page.");
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            refreshButton.setDisable(loading);
            uploadPhotoButton.setDisable(loading);
        });
    }

    private String value(String text) {
        return text == null || text.isBlank() ? "Not available" : text;
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}