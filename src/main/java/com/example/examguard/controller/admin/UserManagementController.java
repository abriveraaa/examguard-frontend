package com.example.examguard.controller.admin;

import com.example.examguard.controller.layout.DashboardShellController;
import com.example.examguard.controller.layout.ReactivationJustificationDialogController;
import com.example.examguard.controller.layout.ShellAwareController;
import com.example.examguard.model.admin.AdminUserResponse;
import com.example.examguard.model.core.UserManagementRow;
import com.example.examguard.model.core.response.FacultyUserResponse;
import com.example.examguard.model.core.response.ReactivationCandidateResponse;
import com.example.examguard.model.core.response.StudentProfileResponse;
import com.example.examguard.model.enums.UserType;
import com.example.examguard.service.AdminApiService;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.utility.Session;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class UserManagementController implements ShellAwareController {

    @FXML
    private Label adminCountLabel;
    @FXML
    private Label studentCountLabel;
    @FXML
    private Label facultyCountLabel;
    @FXML
    private Label lastSyncLabel;
    @FXML
    private StackPane overlayPane;

    @FXML
    private TextField adminSearchField;
    @FXML
    private ComboBox<String> adminStatusFilterComboBox;

    @FXML
    private TextField studentSearchField;
    @FXML
    private ComboBox<String> studentStatusFilterComboBox;
    @FXML
    private ComboBox<String> studentReactivationFilterComboBox;

    @FXML
    private TextField facultySearchField;
    @FXML
    private ComboBox<String> facultyStatusFilterComboBox;
    @FXML
    private ComboBox<String> facultyReactivationFilterComboBox;

    @FXML
    private TableView<UserManagementRow> adminTable;
    @FXML
    private TableColumn<UserManagementRow, String> adminSchoolIdColumn;
    @FXML
    private TableColumn<UserManagementRow, String> adminUsernameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> adminNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> adminEmailColumn;
    @FXML
    private TableColumn<UserManagementRow, String> adminRegistrarStatusColumn;
    @FXML
    private TableColumn<UserManagementRow, String> adminSystemAccessColumn;
    @FXML
    private TableColumn<UserManagementRow, Void> adminActionColumn;

    @FXML
    private TableView<UserManagementRow> studentTable;
    @FXML
    private TableColumn<UserManagementRow, String> studentSchoolIdColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentUsernameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentEmailColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentCollegeNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentProgramNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentYearLevelColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentSectionNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentRegistrarColumn;
    @FXML
    private TableColumn<UserManagementRow, String> studentSystemAccessColumn;
    @FXML
    private TableColumn<UserManagementRow, Void> studentActionColumn;

    @FXML
    private TableView<UserManagementRow> facultyTable;
    @FXML
    private TableColumn<UserManagementRow, String> facultySchoolIdColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultyUsernameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultyNameColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultyEmailColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultyCollegeColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultyRegistrarStatusColumn;
    @FXML
    private TableColumn<UserManagementRow, String> facultySystemAccessColumn;
    @FXML
    private TableColumn<UserManagementRow, Void> facultyActionColumn;

    private final AuthApiService authApiService = new AuthApiService();
    private final AdminApiService adminApiService = new AdminApiService();
    private final Gson gson = new Gson();

    private final ObservableList<UserManagementRow> adminData = FXCollections.observableArrayList();
    private final ObservableList<UserManagementRow> studentData = FXCollections.observableArrayList();
    private final ObservableList<UserManagementRow> facultyData = FXCollections.observableArrayList();
    private final ObservableList<ReactivationCandidateResponse> eligibleReactivationData = FXCollections.observableArrayList();

    private DashboardShellController shellController;

    private interface ApiCall {
        String call() throws Exception;
    }

    @FXML
    public void initialize() {
        loadLastSync();
        setupTables();
        setupActionColumns();
        setupCenteredColumns();
        setupTableItems();
        setupResizePolicies();
        setupFilterActions();

        refreshAllUsers();
        loadEligibleReactivationUsers();
    }

    @Override
    public void setShellController(DashboardShellController shellController) {
        this.shellController = shellController;
        pushHeroCounts();
    }

    private void refreshStatusFilters() {
        setupStatusFilter(adminStatusFilterComboBox, UserType.ADMIN);
        setupStatusFilter(studentStatusFilterComboBox, UserType.STUDENT);
        setupStatusFilter(facultyStatusFilterComboBox, UserType.FACULTY);

        setupReactivationFilter(studentReactivationFilterComboBox);
        setupReactivationFilter(facultyReactivationFilterComboBox);
    }

    private void setupTableItems() {
        adminTable.setItems(adminData);
        studentTable.setItems(studentData);
        facultyTable.setItems(facultyData);
    }

    private void setupFilterActions() {
        adminStatusFilterComboBox.setOnAction(e -> handleAdminSearch());
        studentStatusFilterComboBox.setOnAction(e -> filterByRole(UserType.STUDENT));
        facultyStatusFilterComboBox.setOnAction(e -> filterByRole(UserType.FACULTY));

        if (studentReactivationFilterComboBox != null) {
            studentReactivationFilterComboBox.setOnAction(e -> filterByRole(UserType.STUDENT));
        }

        if (facultyReactivationFilterComboBox != null) {
            facultyReactivationFilterComboBox.setOnAction(e -> filterByRole(UserType.FACULTY));
        }
    }

    private void setupResizePolicies() {
        adminTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        facultyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void refreshAllUsers() {
        loadUsers(UserType.ADMIN, AdminUserResponse.class, adminData);
        loadUsers(UserType.STUDENT, StudentProfileResponse.class, studentData);
        loadUsers(UserType.FACULTY, FacultyUserResponse.class, facultyData);
    }

    private void refreshNonAdminUsers() {
        loadUsers(UserType.STUDENT, StudentProfileResponse.class, studentData);
        loadUsers(UserType.FACULTY, FacultyUserResponse.class, facultyData);
    }

    private <T> void loadUsers(
            UserType type,
            Class<T> responseClass,
            ObservableList<UserManagementRow> target
    ) {
        runTask(
                () -> authApiService.getUsersByType(type),
                responseClass,
                target,
                type
        );
    }

    private <T> void runTask(
            ApiCall call,
            Class<T> clazz,
            ObservableList<UserManagementRow> target,
            UserType role
    ) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return call.call();
            }
        };

        task.setOnSucceeded(e -> {
            try {
                Type listType = TypeToken.getParameterized(List.class, clazz).getType();
                List<T> list = gson.fromJson(task.getValue(), listType);

                target.clear();

                if (list != null) {
                    for (T obj : list) {
                        UserManagementRow row = UserManagementRow.from(obj, role.name());

                        if (row != null) {
                            target.add(row);
                        }
                    }
                }

                updateCounts();
                pushHeroCounts();
                refreshStatusFilters();
                filterByRole(role);

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error loading " + role.name());
            }
        });

        task.setOnFailed(e -> {
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }

            showAlert("Backend error");
        });

        startDaemonThread(task);
    }

    private void startDaemonThread(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleAdminSearch() {
        filterByRole(UserType.ADMIN);
    }

    @FXML
    private void handleStudentSearch() {
        filterByRole(UserType.STUDENT);
    }

    @FXML
    private void handleFacultySearch() {
        filterByRole(UserType.FACULTY);
    }

    private void filterByRole(UserType role) {
        ObservableList<UserManagementRow> source = getDataByRole(role);
        TableView<UserManagementRow> table = getTableByRole(role);
        TextField searchField = getSearchFieldByRole(role);
        ComboBox<String> statusComboBox = getStatusComboBoxByRole(role);
        ComboBox<String> reactivationComboBox = getReactivationComboBoxByRole(role);

        String key = searchField == null || searchField.getText() == null
                ? ""
                : searchField.getText().toLowerCase(Locale.ROOT);

        String status = statusComboBox == null || statusComboBox.getValue() == null
                ? "All"
                : statusComboBox.getValue();

        String reactivation = reactivationComboBox == null || reactivationComboBox.getValue() == null
                ? "All"
                : reactivationComboBox.getValue();

        boolean hasReactivationFilter = role != UserType.ADMIN;

        if (key.isEmpty()
                && "All".equals(status)
                && (!hasReactivationFilter || "All".equals(reactivation))) {
            table.setItems(source);
            table.refresh();
            return;
        }

        table.setItems(FXCollections.observableArrayList(
                source.stream()
                        .filter(row -> matchesKeyword(row, key, role))
                        .filter(row -> matchesStatus(row, status))
                        .filter(row -> !hasReactivationFilter || matchesReactivation(row, reactivation, role))
                        .collect(Collectors.toList())
        ));

        table.refresh();
    }

    private boolean matchesKeyword(UserManagementRow row, String key, UserType role) {
        if (key == null || key.isEmpty()) return true;

        boolean commonMatch =
                contains(row.getSchoolId(), key)
                        || contains(row.getUsername(), key)
                        || contains(row.getFullName(), key)
                        || contains(row.getEmail(), key)
                        || contains(row.getCollegeName(), key);

        if (role == UserType.ADMIN || role == UserType.FACULTY) {
            return commonMatch;
        }

        return commonMatch
                || contains(row.getProgramName(), key)
                || contains(row.getYearLevel(), key)
                || contains(row.getSectionName(), key);
    }

    private boolean contains(String value, String key) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(key);
    }

    private boolean matchesStatus(UserManagementRow row, String status) {
        if ("All".equalsIgnoreCase(status)) return true;
        return status.equalsIgnoreCase(row.getRegistrarStatus());
    }

    private boolean matchesReactivation(UserManagementRow row, String reactivation, UserType role) {
        if ("All".equals(reactivation)) return true;

        if ("Pending Reactivation".equals(reactivation)) {
            return isEligibleForReactivation(row.getSchoolId(), role);
        }

        return true;
    }

    @FXML
    private void handleClearFilters() {
        clearTextField(adminSearchField);
        clearTextField(studentSearchField);
        clearTextField(facultySearchField);

        resetComboBox(adminStatusFilterComboBox);
        resetComboBox(studentStatusFilterComboBox);
        resetComboBox(facultyStatusFilterComboBox);
        resetComboBox(studentReactivationFilterComboBox);
        resetComboBox(facultyReactivationFilterComboBox);

        adminTable.setItems(adminData);
        studentTable.setItems(studentData);
        facultyTable.setItems(facultyData);

        refreshTables();
    }

    private void clearTextField(TextField textField) {
        if (textField != null) textField.clear();
    }

    private void resetComboBox(ComboBox<String> comboBox) {
        if (comboBox != null) comboBox.setValue("All");
    }

    private void refreshTables() {
        adminTable.refresh();
        studentTable.refresh();
        facultyTable.refresh();
    }

    @FXML
    private void handleRefreshAndSyncAccess() {
        showOverlay();

        runBackgroundAction(
                () -> adminApiService.refreshAndSyncRegistrarAccess(),
                response -> {
                    hideOverlay();
                    showAlert(response != null ? response : "Refresh and sync completed.");

                    refreshAllUsers();
                    loadEligibleReactivationUsers();
                    pushHeroCounts();

                    loadLastSync();
                },
                () -> {
                    hideOverlay();
                    showAlert("Error while refreshing and syncing access.");
                }
        );
    }

    private String formatDateTime(String isoDate) {
        try {
            if (isoDate == null || isoDate.isBlank()) {
                return "Never";
            }

            String cleaned = isoDate.trim().replace("\"", "");

            OffsetDateTime date = OffsetDateTime.parse(cleaned);

            ZonedDateTime phTime = date.atZoneSameInstant(ZoneId.of("Asia/Manila"));

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");

            return phTime.format(formatter);

        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid date";
        }
    }

    private void runBackgroundAction(
            ApiCall call,
            java.util.function.Consumer<String> onSuccess,
            Runnable onError
    ) {
        new Thread(() -> {
            try {
                String response = call.call();
                Platform.runLater(() -> onSuccess.accept(response));
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(onError);
            }
        }).start();
    }

    @FXML
    private void handleAddAdmin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/add-admin.fxml"));
            VBox root = loader.load();

            CreateAdminController controller = loader.getController();
            controller.setOnSuccess(() -> {
                loadUsers(UserType.ADMIN, AdminUserResponse.class, adminData);
                pushHeroCounts();
                showAlert("Admin profile created successfully.");
            });

            openDialog(root, "Add Admin", 450, 500);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open Add Admin dialog.");
        }
    }

    private void handleEditAdmin(UserManagementRow row) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/edit-admin.fxml"));
            VBox root = loader.load();

            EditAdminController controller = loader.getController();

            long activeAdmins = adminData.stream()
                    .filter(a -> "Active".equalsIgnoreCase(a.getRegistrarStatus()))
                    .filter(a -> "Active".equalsIgnoreCase(a.getSystemAccess()))
                    .count();

            controller.configure(
                    row,
                    Session.getUsername(),
                    (int) activeAdmins
            );

            controller.setOnSuccess(() -> {
                loadUsers(UserType.ADMIN, AdminUserResponse.class, adminData);
                pushHeroCounts();
                showAlert("Admin profile updated successfully.");
            });

            openDialog(root, "Edit Admin", 450, 500);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open Edit Admin dialog.");
        }
    }

    private void handleViewUser(UserManagementRow row, UserType role) {
        openUserViewDialog(row, role);
    }

    private void openUserViewDialog(UserManagementRow row, UserType role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/view-user.fxml"));
            Parent root = loader.load();

            UserViewDialogController controller = loader.getController();
            boolean eligible = isEligibleForReactivation(row.getSchoolId(), role);

            controller.setUserData(row, role.name(), eligible);

            openDialog(root, "View " + role.name(), 900, 600);

            if (role != UserType.ADMIN) {
                refreshNonAdminUsers();
                loadEligibleReactivationUsers();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open user view.");
        }
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

    private TableCell<UserManagementRow, Void> createActionCell(UserType role) {
        return new TableCell<>() {

            private final Button viewButton = new Button("View");
            private final Button editButton = new Button("Edit");

            private final HBox container = role == UserType.ADMIN
                    ? new HBox(8, viewButton, editButton)
                    : new HBox(8, viewButton);

            {
                container.setAlignment(Pos.CENTER);
                styleActionButton(viewButton, "#302C29", "white");
                styleActionButton(editButton, "#C9A227", "#302C29");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);

                UserManagementRow row = getTableRow() == null ? null : getTableRow().getItem();

                if (empty || row == null) {
                    setGraphic(null);
                    setText(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                    return;
                }

                viewButton.setOnAction(e -> handleViewUser(row, role));

                if (role == UserType.ADMIN) {
                    editButton.setOnAction(e -> handleEditAdmin(row));
                }

                setGraphic(container);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                setAlignment(Pos.CENTER);
            }
        };
    }

    private void styleActionButton(Button button, String backgroundColor, String textColor) {
        button.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
    }

    private void loadEligibleReactivationUsers() {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return adminApiService.getEligibleReactivationUsers();
            }
        };

        task.setOnSucceeded(e -> {
            try {
                Type listType = new TypeToken<List<ReactivationCandidateResponse>>() {
                }.getType();
                List<ReactivationCandidateResponse> list = gson.fromJson(task.getValue(), listType);

                eligibleReactivationData.clear();

                if (list != null) {
                    eligibleReactivationData.addAll(list);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Failed to load eligible reactivation users.");
            }
        });

        task.setOnFailed(e -> {
            if (task.getException() != null) {
                task.getException().printStackTrace();
            }

            showAlert("Backend error while loading reactivation list.");
        });

        startDaemonThread(task);
    }

    private boolean isEligibleForReactivation(String schoolId, UserType role) {
        return eligibleReactivationData.stream().anyMatch(item ->
                schoolId != null
                        && role != null
                        && schoolId.equalsIgnoreCase(item.getSchoolId())
                        && role.name().equalsIgnoreCase(item.getRole())
                        && Boolean.TRUE.equals(item.getEligibleForReactivation())
        );
    }

    private void updateCounts() {
        setLabelText(adminCountLabel, adminData.size());
        setLabelText(studentCountLabel, studentData.size());
        setLabelText(facultyCountLabel, facultyData.size());
    }

    private void setLabelText(Label label, int value) {
        if (label != null) {
            label.setText(String.valueOf(value));
        }
    }

    private void pushHeroCounts() {
        if (shellController == null) return;

        shellController.setHeroCards(
                new DashboardShellController.HeroCardData("Admins", String.valueOf(adminData.size())),
                new DashboardShellController.HeroCardData("Students", String.valueOf(studentData.size())),
                new DashboardShellController.HeroCardData("Faculty", String.valueOf(facultyData.size()))
        );
    }

    private void showOverlay() {
        setOverlayVisible(true);
    }

    private void hideOverlay() {
        setOverlayVisible(false);
    }

    private void setOverlayVisible(boolean visible) {
        if (overlayPane != null) {
            overlayPane.setVisible(visible);
            overlayPane.setManaged(visible);
        }
    }

    private void showAlert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg).showAndWait();
    }

    private String askForJustification(String description, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/admin/reactivation-justification.fxml"));
            Parent root = loader.load();

            ReactivationJustificationDialogController controller = loader.getController();
            controller.setDescription(description);

            openDialog(root, title, 450, 320);

            if (controller.isConfirmed()) {
                return controller.getJustification();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Failed to open justification dialog.");
        }

        return null;
    }

    @FXML
    private void handleStudentExport() {
        showAlert("Export coming soon");
    }

    @FXML
    private void handleFacultyExport() {
        showAlert("Export coming soon");
    }

    private void setupTables() {
        setupBasicColumns(
                adminSchoolIdColumn, adminUsernameColumn, adminNameColumn, adminEmailColumn,
                adminRegistrarStatusColumn, adminSystemAccessColumn
        );

        setupBasicColumns(
                studentSchoolIdColumn, studentUsernameColumn, studentNameColumn, studentEmailColumn,
                studentRegistrarColumn, studentSystemAccessColumn
        );

        setupBasicColumns(
                facultySchoolIdColumn, facultyUsernameColumn, facultyNameColumn, facultyEmailColumn,
                facultyRegistrarStatusColumn, facultySystemAccessColumn
        );

        studentCollegeNameColumn.setCellValueFactory(new PropertyValueFactory<>("collegeName"));
        studentProgramNameColumn.setCellValueFactory(new PropertyValueFactory<>("programName"));
        studentYearLevelColumn.setCellValueFactory(new PropertyValueFactory<>("yearLevel"));
        studentSectionNameColumn.setCellValueFactory(new PropertyValueFactory<>("sectionName"));
        facultyCollegeColumn.setCellValueFactory(new PropertyValueFactory<>("collegeName"));
    }

    private void setupBasicColumns(
            TableColumn<UserManagementRow, String> schoolId,
            TableColumn<UserManagementRow, String> username,
            TableColumn<UserManagementRow, String> name,
            TableColumn<UserManagementRow, String> email,
            TableColumn<UserManagementRow, String> registrarStatus,
            TableColumn<UserManagementRow, String> systemAccess
    ) {
        schoolId.setCellValueFactory(new PropertyValueFactory<>("schoolId"));
        username.setCellValueFactory(new PropertyValueFactory<>("username"));
        name.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        email.setCellValueFactory(new PropertyValueFactory<>("email"));
        registrarStatus.setCellValueFactory(new PropertyValueFactory<>("registrarStatus"));
        systemAccess.setCellValueFactory(new PropertyValueFactory<>("systemAccess"));
    }

    private void setupActionColumns() {
        adminActionColumn.setCellFactory(col -> createActionCell(UserType.ADMIN));
        studentActionColumn.setCellFactory(col -> createActionCell(UserType.STUDENT));
        facultyActionColumn.setCellFactory(col -> createActionCell(UserType.FACULTY));
    }

    private void setupFilters() {
        setupStatusFilter(adminStatusFilterComboBox, UserType.ADMIN);
        setupStatusFilter(studentStatusFilterComboBox, UserType.STUDENT);
        setupStatusFilter(facultyStatusFilterComboBox, UserType.FACULTY);

        setupReactivationFilter(studentReactivationFilterComboBox);
        setupReactivationFilter(facultyReactivationFilterComboBox);
    }

    private void setupStatusFilter(ComboBox<String> comboBox, UserType role) {
        if (comboBox == null) {
            return;
        }

        ObservableList<UserManagementRow> data = getDataByRole(role);

        ObservableList<String> statuses = data.stream()
                .map(UserManagementRow::getRegistrarStatus)
                .filter(status -> status != null && !status.isBlank() && !status.equals("-"))
                .distinct()
                .sorted()
                .collect(Collectors.toCollection(FXCollections::observableArrayList));

        statuses.add(0, "All");

        comboBox.setItems(statuses);
        comboBox.setValue("All");
    }

    private void setupReactivationFilter(ComboBox<String> comboBox) {
        if (comboBox != null) {
            comboBox.setItems(FXCollections.observableArrayList("All", "Pending Reactivation"));
            comboBox.setValue("All");
        }
    }

    private void loadLastSync() {
        runBackgroundAction(
                () -> adminApiService.getLastSuccessfulSync(),
                lastSync -> {
                    if (lastSync == null || lastSync.isBlank()) {
                        lastSyncLabel.setText("Last Sync: Never");
                    } else {
                        lastSyncLabel.setText("Last Sync: " + formatDateTime(lastSync));
                    }
                },
                () -> lastSyncLabel.setText("Last Sync: Never")
        );
    }

    private void setupCenteredColumns() {
        centerColumn(adminRegistrarStatusColumn);
        centerColumn(adminSystemAccessColumn);
        centerColumn(adminActionColumn);

        centerColumn(studentYearLevelColumn);
        centerColumn(studentSectionNameColumn);
        centerColumn(studentRegistrarColumn);
        centerColumn(studentSystemAccessColumn);
        centerColumn(studentActionColumn);

        centerColumn(facultyRegistrarStatusColumn);
        centerColumn(facultySystemAccessColumn);
        centerColumn(facultyActionColumn);
    }

    private void centerColumn(TableColumn<?, ?> column) {
        if (column != null) {
            column.setStyle("-fx-alignment: CENTER;");
        }
    }

    private ObservableList<UserManagementRow> getDataByRole(UserType role) {
        return switch (role) {
            case ADMIN -> adminData;
            case STUDENT -> studentData;
            case FACULTY -> facultyData;
        };
    }

    private TableView<UserManagementRow> getTableByRole(UserType role) {
        return switch (role) {
            case ADMIN -> adminTable;
            case STUDENT -> studentTable;
            case FACULTY -> facultyTable;
        };
    }

    private TextField getSearchFieldByRole(UserType role) {
        return switch (role) {
            case ADMIN -> adminSearchField;
            case STUDENT -> studentSearchField;
            case FACULTY -> facultySearchField;
        };
    }

    private ComboBox<String> getStatusComboBoxByRole(UserType role) {
        return switch (role) {
            case ADMIN -> adminStatusFilterComboBox;
            case STUDENT -> studentStatusFilterComboBox;
            case FACULTY -> facultyStatusFilterComboBox;
        };
    }

    private ComboBox<String> getReactivationComboBoxByRole(UserType role) {
        return switch (role) {
            case ADMIN -> null;
            case STUDENT -> studentReactivationFilterComboBox;
            case FACULTY -> facultyReactivationFilterComboBox;
        };
    }
}