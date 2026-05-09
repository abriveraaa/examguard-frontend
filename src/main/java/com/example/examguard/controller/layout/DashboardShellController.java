package com.example.examguard.controller.layout;

import com.example.examguard.controller.admin.ExamManagementController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DashboardShellController {

    @FXML private HBox navContainer;
    @FXML private Label avatarLabel;
    @FXML private Label greetingTitleLabel;
    @FXML private Label greetingSubtitleLabel;
    @FXML private StackPane heroPane;
    @FXML private ImageView bgImage;
    @FXML private StackPane contentHolder;
    @FXML private HBox heroCardsContainer;

    private String currentRole;
    private String activePage;

    String name = com.example.examguard.utility.Session.getFirstName();


    @FXML
    public void initialize() {
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(heroPane.widthProperty());
        clip.heightProperty().bind(heroPane.heightProperty());
        heroPane.setClip(clip);

        bgImage.fitWidthProperty().bind(heroPane.widthProperty());

//        floatingStudentAvatar.setVisible(false);
//        floatingStudentAvatar.setManaged(false);
    }

    public void setAvatarLetter(String letter) {
        avatarLabel.setText(letter);
    }

    public void setGreeting(String title, String subtitle) {
        greetingTitleLabel.setText(title);
        greetingSubtitleLabel.setText(subtitle);
    }

    public void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node content = loader.load();

            boolean isFullWidthPage =
                    fxmlPath.equals("/fxml/student/student-dashboard.fxml") ||
                            fxmlPath.equals("/fxml/faculty/faculty-dashboard.fxml");

            if (isFullWidthPage) {
                contentHolder.setPadding(Insets.EMPTY);
                contentHolder.setStyle("-fx-padding: 0; -fx-background-color: rgb(251, 249, 248);");

                StackPane.setAlignment(content, Pos.TOP_LEFT);

                if (content instanceof Region region) {
                    region.prefWidthProperty().bind(contentHolder.widthProperty());
                    region.prefHeightProperty().bind(contentHolder.heightProperty());
                }

            } else {
                contentHolder.setPadding(new Insets(16, 16, 16, 16));
                contentHolder.setStyle("-fx-padding: 16 16 16 16; -fx-background-color: rgb(251, 249, 248);");
            }

            contentHolder.getChildren().setAll(content);

            updateHeroVisibility(fxmlPath);

            Object controller = loader.getController();

            if (controller instanceof ShellAwareController shellAwareController) {
                shellAwareController.setShellController(this);
            }

        } catch (IOException e) {
            System.out.println("Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private void updateHeroVisibility(String fxmlPath) {
        boolean hideHero =
                fxmlPath.contains("create-exam") ||
                        fxmlPath.contains("edit-exam") ||
                        fxmlPath.contains("question-editor");

        heroPane.setVisible(!hideHero);
        heroPane.setManaged(!hideHero);
    }

    public void loadAdminView() {
        currentRole = "ADMIN";
        setAvatarLetter("A");

        if (com.example.examguard.utility.Session.isMustChangePassword()) {
            activePage = "Profile";
            buildNavigation();
            setGreeting("Change Password", "Update your password before continuing.");
            hideHeroCards();
            loadContent("/fxml/common/change-password.fxml");
            return;
        }

        switchPage("Home");
    }

    public void loadFacultyView() {
        currentRole = "FACULTY";
        setAvatarLetter("F");

        if (com.example.examguard.utility.Session.isMustChangePassword()) {
            activePage = "Profile";
            buildNavigation();
            setGreeting("Change Password", "Update your password before continuing.");
            hideHeroCards();
            loadContent("/fxml/common/change-password.fxml");
            return;
        }

        switchPage("Home");
    }

    public void loadStudentView() {
        currentRole = "STUDENT";
        setAvatarLetter("S");

        if (com.example.examguard.utility.Session.isMustChangePassword()) {
            activePage = "Profile";
            buildNavigation();
            setGreeting("Change Password", "Update your password before continuing.");
            hideHeroCards();
            loadContent("/fxml/common/change-password.fxml");
            return;
        }

        switchPage("Home");
    }

    private void switchPage(String page) {
        activePage = page;
        buildNavigation();
        updateGreetingAndContent();
    }

    private void buildNavigation() {
        navContainer.getChildren().clear();

        for (String item : getPagesForRole().keySet()) {
            boolean isActive = activePage != null && item.equalsIgnoreCase(activePage);
            boolean passwordLocked = com.example.examguard.utility.Session.isMustChangePassword();

            VBox navItem = new VBox();
            navItem.setAlignment(Pos.CENTER);
            navItem.setSpacing(6);

            Button button = new Button(item);
            button.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: " + (isActive ? "white" : "#ffffffb2") + ";" +
                            "-fx-padding: 0;" +
                            "-fx-font-family: 'Segoe UI';" +
                            "-fx-font-size: 16px;" +
                            "-fx-cursor: " + (passwordLocked ? "default" : "hand") + ";" +
                            "-fx-opacity: " + (passwordLocked ? "0.55" : "1") + ";"
            );

            button.setOnAction(e -> {
                if (com.example.examguard.utility.Session.isMustChangePassword()) {
                    setGreeting(
                            "Change Password Required",
                            "You must update your password before accessing other features."
                    );
                    activePage = null;
                    buildNavigation();
                    hideHeroCards();
                    loadContent("/fxml/common/change-password.fxml");
                    return;
                }

                switchPage(item);
            });

            Pane underline = new Pane();
            underline.setPrefHeight(3);
            underline.setPrefWidth(60);
            underline.setStyle("-fx-background-color: " + (isActive ? "#F4D27A" : "#666667") + ";");

            navItem.getChildren().addAll(button, underline);
            navContainer.getChildren().add(navItem);
        }
    }

    private void updateGreetingAndContent() {
        switch (currentRole) {
            case "ADMIN":
                updateAdminPage();
                break;
            case "FACULTY":
                updateFacultyPage();
                break;
            case "STUDENT":
                updateStudentPage();
                break;
            default:
                hideHeroCards();
                break;
        }
    }

    private void updateAdminPage() {
        switch (activePage) {
            case "Home":
                setGreeting("Welcome, " + name, "Monitor exams, violations, and manage users and system.");
                setHeroCards(
                        new HeroCardData("Active Exams", "24"),
                        new HeroCardData("Users", "1,240"),
                        new HeroCardData("Alerts", "8")
                );
                loadContent("/fxml/admin/admin-dashboard.fxml");
                break;

            case "Users":
                setGreeting("User Management", "Manage faculty, student, and admin access.");
                setHeroCards(
                        new HeroCardData("Admins", "0"),
                        new HeroCardData("Faculty", "0"),
                        new HeroCardData("Students", "0"),
                        new HeroCardData("Blocked", "0"),
                        new HeroCardData("Active", "0")
                );
                loadContent("/fxml/admin/admin-users.fxml");
                break;

            case "Exams":
                setGreeting("Exam Management", "Create, schedule, and monitor examination activities.");
                setHeroCards(
                        new HeroCardData("Active Exams", "12"),
                        new HeroCardData("Scheduled", "18"),
                        new HeroCardData("Completed", "94")
                );
                loadContent("/fxml/exam/exam-management.fxml");
                break;

            case "Violations":
                setGreeting("Violation Monitoring", "Review suspicious activity and flagged sessions.");
                setHeroCards(
                        new HeroCardData("Open Alerts", "8"),
                        new HeroCardData("Pending", "14"),
                        new HeroCardData("Resolved", "67")
                );
                loadContent("/fxml/admin/admin-violations.fxml");
                break;

            case "Reports":
                setGreeting("Reports", "Analyze exam results, security trends, and performance.");
                setHeroCards(
                        new HeroCardData("Generated", "28"),
                        new HeroCardData("Monthly", "12"),
                        new HeroCardData("Archived", "143")
                );
                loadContent("/fxml/admin/admin-reports.fxml");
                break;

            case "Settings":
                setGreeting("System Settings", "Configure security, thresholds, and platform behavior.");
                setHeroCards(
                        new HeroCardData("Policies", "6"),
                        new HeroCardData("Thresholds", "12"),
                        new HeroCardData("Modules", "9")
                );
                loadContent("/fxml/admin/admin-settings.fxml");
                break;

            default:
                hideHeroCards();
                break;
        }
    }

    private void updateFacultyPage() {
        switch (activePage) {
            case "Home":
                setGreeting("Welcome, " + name, "Manage active exams, submissions, and integrity reviews.");
                setHeroCards(
                        new HeroCardData("Active Exams", "..."),
                        new HeroCardData("Students", "..."),
                        new HeroCardData("Submitted", "..."),
                        new HeroCardData("Needs Review", "...")
                );
                loadContent("/fxml/faculty/faculty-dashboard.fxml");
                break;

            case "Exams":
                setGreeting("My Exams", "View and manage your created examinations.");
                setHeroCards(
                        new HeroCardData("Drafts", "3"),
                        new HeroCardData("Published", "8"),
                        new HeroCardData("Completed", "15")
                );
                loadContent("/fxml/exam/exam-management.fxml");
                break;

            case "Students":
                setGreeting("Student Monitoring", "Track student activity and performance during exams.");
                setHeroCards(
                        new HeroCardData("Assigned", "214"),
                        new HeroCardData("Online", "38"),
                        new HeroCardData("Flagged", "4")
                );
                loadContent("/fxml/faculty/faculty-students.fxml");
                break;

            case "Reports":
                setGreeting("Reports", "Check summaries, scores, and exam analytics.");
                setHeroCards(
                        new HeroCardData("Reports", "9"),
                        new HeroCardData("Average Score", "87%"),
                        new HeroCardData("Pass Rate", "91%")
                );
                loadContent("/fxml/faculty/faculty-reports.fxml");
                break;

            case "Settings":
                setGreeting("Settings", "Adjust your preferences and exam configurations.");
                setHeroCards(
                        new HeroCardData("Exam Rules", "5"),
                        new HeroCardData("Templates", "4"),
                        new HeroCardData("Preferences", "8")
                );
                loadContent("/fxml/faculty/faculty-settings.fxml");
                break;

            default:
                hideHeroCards();
                break;
        }
    }

    private void updateStudentPage() {
        switch (activePage) {
            case "Home":
                setGreeting("Welcome, " + name, null);
                loadContent("/fxml/student/student-dashboard.fxml");
                break;

            case "Exams":
                setGreeting("My Exams", "See your available and ongoing examinations.");
                loadContent("/fxml/student/student-exam.fxml");
                break;

            case "Results":
                setGreeting("Results", "Review completed exams and performance summaries.");
                setHeroCards(
                        new HeroCardData("Passed", "10"),
                        new HeroCardData("Failed", "2"),
                        new HeroCardData("Average", "89%")
                );
                loadContent("/fxml/student/student-results.fxml");
                break;

            default:
                hideHeroCards();
                break;
        }
    }

    private Map<String, String> getPagesForRole() {
        Map<String, String> pages = new LinkedHashMap<>();

        switch (currentRole) {
            case "ADMIN":
                pages.put("Home", "");
                pages.put("Users", "");
                pages.put("Exams", "");
                pages.put("Violations", "");
                pages.put("Reports", "");
                pages.put("Settings", "");
                break;

            case "FACULTY":
                pages.put("Home", "");
                pages.put("Exams", "");
                pages.put("Students", "");
                pages.put("Reports", "");
                pages.put("Settings", "");
                break;

            case "STUDENT":
                pages.put("Home", "");
                pages.put("Exams", "");
                pages.put("Results", "");
                break;
        }

        return pages;
    }

    @FXML
    private void handleViewProfile() {
        if (com.example.examguard.utility.Session.isMustChangePassword()) {
            setGreeting("Change Password Required", "You must update your password before accessing other features.");
            activePage = null;
            buildNavigation();
            hideHeroCards();
            loadContent("/fxml/common/change-password.fxml");
            return;
        }

        setGreeting("Profile Settings", "Manage your account details and password.");
        activePage = null;
        buildNavigation();
        hideHeroCards();
        loadContent("/fxml/common/profile-settings.fxml");
    }

    @FXML
    private void handleChangePasswordMenu() {
        activePage = null;
        buildNavigation();

        setGreeting("Change Password", "Update your account password securely.");
        hideHeroCards();
        loadContent("/fxml/common/change-password.fxml");
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                String token = com.example.examguard.utility.Session.getSessionToken();

                if (token != null) {
                    new com.example.examguard.service.AuthApiService().logout(token);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            com.example.examguard.utility.Session.clear();
            com.example.examguard.utility.SceneManager.switchScene("auth/login.fxml");
        }
    }

    public void setHeroCards(HeroCardData... cards) {
        heroCardsContainer.getChildren().clear();

        if (cards == null || cards.length == 0) {
            hideHeroCards();
            return;
        }

        showHeroCards();

        for (HeroCardData card : cards) {
            VBox cardBox = new VBox();
            cardBox.setAlignment(Pos.CENTER_LEFT);
            cardBox.setSpacing(10);
            cardBox.setPrefWidth(150);
            cardBox.setStyle(
                    "-fx-background-color: rgba(255,255,255,0.14);" +
                            "-fx-background-radius: 18;" +
                            "-fx-border-color: rgba(255,255,255,0.16);" +
                            "-fx-border-radius: 18;" +
                            "-fx-padding: 16;"
            );

            Label titleLabel = new Label(card.getTitle());
            titleLabel.setStyle(
                    "-fx-font-size: 14px;" +
                            "-fx-text-fill: rgba(255,255,255,0.75);" +
                            "-fx-font-family: 'Segoe UI';"
            );

            Label valueLabel = new Label(card.getValue());
            valueLabel.setStyle(
                    "-fx-font-size: 26px;" +
                            "-fx-font-weight: bold;" +
                            "-fx-text-fill: white;" +
                            "-fx-font-family: 'Segoe UI';"
            );

            cardBox.getChildren().addAll(titleLabel, valueLabel);
            heroCardsContainer.getChildren().add(cardBox);
        }
    }

    public void openExamManagementWorkspace(Long examId) {
        try {
            activePage = "Exams";
            buildNavigation();
            setGreeting("My Exams", "View and manage your created examinations.");
            setHeroCards(
                    new HeroCardData("Drafts", "3"),
                    new HeroCardData("Published", "8"),
                    new HeroCardData("Completed", "15")
            );

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/exam/exam-management.fxml")
            );

            Parent content = loader.load();

            Object controller = loader.getController();

            if (controller instanceof ShellAwareController shellAwareController) {
                shellAwareController.setShellController(this);
            }

            contentHolder.setPadding(new Insets(16, 16, 16, 16));
            contentHolder.setStyle(
                    "-fx-padding: 16 16 16 16;" +
                            "-fx-background-color: rgb(251, 249, 248);"
            );
            contentHolder.getChildren().setAll(content);

            if (controller instanceof ExamManagementController examController) {
                Platform.runLater(() -> examController.openWorkspaceFromDashboard(examId));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class HeroCardData {
        private final String title;
        private final String value;

        public HeroCardData(String title, String value) {
            this.title = title;
            this.value = value;
        }

        public String getTitle() {
            return title;
        }

        public String getValue() {
            return value;
        }
    }

    public void hideHeroCards() {
        heroCardsContainer.setVisible(false);
        heroCardsContainer.setManaged(false);
    }

    public void showHeroCards() {
        heroCardsContainer.setVisible(true);
        heroCardsContainer.setManaged(true);
    }

    public void hideHeroSection() {
        heroPane.setVisible(false);
        heroPane.setManaged(false);
    }

    public void showHeroSection() {
        heroPane.setVisible(true);
        heroPane.setManaged(true);
    }



    public void updateHeroCards(HeroCardData... cards) {
        setHeroCards(cards);
    }
}