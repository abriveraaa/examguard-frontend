package com.example.examguard.controller.admin;

import com.example.examguard.model.admin.monitoring.AdminLogRowDto;
import com.example.examguard.model.admin.monitoring.ChartPointDto;
import com.example.examguard.model.admin.monitoring.MetricCardDto;
import com.example.examguard.model.admin.monitoring.MonitoringOverviewResponse;
import com.example.examguard.model.admin.monitoring.AdminMonitoringLogsResponse;
import com.example.examguard.service.AdminApiService;
import com.example.examguard.utility.LoadingSpinner;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class AdminMonitoringController {

    @FXML private ComboBox<String> academicYearComboBox;
    @FXML private ComboBox<String> termComboBox;
    @FXML private ComboBox<String> rangeComboBox;
    @FXML private DatePicker customStartDatePicker;
    @FXML private DatePicker customEndDatePicker;

    @FXML private ComboBox<String> concurrentTimeScaleComboBox;
    @FXML private ComboBox<String> violationDistributionGroupComboBox;
    @FXML private ComboBox<String> activityVolumeGroupComboBox;
    @FXML private ComboBox<String> violationsByProgramGroupComboBox;

    @FXML private PieChart violationDistributionChart;
    @FXML private BarChart<String, Number> activityVolumeChart;
    @FXML private BarChart<String, Number> violationsByProgramChart;

    @FXML private Label concurrentUsersLabel;
    @FXML private Label violationLogsLabel;
    @FXML private Label systemLogsLabel;
    @FXML private Label activeSessionsLabel;

    @FXML private Button overviewTabButton;
    @FXML private Button allLogsTabButton;
    @FXML private Button violationLogsTabButton;
    @FXML private Button systemLogsTabButton;
    @FXML private Button sessionsTabButton;

    @FXML private Button accessLogsTabButton;
    @FXML private Button accountLogsTabButton;
    @FXML private Button registrarLogsTabButton;

    @FXML private VBox overviewPane;
    @FXML private VBox allLogsPane;
    @FXML private VBox violationLogsPane;
    @FXML private VBox systemLogsPane;
    @FXML private VBox sessionsPane;
    @FXML private VBox accessLogsPane;
    @FXML private VBox accountLogsPane;
    @FXML private VBox registrarLogsPane;

    @FXML private LineChart<String, Number> concurrentUsersChart;
    @FXML private PieChart logDistributionChart;

    @FXML private ListView<String> recentViolationsListView;
    @FXML private ListView<String> recentSystemEventsListView;

    @FXML private TextField allLogsSearchField;
    @FXML private TextField violationSearchField;
    @FXML private TextField systemSearchField;
    @FXML private TextField sessionSearchField;
    @FXML private TextField accessSearchField;
    @FXML private TextField accountSearchField;
    @FXML private TextField registrarSearchField;

    @FXML private ComboBox<String> violationTypeFilterComboBox;
    @FXML private ComboBox<String> systemModuleFilterComboBox;

    @FXML private TableView<AdminLogRowDto> allLogsTable;
    @FXML private TableView<AdminLogRowDto> violationLogsTable;
    @FXML private TableView<AdminLogRowDto> systemLogsTable;
    @FXML private TableView<AdminLogRowDto> sessionLogsTable;
    @FXML private TableView<AdminLogRowDto> accessLogsTable;
    @FXML private TableView<AdminLogRowDto> accountLogsTable;
    @FXML private TableView<AdminLogRowDto> registrarLogsTable;

    @FXML private Label allLogsPageLabel;
    @FXML private Button allLogsPrevButton;
    @FXML private Button allLogsNextButton;

    @FXML private Label violationLogsPageLabel;
    @FXML private Button violationLogsPrevButton;
    @FXML private Button violationLogsNextButton;

    @FXML private Label systemLogsPageLabel;
    @FXML private Button systemLogsPrevButton;
    @FXML private Button systemLogsNextButton;

    @FXML private Label sessionLogsPageLabel;
    @FXML private Button sessionLogsPrevButton;
    @FXML private Button sessionLogsNextButton;

    @FXML private Label accessLogsPageLabel;
    @FXML private Button accessLogsPrevButton;
    @FXML private Button accessLogsNextButton;

    @FXML private Label accountLogsPageLabel;
    @FXML private Button accountLogsPrevButton;
    @FXML private Button accountLogsNextButton;

    @FXML private Label registrarLogsPageLabel;
    @FXML private Button registrarLogsPrevButton;
    @FXML private Button registrarLogsNextButton;

    @FXML private Label reactivationLogsPageLabel;
    @FXML private Button reactivationLogsPrevButton;
    @FXML private Button reactivationLogsNextButton;

    @FXML private TableColumn<AdminLogRowDto, String> allLogTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogTypeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogActorRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogModuleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogActionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogMessageColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogTargetUserColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogTargetRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> allLogDurationColumn;

    @FXML private TableColumn<AdminLogRowDto, String> violationTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationStudentColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationCourseColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationExamTitleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationQuestionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationTypeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationSeverityColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> violationMessageColumn;

    @FXML private TableColumn<AdminLogRowDto, String> systemTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemTargetUserColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemTargetRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemModuleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemActionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemMessageColumn;
    @FXML private TableColumn<AdminLogRowDto, String> systemDurationColumn;

    @FXML private TableColumn<AdminLogRowDto, String> sessionStartedColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionUserColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionDeviceColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionIpColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> sessionLastSeenColumn;

    @FXML private TableColumn<AdminLogRowDto, String> accessTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accessActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accessActionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accessStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accessMessageColumn;

    @FXML private TableColumn<AdminLogRowDto, String> accountTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accountActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accountActionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accountStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> accountMessageColumn;

    @FXML private TableColumn<AdminLogRowDto, String> registrarTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> registrarActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> registrarActionColumn;
    @FXML private TableColumn<AdminLogRowDto, String> registrarStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> registrarMessageColumn;

    @FXML private TableColumn<AdminLogRowDto, String> reactivationTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> reactivationActorColumn;
    @FXML private TableColumn<AdminLogRowDto, String> reactivationRoleColumn;
    @FXML private TableColumn<AdminLogRowDto, String> reactivationStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> reactivationMessageColumn;

    private final AdminApiService adminApiService = new AdminApiService();

    private int allLogsPage = 0;
    private int violationLogsPage = 0;
    private int systemLogsPage = 0;
    private int sessionLogsPage = 0;
    private int accessLogsPage = 0;
    private int accountLogsPage = 0;
    private int registrarLogsPage = 0;
    private int reactivationLogsPage = 0;


    private int allLogsTotalPages = 1;
    private int violationLogsTotalPages = 1;
    private int systemLogsTotalPages = 1;
    private int sessionLogsTotalPages = 1;
    private int accessLogsTotalPages = 1;
    private int accountLogsTotalPages = 1;
    private int registrarLogsTotalPages = 1;
    private int reactivationLogsTotalPages = 1;

    private long allLogsTotalElements = 0;
    private long violationLogsTotalElements = 0;
    private long systemLogsTotalElements = 0;
    private long sessionLogsTotalElements = 0;
    private long accessLogsTotalElements = 0;
    private long accountLogsTotalElements = 0;
    private long registrarLogsTotalElements = 0;
    private long reactivationLogsTotalElements = 0;

    private static final int PAGE_SIZE = 20;
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");

    private static final DateTimeFormatter LOG_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    public void initialize() {
        setupFilters();
        updateCustomRangeVisibility();

        setupPlaceholderData();
        setupLogTables();
        setupSearchListeners();

        loadOverviewAsync();
        loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);

        setupLogTables();

        setupTableResizePolicies();

        setupBadgeColumns();

        setupSearchListeners();
        initializePaginationLabels();
    }

    private void setupLogTables() {

        allLogTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        allLogTypeColumn.setCellValueFactory(c -> text(c.getValue().getSource()));
        allLogActorColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        allLogActorRoleColumn.setCellValueFactory(c -> text(c.getValue().getActorRole()));
        allLogModuleColumn.setCellValueFactory(c -> text(c.getValue().getModule()));
        allLogActionColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        allLogStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        allLogMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));
        allLogTargetUserColumn.setCellValueFactory(c -> text(c.getValue().getTargetUserId()));
        allLogTargetRoleColumn.setCellValueFactory(c -> text(c.getValue().getTargetRole()));
        allLogDurationColumn.setCellValueFactory(c -> text(formatDuration(c.getValue().getDurationMs())));

        violationTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        violationStudentColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        violationCourseColumn.setCellValueFactory(c -> text(c.getValue().getCourseCode()));
        violationExamTitleColumn.setCellValueFactory(c -> text(c.getValue().getExamTitle()));
        violationQuestionColumn.setCellValueFactory(c -> text(c.getValue().getQuestionNumber()));
        violationTypeColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        violationSeverityColumn.setCellValueFactory(c -> text(c.getValue().getSeverity()));
        violationStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        violationMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));


        systemTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        systemActorColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        systemRoleColumn.setCellValueFactory(c -> text(c.getValue().getActorRole()));
        systemTargetUserColumn.setCellValueFactory(c -> text(c.getValue().getTargetUserId()));
        systemTargetRoleColumn.setCellValueFactory(c -> text(c.getValue().getTargetRole()));
        systemModuleColumn.setCellValueFactory(c -> text(c.getValue().getModule()));
        systemActionColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        systemStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        systemMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));
        systemTargetUserColumn.setCellValueFactory(c -> text(c.getValue().getTargetUserId()));
        systemTargetRoleColumn.setCellValueFactory(c -> text(c.getValue().getTargetRole()));
        systemDurationColumn.setCellValueFactory(c -> text(formatDuration(c.getValue().getDurationMs())));

        sessionStartedColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        sessionUserColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        sessionRoleColumn.setCellValueFactory(c -> text(c.getValue().getActorRole()));
        sessionDeviceColumn.setCellValueFactory(c -> text(c.getValue().getExamTitle()));
        sessionIpColumn.setCellValueFactory(c -> text(c.getValue().getCourseCode()));
        sessionStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        sessionLastSeenColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getEndedAt())));

        accessTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        accessActorColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        accessActionColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        accessStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        accessMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));

        accountTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        accountActorColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        accountActionColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        accountStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        accountMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));

        registrarTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        registrarActorColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        registrarActionColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        registrarStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        registrarMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));

    }

    private void initializePaginationLabels() {
        if (allLogsPageLabel != null) {
            allLogsPageLabel.setText("Page 1 of 1 • 0 records");
        }

        if (violationLogsPageLabel != null) {
            violationLogsPageLabel.setText("Page 1 of 1 • 0 records");
        }

        if (systemLogsPageLabel != null) {
            systemLogsPageLabel.setText("Page 1 of 1 • 0 records");
        }

        if (sessionLogsPageLabel != null) {
            sessionLogsPageLabel.setText("Page 1 of 1 • 0 records");
        }

        if (allLogsPrevButton != null) allLogsPrevButton.setDisable(true);
        if (allLogsNextButton != null) allLogsNextButton.setDisable(true);

        if (violationLogsPrevButton != null) violationLogsPrevButton.setDisable(true);
        if (violationLogsNextButton != null) violationLogsNextButton.setDisable(true);

        if (systemLogsPrevButton != null) systemLogsPrevButton.setDisable(true);
        if (systemLogsNextButton != null) systemLogsNextButton.setDisable(true);

        if (sessionLogsPrevButton != null) sessionLogsPrevButton.setDisable(true);
        if (sessionLogsNextButton != null) sessionLogsNextButton.setDisable(true);

        if (accessLogsPageLabel != null) accessLogsPageLabel.setText("Page 1 of 1 • 0 records");
        if (accountLogsPageLabel != null) accountLogsPageLabel.setText("Page 1 of 1 • 0 records");
        if (registrarLogsPageLabel != null) registrarLogsPageLabel.setText("Page 1 of 1 • 0 records");
        if (reactivationLogsPageLabel != null) reactivationLogsPageLabel.setText("Page 1 of 1 • 0 records");

        if (accessLogsPrevButton != null) accessLogsPrevButton.setDisable(true);
        if (accessLogsNextButton != null) accessLogsNextButton.setDisable(true);

        if (accountLogsPrevButton != null) accountLogsPrevButton.setDisable(true);
        if (accountLogsNextButton != null) accountLogsNextButton.setDisable(true);

        if (registrarLogsPrevButton != null) registrarLogsPrevButton.setDisable(true);
        if (registrarLogsNextButton != null) registrarLogsNextButton.setDisable(true);

        if (reactivationLogsPrevButton != null) reactivationLogsPrevButton.setDisable(true);
        if (reactivationLogsNextButton != null) reactivationLogsNextButton.setDisable(true);
    }

    private SimpleStringProperty text(Object value) {
        return new SimpleStringProperty(value == null ? "-" : String.valueOf(value));
    }

    private void loadLogsAsync(
            String source,
            TableView<AdminLogRowDto> table,
            TextField searchField,
            int page
    ) {
        Task<AdminMonitoringLogsResponse> task = new Task<>() {
            @Override
            protected AdminMonitoringLogsResponse call() throws Exception {
                return adminApiService.getLogs(
                        buildLogsRequest(source, page, searchField)
                );
            }
        };

        task.setOnSucceeded(e -> {
            AdminMonitoringLogsResponse response = task.getValue();

            if (response == null || response.getContent() == null) {
                table.setItems(FXCollections.observableArrayList());
                updatePaginationState(source, page, 1, 0, false);
                return;
            }

            table.setItems(FXCollections.observableArrayList(response.getContent()));

            updatePaginationState(
                    source,
                    response.getCurrentPage(),
                    response.getTotalPages(),
                    response.getTotalElements(),
                    response.isHasNext()
            );
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            showInfo("Failed to load " + source + " logs: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void updatePaginationState(
            String source,
            int currentPage,
            int totalPages,
            long totalElements,
            boolean hasNext
    ) {
        int safeTotalPages = Math.max(totalPages, 1);
        int displayPage = Math.min(currentPage + 1, safeTotalPages);

        String labelText = "Page " + displayPage + " of " + safeTotalPages +
                " • " + totalElements + " records";

        switch (source) {
            case "ALL" -> {
                allLogsPage = currentPage;
                allLogsTotalPages = safeTotalPages;
                allLogsTotalElements = totalElements;

                if (allLogsPageLabel != null) {
                    allLogsPageLabel.setText(labelText);
                }

                if (allLogsPrevButton != null) {
                    allLogsPrevButton.setDisable(currentPage <= 0);
                }

                if (allLogsNextButton != null) {
                    allLogsNextButton.setDisable(!hasNext);
                }
            }

            case "VIOLATION" -> {
                violationLogsPage = currentPage;
                violationLogsTotalPages = safeTotalPages;
                violationLogsTotalElements = totalElements;

                if (violationLogsPageLabel != null) {
                    violationLogsPageLabel.setText(labelText);
                }

                if (violationLogsPrevButton != null) {
                    violationLogsPrevButton.setDisable(currentPage <= 0);
                }

                if (violationLogsNextButton != null) {
                    violationLogsNextButton.setDisable(!hasNext);
                }
            }

            case "SYSTEM" -> {
                systemLogsPage = currentPage;
                systemLogsTotalPages = safeTotalPages;
                systemLogsTotalElements = totalElements;

                if (systemLogsPageLabel != null) {
                    systemLogsPageLabel.setText(labelText);
                }

                if (systemLogsPrevButton != null) {
                    systemLogsPrevButton.setDisable(currentPage <= 0);
                }

                if (systemLogsNextButton != null) {
                    systemLogsNextButton.setDisable(!hasNext);
                }
            }

            case "SESSION" -> {
                sessionLogsPage = currentPage;
                sessionLogsTotalPages = safeTotalPages;
                sessionLogsTotalElements = totalElements;

                if (sessionLogsPageLabel != null) {
                    sessionLogsPageLabel.setText(labelText);
                }

                if (sessionLogsPrevButton != null) {
                    sessionLogsPrevButton.setDisable(currentPage <= 0);
                }

                if (sessionLogsNextButton != null) {
                    sessionLogsNextButton.setDisable(!hasNext);
                }
            }

            case "ACCESS" -> {
                accessLogsPage = currentPage;
                accessLogsTotalPages = safeTotalPages;
                accessLogsTotalElements = totalElements;

                if (accessLogsPageLabel != null) accessLogsPageLabel.setText(labelText);
                if (accessLogsPrevButton != null) accessLogsPrevButton.setDisable(currentPage <= 0);
                if (accessLogsNextButton != null) accessLogsNextButton.setDisable(!hasNext);
            }
            case "ACCOUNT" -> {
                accountLogsPage = currentPage;
                accountLogsTotalPages = safeTotalPages;
                accountLogsTotalElements = totalElements;

                if (accountLogsPageLabel != null) accountLogsPageLabel.setText(labelText);
                if (accountLogsPrevButton != null) accountLogsPrevButton.setDisable(currentPage <= 0);
                if (accountLogsNextButton != null) accountLogsNextButton.setDisable(!hasNext);
            }
            case "REGISTRAR" -> {
                registrarLogsPage = currentPage;
                registrarLogsTotalPages = safeTotalPages;
                registrarLogsTotalElements = totalElements;

                if (registrarLogsPageLabel != null) registrarLogsPageLabel.setText(labelText);
                if (registrarLogsPrevButton != null) registrarLogsPrevButton.setDisable(currentPage <= 0);
                if (registrarLogsNextButton != null) registrarLogsNextButton.setDisable(!hasNext);
            }
            case "REACTIVATION" -> {
                reactivationLogsPage = currentPage;
                reactivationLogsTotalPages = safeTotalPages;
                reactivationLogsTotalElements = totalElements;

                if (reactivationLogsPageLabel != null) reactivationLogsPageLabel.setText(labelText);
                if (reactivationLogsPrevButton != null) reactivationLogsPrevButton.setDisable(currentPage <= 0);
                if (reactivationLogsNextButton != null) reactivationLogsNextButton.setDisable(!hasNext);
            }
        }
    }

    @FXML
    private void handleAllLogsPrevious() {
        if (allLogsPage <= 0) return;

        allLogsPage--;
        loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
    }

    @FXML
    private void handleAllLogsNext() {
        if (allLogsPage + 1 >= allLogsTotalPages) return;

        allLogsPage++;
        loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
    }

    @FXML
    private void handleViolationLogsPrevious() {
        if (violationLogsPage <= 0) return;

        violationLogsPage--;
        loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
    }

    @FXML
    private void handleViolationLogsNext() {
        if (violationLogsPage + 1 >= violationLogsTotalPages) return;

        violationLogsPage++;
        loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
    }

    @FXML
    private void handleSystemLogsPrevious() {
        if (systemLogsPage <= 0) return;

        systemLogsPage--;
        loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
    }

    @FXML
    private void handleSystemLogsNext() {
        if (systemLogsPage + 1 >= systemLogsTotalPages) return;

        systemLogsPage++;
        loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
    }

    @FXML
    private void handleSessionLogsPrevious() {
        if (sessionLogsPage <= 0) return;

        sessionLogsPage--;
        loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
    }

    @FXML
    private void handleSessionLogsNext() {
        if (sessionLogsPage + 1 >= sessionLogsTotalPages) return;

        sessionLogsPage++;
        loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
    }

    @FXML
    private void handleAccessLogsPrevious() {
        if (accessLogsPage <= 0) return;
        accessLogsPage--;
        loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
    }

    @FXML
    private void handleAccessLogsNext() {
        if (accessLogsPage + 1 >= accessLogsTotalPages) return;
        accessLogsPage++;
        loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
    }

    @FXML
    private void handleAccountLogsPrevious() {
        if (accountLogsPage <= 0) return;
        accountLogsPage--;
        loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
    }

    @FXML
    private void handleAccountLogsNext() {
        if (accountLogsPage + 1 >= accountLogsTotalPages) return;
        accountLogsPage++;
        loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
    }

    @FXML
    private void handleRegistrarLogsPrevious() {
        if (registrarLogsPage <= 0) return;
        registrarLogsPage--;
        loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
    }

    @FXML
    private void handleRegistrarLogsNext() {
        if (registrarLogsPage + 1 >= registrarLogsTotalPages) return;
        registrarLogsPage++;
        loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
    }


    private void loadOverviewAsync() {
        if (!isCustomRangeValid()) {
            return;
        }

        Task<MonitoringOverviewResponse> task = new Task<>() {
            @Override
            protected MonitoringOverviewResponse call() throws Exception {
                return adminApiService.getOverview(buildOverviewRequest());
            }
        };

        task.setOnSucceeded(e -> {
            MonitoringOverviewResponse response = task.getValue();
            applyOverviewResponse(response);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            ex.printStackTrace();
            showInfo("Failed to load monitoring overview: " + ex.getMessage());
        });

        new Thread(task).start();
    }

    private void applyOverviewResponse(MonitoringOverviewResponse response) {
        if (response == null) return;

        applySummaryCards(response.getSummaryCards());
        applyActivityVolumeChart(response.getActivityVolume());
        applyActivityVolumeBarChart(response.getActivityVolume());
        applyViolationDistributionChart(response.getViolationsByType());
        applyViolationsByProgramChart(response.getViolationsByProgram());
        applyRecentCriticalEvents(response.getRecentCriticalEvents());
    }

    private void applySummaryCards(List<MetricCardDto> cards) {
        if (cards == null) return;

        long activities = 0;
        long violations = 0;
        long critical = 0;

        for (MetricCardDto card : cards) {
            if (card.getLabel() == null) continue;

            String label = card.getLabel().toLowerCase();
            long value = card.getValue() == null ? 0 : card.getValue();

            if (label.contains("activity")) {
                activities = value;
            } else if (label.contains("violation")) {
                violations = value;
            } else if (label.contains("critical")) {
                critical = value;
            }
        }

        systemLogsLabel.setText(String.valueOf(activities));
        violationLogsLabel.setText(String.valueOf(violations));
        activeSessionsLabel.setText("0");
        concurrentUsersLabel.setText(String.valueOf(critical));
    }

    private void applyActivityVolumeChart(List<ChartPointDto> points) {
        if (concurrentUsersChart == null) return;

        concurrentUsersChart.getData().clear();

        if (points == null || points.isEmpty()) {
            XYChart.Series<String, Number> emptySeries = new XYChart.Series<>();
            emptySeries.setName("No Activity");
            emptySeries.getData().add(new XYChart.Data<>("No Data", 0));
            concurrentUsersChart.getData().add(emptySeries);
            return;
        }

        Map<String, XYChart.Series<String, Number>> seriesMap = new LinkedHashMap<>();

        for (ChartPointDto point : points) {
            String category = safe(point.getCategory());
            String label = safe(point.getLabel());
            long value = point.getValue() == null ? 0 : point.getValue();

            XYChart.Series<String, Number> series = seriesMap.computeIfAbsent(category, key -> {
                XYChart.Series<String, Number> s = new XYChart.Series<>();
                s.setName(key);
                return s;
            });

            series.getData().add(new XYChart.Data<>(label, value));
        }

        concurrentUsersChart.getData().addAll(seriesMap.values());
    }

    private void applyViolationDistributionChart(List<ChartPointDto> points) {
        if (violationDistributionChart == null) return;

        ObservableList<PieChart.Data> data = FXCollections.observableArrayList();

        if (points != null) {
            Map<String, Long> grouped = new LinkedHashMap<>();

            for (ChartPointDto point : points) {
                String label = safe(point.getLabel());
                long value = point.getValue() == null ? 0 : point.getValue();

                grouped.merge(label, value, Long::sum);
            }

            grouped.forEach((label, value) ->
                    data.add(new PieChart.Data(label, value))
            );
        }

        if (data.isEmpty()) {
            data.add(new PieChart.Data("No Data", 0));
        }

        violationDistributionChart.setData(data);
    }

    private void applyActivityVolumeBarChart(List<ChartPointDto> points) {
        if (activityVolumeChart == null) return;

        activityVolumeChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Activity Volume");

        if (points == null || points.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
            activityVolumeChart.getData().add(series);
            return;
        }

        Map<String, Long> grouped = new LinkedHashMap<>();

        for (ChartPointDto point : points) {
            String category = safe(point.getCategory());
            long value = point.getValue() == null ? 0 : point.getValue();

            grouped.merge(category, value, Long::sum);
        }

        grouped.forEach((label, value) ->
                series.getData().add(new XYChart.Data<>(label, value))
        );

        activityVolumeChart.getData().add(series);
    }

    private void applyViolationsByProgramChart(List<ChartPointDto> points) {
        if (violationsByProgramChart == null) return;

        violationsByProgramChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Violations by Program");

        if (points == null || points.isEmpty()) {
            series.getData().add(new XYChart.Data<>("No Data", 0));
            violationsByProgramChart.getData().add(series);
            return;
        }

        for (ChartPointDto point : points) {
            String label = safe(point.getLabel());
            long value = point.getValue() == null ? 0 : point.getValue();

            series.getData().add(new XYChart.Data<>(label, value));
        }

        violationsByProgramChart.getData().add(series);
    }

    private void applyRecentCriticalEvents(List<AdminLogRowDto> logs) {
        if (recentSystemEventsListView == null) return;

        ObservableList<String> items = FXCollections.observableArrayList();

        if (logs != null) {
            for (AdminLogRowDto log : logs) {
                items.add(
                        safe(log.getStartedAt()) + " • " +
                                safe(log.getModule()) + " • " +
                                safe(log.getAction()) + " • " +
                                safe(log.getMessage())
                );
            }
        }

        if (items.isEmpty()) {
            items.add("No recent critical events.");
        }

        recentSystemEventsListView.setItems(items);
    }

    private void setupFilters() {
        if (academicYearComboBox != null) {
            academicYearComboBox.setItems(FXCollections.observableArrayList(
                    "All Academic Years",
                    "2025-2026",
                    "2026-2027"
            ));
            academicYearComboBox.setValue("All Academic Years");
            academicYearComboBox.setOnAction(e -> reloadOverviewByFilters());
        }

        if (termComboBox != null) {
            termComboBox.setItems(FXCollections.observableArrayList(
                    "All Terms",
                    "1st Semester",
                    "2nd Semester",
                    "Summer"
            ));
            termComboBox.setValue("All Terms");
            termComboBox.setOnAction(e -> reloadOverviewByFilters());
        }

        if (rangeComboBox != null) {
            rangeComboBox.setItems(FXCollections.observableArrayList(
                    "Entire Period",
                    "Entire Term",
                    "This Month",
                    "Today",
                    "Custom Range"
            ));
            rangeComboBox.setValue("Entire Period");
            rangeComboBox.setOnAction(e -> {
                updateCustomRangeVisibility();
                reloadOverviewByFilters();
            });
        }

        setupChartDropdowns();

        if (customStartDatePicker != null) {
            customStartDatePicker.setOnAction(e -> reloadOverviewByFilters());
        }

        if (customEndDatePicker != null) {
            customEndDatePicker.setOnAction(e -> reloadOverviewByFilters());
        }

        if (violationTypeFilterComboBox != null) {
            violationTypeFilterComboBox.setItems(FXCollections.observableArrayList(
                    "All Violations",
                    "FOCUS_LOST",
                    "FULLSCREEN_EXIT",
                    "MULTIPLE_FACE",
                    "NO_FACE",
                    "LOOKING_AWAY"
            ));
            violationTypeFilterComboBox.setValue("All Violations");
        }

        if (systemModuleFilterComboBox != null) {
            systemModuleFilterComboBox.setItems(FXCollections.observableArrayList(
                    "All Modules",
                    "AUTH",
                    "USERS",
                    "EXAMS",
                    "REPORTS",
                    "MONITORING"
            ));
            systemModuleFilterComboBox.setValue("All Modules");
        }
    }

    private void setupChartDropdowns() {
        setupTimeScaleOptions();

        if (violationDistributionGroupComboBox != null) {
            violationDistributionGroupComboBox.setItems(FXCollections.observableArrayList(
                    "Violation Type"
            ));
            violationDistributionGroupComboBox.setValue("Violation Type");
        }

        if (activityVolumeGroupComboBox != null) {
            activityVolumeGroupComboBox.setItems(FXCollections.observableArrayList(
                    "Role"
            ));
            activityVolumeGroupComboBox.setValue("Role");
        }

        if (violationsByProgramGroupComboBox != null) {
            violationsByProgramGroupComboBox.setItems(FXCollections.observableArrayList(
                    "Program"
            ));
            violationsByProgramGroupComboBox.setValue("Program");
        }
    }

    private void setupTimeScaleOptions() {
        if (concurrentTimeScaleComboBox == null) return;

        String academicYear = valueOf(academicYearComboBox, "All Academic Years");
        String range = valueOf(rangeComboBox, "Entire Period");

        ObservableList<String> options;

        if ("All Academic Years".equals(academicYear)) {
            options = FXCollections.observableArrayList("Auto", "Year");
        } else if ("Today".equals(range)) {
            options = FXCollections.observableArrayList("Auto", "Hour");
        } else if ("This Month".equals(range) || "Custom Range".equals(range)) {
            options = FXCollections.observableArrayList("Auto", "Day", "Hour");
        } else {
            options = FXCollections.observableArrayList("Auto", "Month");
        }

        String previous = concurrentTimeScaleComboBox.getValue();

        concurrentTimeScaleComboBox.setItems(options);

        if (previous != null && options.contains(previous)) {
            concurrentTimeScaleComboBox.setValue(previous);
        } else {
            concurrentTimeScaleComboBox.setValue("Auto");
        }

        concurrentTimeScaleComboBox.setOnAction(e -> setupConcurrentUsersChart());
    }

    private void setupPlaceholderData() {
        concurrentUsersLabel.setText("0");
        violationLogsLabel.setText("0");
        systemLogsLabel.setText("0");
        activeSessionsLabel.setText("0");

        if (logDistributionChart != null) {
            setupLogDistributionChart();
        }

        recentViolationsListView.setItems(FXCollections.observableArrayList(
                "No violation logs loaded yet."
        ));

        recentSystemEventsListView.setItems(FXCollections.observableArrayList(
                "No system logs loaded yet."
        ));
    }

    private void reloadOverviewByFilters() {
        if (!isCustomRangeValid()) {
            return;
        }

        setupTimeScaleOptions();
        loadOverviewAsync();

        if (allLogsPane.isVisible()) {
            allLogsPage = 0;
            loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
        } else if (violationLogsPane.isVisible()) {
            violationLogsPage = 0;
            loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
        } else if (systemLogsPane.isVisible()) {
            systemLogsPage = 0;
            loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
        } else if (sessionsPane.isVisible()) {
            sessionLogsPage = 0;
            loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
        } else if (accessLogsPane.isVisible()) {
            accessLogsPage = 0;
            loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
        } else if (accountLogsPane.isVisible()) {
            accountLogsPage = 0;
            loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
        } else if (registrarLogsPane.isVisible()) {
            registrarLogsPage = 0;
            loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
        }
    }

    private void setupLogDistributionChart() {
        if (logDistributionChart == null) return;

        logDistributionChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Violations", 120),
                new PieChart.Data("System", 532),
                new PieChart.Data("Sessions", 2543)
        ));
    }

    private void setupConcurrentUsersChart() {
        if (concurrentUsersChart == null) return;

        concurrentUsersChart.getData().clear();

        String groupBy = resolveTimeScale();

        XYChart.Series<String, Number> adminSeries = new XYChart.Series<>();
        adminSeries.setName("Admin");

        XYChart.Series<String, Number> facultySeries = new XYChart.Series<>();
        facultySeries.setName("Faculty");

        XYChart.Series<String, Number> studentSeries = new XYChart.Series<>();
        studentSeries.setName("Student");

        for (String label : buildTimeLabels(groupBy)) {
            adminSeries.getData().add(new XYChart.Data<>(label, 0));
            facultySeries.getData().add(new XYChart.Data<>(label, 0));
            studentSeries.getData().add(new XYChart.Data<>(label, 0));
        }

        concurrentUsersChart.getData().addAll(adminSeries, facultySeries, studentSeries);
    }


    private void setupSearchListeners() {
        if (allLogsSearchField != null) {
            allLogsSearchField.setOnAction(e -> {
                allLogsPage = 0;
                loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
            });
        }

        if (violationSearchField != null) {
            violationSearchField.setOnAction(e -> {
                violationLogsPage = 0;
                loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
            });
        }

        if (systemSearchField != null) {
            systemSearchField.setOnAction(e -> {
                systemLogsPage = 0;
                loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
            });
        }

        if (sessionSearchField != null) {
            sessionSearchField.setOnAction(e -> {
                sessionLogsPage = 0;
                loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
            });
        }

        if (accessSearchField != null) {
            accessSearchField.setOnAction(e -> {
                accessLogsPage = 0;
                loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
            });
        }

        if (accountSearchField != null) {
            accountSearchField.setOnAction(e -> {
                accountLogsPage = 0;
                loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
            });
        }

        if (registrarSearchField != null) {
            registrarSearchField.setOnAction(e -> {
                registrarLogsPage = 0;
                loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
            });
        }
    }

    @FXML
    private void handleExportAllLogs() {
        showInfo("Export all logs coming next.");
    }

    @FXML
    private void handleExportViolationLogs() {
        showInfo("Export violation logs coming next.");
    }

    @FXML
    private void handleExportSystemLogs() {
        showInfo("Export system logs coming next.");
    }

    @FXML
    private void handleExportSessionLogs() {
        showInfo("Export session logs coming next.");
    }

    @FXML
    private void handleExportAccessLogs() {
        showInfo("Export access logs coming next.");
    }

    @FXML
    private void handleExportAccountLogs() {
        showInfo("Export account logs coming next.");
    }

    @FXML
    private void handleExportRegistrarLogs() {
        showInfo("Export registrar sync logs coming next.");
    }

    @FXML
    private void handleExportReactivationLogs() {
        showInfo("Export reactivation logs coming next.");
    }

    @FXML
    private void showOverviewPane() {
        showPane(overviewPane, overviewTabButton);
    }

    @FXML
    private void showAllLogsPane() {
        showPane(allLogsPane, allLogsTabButton);
        allLogsPage = 0;
        loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
    }

    @FXML
    private void showViolationLogsPane() {
        showPane(violationLogsPane, violationLogsTabButton);
        violationLogsPage = 0;
        loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
    }

    @FXML
    private void showSystemLogsPane() {
        showPane(systemLogsPane, systemLogsTabButton);
        systemLogsPage = 0;
        loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
    }

    @FXML
    private void showSessionsPane() {
        showPane(sessionsPane, sessionsTabButton);
        sessionLogsPage = 0;
        loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
    }

    @FXML
    private void showAccessLogsPane() {
        showPane(accessLogsPane, accessLogsTabButton);
        accessLogsPage = 0;
        loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
    }

    @FXML
    private void showAccountLogsPane() {
        showPane(accountLogsPane, accountLogsTabButton);
        accountLogsPage = 0;
        loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
    }

    @FXML
    private void showRegistrarLogsPane() {
        showPane(registrarLogsPane, registrarLogsTabButton);
        registrarLogsPage = 0;
        loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
    }


    @FXML
    private void handleRefresh() {
        loadOverviewAsync();

        if (allLogsPane.isVisible()) {
            loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
        } else if (violationLogsPane.isVisible()) {
            loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
        } else if (systemLogsPane.isVisible()) {
            loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
        } else if (sessionsPane.isVisible()) {
            loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
        } else if (accessLogsPane.isVisible()) {
            accessLogsPage = 0;
            loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
        } else if (accountLogsPane.isVisible()) {
            accountLogsPage = 0;
            loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
        } else if (registrarLogsPane.isVisible()) {
            registrarLogsPage = 0;
            loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
        }
    }


    // =============
    // HELPERS
    // =============
    private Map<String, Object> buildOverviewRequest() {
        Map<String, Object> body = new HashMap<>();

        body.put("academicYear", valueOf(academicYearComboBox, "All Academic Years"));
        body.put("term", valueOf(termComboBox, "All Terms"));
        body.put("range", valueOf(rangeComboBox, "This Month"));
        body.put("groupBy", valueOf(concurrentTimeScaleComboBox, "Auto"));
        body.put("programCode", "All Programs");
        body.put("collegeCode", "All Colleges");
        body.put("role", "All Roles");

        putCustomDates(body);

        return body;
    }

    private Map<String, Object> buildLogsRequest(String source, int page, TextField searchField) {
        Map<String, Object> body = new HashMap<>();

        body.put("page", page);
        body.put("size", PAGE_SIZE);
        body.put("source", source);
        body.put("range", valueOf(rangeComboBox, "This Month"));
        body.put("role", "All Roles");
        body.put("severity", "All Severities");
        body.put("search", searchField == null ? "" : searchField.getText());

        putCustomDates(body);

        return body;
    }

    private void putCustomDates(Map<String, Object> body) {
        if (!"Custom Range".equals(valueOf(rangeComboBox, "This Month"))) {
            return;
        }

        if (customStartDatePicker == null || customEndDatePicker == null) {
            return;
        }

        LocalDate start = customStartDatePicker.getValue();
        LocalDate end = customEndDatePicker.getValue();

        if (start == null || end == null) {
            return;
        }

        body.put("startDate", start.atStartOfDay(MANILA).toOffsetDateTime().toString());
        body.put("endDate", end.atTime(23, 59, 59).atZone(MANILA).toOffsetDateTime().toString());
    }

    private void showPane(VBox selectedPane, Button selectedButton) {
        VBox[] panes = {
                overviewPane,
                allLogsPane,
                violationLogsPane,
                systemLogsPane,
                sessionsPane,
                accessLogsPane,
                accountLogsPane,
                registrarLogsPane
        };

        Button[] buttons = {
                overviewTabButton,
                allLogsTabButton,
                violationLogsTabButton,
                systemLogsTabButton,
                sessionsTabButton,
                accessLogsTabButton,
                accountLogsTabButton,
                registrarLogsTabButton
        };

        for (VBox pane : panes) {
            pane.setVisible(false);
            pane.setManaged(false);
        }

        for (Button button : buttons) {
            button.getStyleClass().remove("side-tab-active");

            if (!button.getStyleClass().contains("side-tab")) {
                button.getStyleClass().add("side-tab");
            }
        }

        selectedPane.setVisible(true);
        selectedPane.setManaged(true);

        selectedButton.getStyleClass().remove("side-tab");
        selectedButton.getStyleClass().add("side-tab-active");
    }

    private void showInfo(String message) {
        new Alert(Alert.AlertType.INFORMATION, message).showAndWait();
    }

    private String resolveTimeScale() {
        String selected = valueOf(concurrentTimeScaleComboBox, "Auto");

        if (!"Auto".equals(selected)) {
            return selected;
        }

        String academicYear = valueOf(academicYearComboBox, "All Academic Years");
        String range = valueOf(rangeComboBox, "Entire Period");

        if ("All Academic Years".equals(academicYear)) {
            return "Year";
        }

        if ("Today".equals(range)) {
            return "Hour";
        }

        if ("This Month".equals(range) || "Custom Range".equals(range)) {
            return "Day";
        }

        return "Month";
    }

    private ObservableList<String> buildTimeLabels(String groupBy) {
        ObservableList<String> labels = FXCollections.observableArrayList();

        switch (groupBy) {
            case "Year" -> {
                labels.add("2023-2024");
                labels.add("2024-2025");
                labels.add("2025-2026");
                labels.add("2026-2027");
            }
            case "Month" -> {
                labels.addAll("Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");
            }
            case "Day" -> {
                for (int day = 1; day <= 31; day++) {
                    labels.add(String.valueOf(day));
                }
            }
            case "Hour" -> {
                for (int hour = 0; hour < 24; hour++) {
                    labels.add(String.format("%02d:00", hour));
                }
            }
        }

        return labels;
    }

    private String valueOf(ComboBox<String> comboBox, String fallback) {
        return comboBox == null || comboBox.getValue() == null
                ? fallback
                : comboBox.getValue();
    }

    private void updateCustomRangeVisibility() {
        boolean custom = "Custom Range".equals(valueOf(rangeComboBox, "Entire Period"));

        if (customStartDatePicker != null) {
            customStartDatePicker.setVisible(custom);
            customStartDatePicker.setManaged(custom);
        }

        if (customEndDatePicker != null) {
            customEndDatePicker.setVisible(custom);
            customEndDatePicker.setManaged(custom);
        }
    }

    private boolean isCustomRangeValid() {
        if (!"Custom Range".equals(valueOf(rangeComboBox, "Entire Period"))) {
            return true;
        }

        if (customStartDatePicker == null || customEndDatePicker == null) {
            return true;
        }

        if (customStartDatePicker.getValue() == null || customEndDatePicker.getValue() == null) {
            return false;
        }

        if (customEndDatePicker.getValue().isBefore(customStartDatePicker.getValue())) {
            showInfo("End date cannot be earlier than start date.");
            return false;
        }

        return true;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatDuration(Long durationMs) {

        if (durationMs == null || durationMs <= 0) {
            return "-";
        }

        long totalSeconds = durationMs / 1000;

        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }

        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }

        return seconds + "s";
    }

    private String formatDateTime(String value) {

        if (value == null || value.isBlank()) {
            return "-";
        }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(value);

            return odt.atZoneSameInstant(MANILA)
                    .format(LOG_DATE_TIME_FORMATTER);

        } catch (Exception ex) {
            return value;
        }
    }

    private void setupBadgeColumns() {

        setBadgeColumn(allLogTypeColumn, this::sourceBadgeClass);
        setBadgeColumn(allLogActionColumn, value -> "badge-neutral");

        setBadgeColumn(systemStatusColumn, this::statusBadgeClass);
        setBadgeColumn(systemActionColumn, value -> "badge-neutral");

        setBadgeColumn(violationSeverityColumn, this::severityBadgeClass);
        setBadgeColumn(violationStatusColumn, this::statusBadgeClass);
        setBadgeColumn(violationTypeColumn, value -> "badge-maroon");

        setBadgeColumn(sessionStatusColumn, this::statusBadgeClass);

        setBadgeColumn(accessStatusColumn, this::statusBadgeClass);
        setBadgeColumn(accountStatusColumn, this::statusBadgeClass);
        setBadgeColumn(registrarStatusColumn, this::statusBadgeClass);
        setBadgeColumn(reactivationStatusColumn, this::statusBadgeClass);
    }

    private void setBadgeColumn(
            TableColumn<AdminLogRowDto, String> column,
            java.util.function.Function<String, String> styleResolver
    ) {
        if (column == null) return;

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);

                if (empty || value == null || value.isBlank() || "-".equals(value)) {
                    setText(null);
                    getStyleClass().removeAll(
                            "badge-cell",
                            "badge-success",
                            "badge-failed",
                            "badge-warning",
                            "badge-critical",
                            "badge-maroon",
                            "badge-blue",
                            "badge-gold",
                            "badge-neutral"
                    );
                    return;
                }

                setText(value);
                setAlignment(Pos.CENTER);

                getStyleClass().removeAll(
                        "badge-cell",
                        "badge-success",
                        "badge-failed",
                        "badge-warning",
                        "badge-critical",
                        "badge-maroon",
                        "badge-blue",
                        "badge-gold",
                        "badge-neutral"
                );

                getStyleClass().add("badge-cell");
                getStyleClass().add(styleResolver.apply(value));
            }
        });
    }

    private String statusBadgeClass(String value) {
        if (value == null) return "badge-neutral";

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "SUCCESS", "ACTIVE", "APPROVED", "REVIEWED", "SUBMITTED" -> "badge-success";
            case "FAILED", "ERROR", "BLOCKED", "DEACTIVATED", "PENALIZED" -> "badge-failed";
            case "PENDING", "PENDING_REVIEW", "IN_PROGRESS", "STARTED", "AUTO_SUBMITTED" -> "badge-warning";
            case "CRITICAL" -> "badge-critical";
            default -> "badge-neutral";
        };
    }

    private String severityBadgeClass(String value) {
        if (value == null) return "badge-neutral";

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "LOW", "MINOR" -> "badge-gold";
            case "MEDIUM", "MODERATE" -> "badge-warning";
            case "HIGH", "MAJOR" -> "badge-failed";
            case "CRITICAL" -> "badge-critical";
            default -> "badge-neutral";
        };
    }

    private String sourceBadgeClass(String value) {
        if (value == null) return "badge-neutral";

        String normalized = value.trim().toUpperCase();

        return switch (normalized) {
            case "SYSTEM" -> "badge-blue";
            case "VIOLATION" -> "badge-maroon";
            case "SESSION" -> "badge-gold";
            case "ACCESS" -> "badge-neutral";
            case "ACCOUNT", "REACTIVATION" -> "badge-warning";
            case "REGISTRAR" -> "badge-success";
            default -> "badge-neutral";
        };
    }

    private void setupTableResizePolicies() {
        setUnconstrainedResize(allLogsTable);
        setUnconstrainedResize(violationLogsTable);
        setUnconstrainedResize(systemLogsTable);
        setUnconstrainedResize(sessionLogsTable);
        setUnconstrainedResize(accessLogsTable);
        setUnconstrainedResize(accountLogsTable);
        setUnconstrainedResize(registrarLogsTable);
    }

    private void setUnconstrainedResize(TableView<AdminLogRowDto> table) {
        if (table == null) return;

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
}