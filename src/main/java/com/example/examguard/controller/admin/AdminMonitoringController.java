package com.example.examguard.controller.admin;

import com.example.examguard.cache.AdminLocalCacheKeys;
import com.example.examguard.cache.LocalCacheService;
import com.example.examguard.utility.Session;
import com.example.examguard.model.admin.monitoring.*;
import com.example.examguard.service.AdminApiService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileOutputStream;

public class AdminMonitoringController {

    private static final int PAGE_SIZE = 20;
    private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    private static final DateTimeFormatter LOG_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final AdminApiService adminApiService = new AdminApiService();
    private final LocalCacheService localCacheService = new LocalCacheService();

    private static final String ADMIN_MONITORING_CACHE_VERSION = "v1";

    @FXML private ComboBox<String> rangeComboBox;
    @FXML private ComboBox<String> violationSeverityFilterComboBox;
    @FXML private ComboBox<String> violationStatusFilterComboBox;
    @FXML private ComboBox<String> systemStatusFilterComboBox;
    @FXML private ComboBox<String> systemRoleFilterComboBox;
    @FXML private ComboBox<String> sessionStatusFilterComboBox;
    @FXML private ComboBox<String> sessionRoleFilterComboBox;
    @FXML private ComboBox<String> accessEventTypeFilterComboBox;
    @FXML private ComboBox<String> accessStatusFilterComboBox;
    @FXML private ComboBox<String> accessRoleFilterComboBox;
    @FXML private ComboBox<String> accountActionFilterComboBox;
    @FXML private ComboBox<String> accountStatusFilterComboBox;
    @FXML private ComboBox<String> accountRoleFilterComboBox;
    @FXML private ComboBox<String> registrarSyncTypeFilterComboBox;
    @FXML private ComboBox<String> registrarStatusFilterComboBox;

    @FXML private DatePicker customStartDatePicker;
    @FXML private DatePicker customEndDatePicker;
    @FXML private Button allLogsTabButton;
    @FXML private Button violationLogsTabButton;
    @FXML private Button systemLogsTabButton;
    @FXML private Button sessionsTabButton;
    @FXML private Button accessLogsTabButton;
    @FXML private Button accountLogsTabButton;
    @FXML private Button registrarLogsTabButton;
    @FXML private Button cameraSessionsTabButton;
    @FXML private VBox allLogsPane;
    @FXML private VBox violationLogsPane;
    @FXML private VBox systemLogsPane;
    @FXML private VBox sessionsPane;
    @FXML private VBox accessLogsPane;
    @FXML private VBox accountLogsPane;
    @FXML private VBox registrarLogsPane;
    @FXML private VBox cameraSessionsPane;
    @FXML private TextField allLogsSearchField;
    @FXML private TextField violationSearchField;
    @FXML private TextField systemSearchField;
    @FXML private TextField sessionSearchField;
    @FXML private TextField accessSearchField;
    @FXML private TextField accountSearchField;
    @FXML private TextField registrarSearchField;
    @FXML private TextField cameraSearchField;
    @FXML private ComboBox<String> violationTypeFilterComboBox;
    @FXML private ComboBox<String> systemModuleFilterComboBox;
    @FXML private ComboBox<String> cameraStatusFilterComboBox;
    @FXML private ComboBox<String> cameraDeviceTypeFilterComboBox;
    @FXML private ComboBox<String> cameraStreamRoleFilterComboBox;
    @FXML private TableView<AdminLogRowDto> allLogsTable;
    @FXML private TableView<AdminLogRowDto> violationLogsTable;
    @FXML private TableView<AdminLogRowDto> systemLogsTable;
    @FXML private TableView<AdminLogRowDto> sessionLogsTable;
    @FXML private TableView<AdminLogRowDto> accessLogsTable;
    @FXML private TableView<AdminLogRowDto> accountLogsTable;
    @FXML private TableView<AdminLogRowDto> registrarLogsTable;
    @FXML private TableView<AdminLogRowDto> cameraSessionsTable;
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
    @FXML private Label cameraSessionsPageLabel;
    @FXML private Button cameraSessionsPrevButton;
    @FXML private Button cameraSessionsNextButton;
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
    @FXML private TableColumn<AdminLogRowDto, String> cameraTimeColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraStudentColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraExamColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraAttemptColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraDeviceColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraStatusColumn;
    @FXML private TableColumn<AdminLogRowDto, String> cameraMessageColumn;

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
    private int cameraSessionsPage = 0;
    private int cameraSessionsTotalPages = 1;
    private long cameraSessionsTotalElements = 0;

    private boolean updatingFilterOptions = false;

    @FXML
    public void initialize() {
        setupFilters();
        updateCustomRangeVisibility();

        setupLogTables();
        setupTablePlaceholders();
        setupTableResizePolicies();
        setupBadgeColumns();
        setupSearchListeners();
        initializePaginationLabels();

        showAllLogsPane();
    }

    private void setTablePlaceholder(TableView<AdminLogRowDto> table, String message) {
        if (table == null) {
            return;
        }

        Label label = new Label(message);
        label.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");
        table.setPlaceholder(label);
    }

    private void setupTablePlaceholders() {
        setTablePlaceholder(allLogsTable, "No logs loaded yet.");
        setTablePlaceholder(violationLogsTable, "No violation logs loaded yet.");
        setTablePlaceholder(systemLogsTable, "No system logs loaded yet.");
        setTablePlaceholder(sessionLogsTable, "No session logs loaded yet.");
        setTablePlaceholder(accessLogsTable, "No access logs loaded yet.");
        setTablePlaceholder(accountLogsTable, "No account logs loaded yet.");
        setTablePlaceholder(registrarLogsTable, "No registrar logs loaded yet.");
        setTablePlaceholder(cameraSessionsTable, "No camera session logs loaded yet.");
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

        cameraTimeColumn.setCellValueFactory(c -> text(formatDateTime(c.getValue().getStartedAt())));
        cameraStudentColumn.setCellValueFactory(c -> text(c.getValue().getActorId()));
        cameraExamColumn.setCellValueFactory(c -> text(c.getValue().getExamId()));
        cameraAttemptColumn.setCellValueFactory(c -> text(c.getValue().getAttemptId()));
        cameraDeviceColumn.setCellValueFactory(c -> text(c.getValue().getAction()));
        cameraStatusColumn.setCellValueFactory(c -> text(c.getValue().getStatus()));
        cameraMessageColumn.setCellValueFactory(c -> text(c.getValue().getMessage()));
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
        if (cameraSessionsPageLabel != null) { cameraSessionsPageLabel.setText("Page 1 of 1 • 0 records");}

        if (accessLogsPrevButton != null) accessLogsPrevButton.setDisable(true);
        if (accessLogsNextButton != null) accessLogsNextButton.setDisable(true);

        if (accountLogsPrevButton != null) accountLogsPrevButton.setDisable(true);
        if (accountLogsNextButton != null) accountLogsNextButton.setDisable(true);

        if (registrarLogsPrevButton != null) registrarLogsPrevButton.setDisable(true);
        if (registrarLogsNextButton != null) registrarLogsNextButton.setDisable(true);

        if (reactivationLogsPrevButton != null) reactivationLogsPrevButton.setDisable(true);
        if (reactivationLogsNextButton != null) reactivationLogsNextButton.setDisable(true);

        if (cameraSessionsPrevButton != null) cameraSessionsPrevButton.setDisable(true);
        if (cameraSessionsNextButton != null) cameraSessionsNextButton.setDisable(true);

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
        Map<String, Object> request = buildLogsRequest(source, page, searchField);

        String cacheKey = AdminLocalCacheKeys.monitoringLogs(
                Session.getSchoolId(),
                source
        );

        setTablePlaceholder(table, "Loading " + source.toLowerCase() + " logs...");

        final AdminMonitoringLogsResponse cached =
                page == 0
                        ? localCacheService.loadData(
                        cacheKey,
                        AdminMonitoringLogsResponse.class
                )
                        : null;

        if (cached != null && cached.getContent() != null) {
            applyLogsResponse(source, table, cached);
            setTablePlaceholder(table, "Offline: showing cached " + source.toLowerCase() + " logs.");
        }

        Task<AdminMonitoringLogsResponse> task = new Task<>() {
            @Override
            protected AdminMonitoringLogsResponse call() throws Exception {
                return adminApiService.getLogs(request);
            }
        };

        task.setOnSucceeded(e -> {
            AdminMonitoringLogsResponse response = task.getValue();

            if (response == null || response.getContent() == null) {
                table.setItems(FXCollections.observableArrayList());
                setTablePlaceholder(table, "No " + source.toLowerCase() + " logs found.");
                updatePaginationState(source, page, 1, 0, false);
                return;
            }

            if (page == 0) {
                localCacheService.save(
                        cacheKey,
                        ADMIN_MONITORING_CACHE_VERSION,
                        response
                );
            }

            applyLogsResponse(source, table, response);

            if (response.getContent().isEmpty()) {
                setTablePlaceholder(table, "No " + source.toLowerCase() + " logs found.");
            }
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
            }

            if (cached == null || cached.getContent() == null || cached.getContent().isEmpty()) {
                table.setItems(FXCollections.observableArrayList());
                setTablePlaceholder(table, "Backend offline. No cached " + source.toLowerCase() + " logs available.");
                updatePaginationState(source, page, 1, 0, false);
            } else {
                setTablePlaceholder(table, "Backend offline. Showing cached " + source.toLowerCase() + " logs.");
            }
        });

        Thread thread = new Thread(task, "admin-monitoring-" + source.toLowerCase() + "-logs-thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyLogsResponse(
            String source,
            TableView<AdminLogRowDto> table,
            AdminMonitoringLogsResponse response
    ) {
        if (response == null || response.getContent() == null) {
            table.setItems(FXCollections.observableArrayList());
            updatePaginationState(source, 0, 1, 0, false);
            return;
        }

        table.setItems(FXCollections.observableArrayList(response.getContent()));

        populateFiltersFromOptions(source, response.getFilterOptions());

        updatePaginationState(
                source,
                response.getCurrentPage(),
                response.getTotalPages(),
                response.getTotalElements(),
                response.isHasNext()
        );
    }

    private void populateFiltersFromOptions(
            String source,
            AdminMonitoringFilterOptionsDto options
    ) {
        if (options == null) {
            return;
        }

        updatingFilterOptions = true;

        try {
            switch (source) {
                case "VIOLATION" -> {
                    setFilterItems(violationTypeFilterComboBox, "All Violations", options.getViolationTypes());
                    setFilterItems(violationSeverityFilterComboBox, "All Severities", options.getSeverities());
                    setFilterItems(violationStatusFilterComboBox, "All Statuses", options.getStatuses());
                }

                case "SYSTEM" -> {
                    setFilterItems(systemModuleFilterComboBox, "All Modules", options.getModules());
                    setFilterItems(systemStatusFilterComboBox, "All Statuses", options.getStatuses());
                    setFilterItems(systemRoleFilterComboBox, "All Roles", options.getRoles());
                }

                case "SESSION" -> {
                    setFilterItems(sessionStatusFilterComboBox, "All Statuses", options.getStatuses());
                    setFilterItems(sessionRoleFilterComboBox, "All Roles", options.getRoles());
                }

                case "ACCESS" -> {
                    setFilterItems(accessEventTypeFilterComboBox, "All Events", options.getActions());
                    setFilterItems(accessStatusFilterComboBox, "All Statuses", options.getStatuses());
                    setFilterItems(accessRoleFilterComboBox, "All Roles", options.getRoles());
                }

                case "ACCOUNT" -> {
                    setFilterItems(accountActionFilterComboBox, "All Actions", options.getActions());
                    setFilterItems(accountStatusFilterComboBox, "All Statuses", options.getStatuses());
                    setFilterItems(accountRoleFilterComboBox, "All Roles", options.getRoles());
                }

                case "REGISTRAR" -> {
                    setFilterItems(registrarSyncTypeFilterComboBox, "All Sync Types", options.getActions());
                    setFilterItems(registrarStatusFilterComboBox, "All Statuses", options.getStatuses());
                }

                case "CAMERA" -> {
                    setFilterItems(cameraStatusFilterComboBox, "All Statuses", options.getCameraStatuses());
                    setFilterItems(cameraDeviceTypeFilterComboBox, "All Devices", options.getCameraDeviceTypes());
                    setFilterItems(cameraStreamRoleFilterComboBox, "All Stream Roles", options.getCameraStreamRoles());
                }
            }
        } finally {
            updatingFilterOptions = false;
        }
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

            case "CAMERA" -> {
                cameraSessionsPage = currentPage;
                cameraSessionsTotalPages = safeTotalPages;
                cameraSessionsTotalElements = totalElements;

                if (cameraSessionsPageLabel != null) {
                    cameraSessionsPageLabel.setText(labelText);
                }

                if (cameraSessionsPrevButton != null) {
                    cameraSessionsPrevButton.setDisable(currentPage <= 0);
                }

                if (cameraSessionsNextButton != null) {
                    cameraSessionsNextButton.setDisable(!hasNext);
                }
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

    @FXML
    private void handleCameraSessionsPrevious() {
        if (cameraSessionsPage <= 0) return;

        cameraSessionsPage--;
        loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
    }

    @FXML
    private void handleCameraSessionsNext() {
        if (cameraSessionsPage + 1 >= cameraSessionsTotalPages) return;

        cameraSessionsPage++;
        loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
    }

    private void setupFilters() {

        if (rangeComboBox != null) {
            rangeComboBox.setItems(FXCollections.observableArrayList(
                    "Today",
                    "This Month",
                    "Entire Period",
                    "Custom Range"
            ));
            rangeComboBox.setValue("Today");
            rangeComboBox.setOnAction(e -> {
                updateCustomRangeVisibility();
                reloadVisibleTabFromFirstPage();
            });
        }

        setupFilterComboBox(violationTypeFilterComboBox, "All Violations");
        setupFilterComboBox(violationSeverityFilterComboBox, "All Severities");
        setupFilterComboBox(violationStatusFilterComboBox, "All Statuses");

        setupFilterComboBox(systemModuleFilterComboBox, "All Modules");
        setupFilterComboBox(systemStatusFilterComboBox, "All Statuses");
        setupFilterComboBox(systemRoleFilterComboBox, "All Roles");

        setupFilterComboBox(sessionStatusFilterComboBox, "All Statuses");
        setupFilterComboBox(sessionRoleFilterComboBox, "All Roles");

        setupFilterComboBox(accessEventTypeFilterComboBox, "All Events");
        setupFilterComboBox(accessStatusFilterComboBox, "All Statuses");
        setupFilterComboBox(accessRoleFilterComboBox, "All Roles");

        setupFilterComboBox(accountActionFilterComboBox, "All Actions");
        setupFilterComboBox(accountStatusFilterComboBox, "All Statuses");
        setupFilterComboBox(accountRoleFilterComboBox, "All Roles");

        setupFilterComboBox(registrarSyncTypeFilterComboBox, "All Sync Types");
        setupFilterComboBox(registrarStatusFilterComboBox, "All Statuses");

        setupFilterComboBox(cameraStatusFilterComboBox, "All Statuses");
        setupFilterComboBox(cameraDeviceTypeFilterComboBox, "All Devices");
        setupFilterComboBox(cameraStreamRoleFilterComboBox, "All Stream Roles");
    }

    private void setupFilterComboBox(ComboBox<String> comboBox, String defaultValue) {
        if (comboBox == null) return;

        comboBox.setItems(FXCollections.observableArrayList(defaultValue));
        comboBox.setValue(defaultValue);

        comboBox.setOnAction(e -> {
            if (updatingFilterOptions) {
                return;
            }

            reloadVisibleTabFromFirstPage();
        });
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

        if (cameraSearchField != null) {
            cameraSearchField.setOnAction(e -> {
                cameraSessionsPage = 0;
                loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
            });
        }
    }

    // ======================
    // EXPORT
    // ======================

    @FXML
    private void handleExportAllLogsPdf() {
        exportLogs("ALL", allLogsSearchField, "PDF");
    }

    @FXML
    private void handleExportAllLogsExcel() {
        exportLogs("ALL", allLogsSearchField, "EXCEL");
    }

    @FXML
    private void handleExportViolationLogsPdf() {
        exportLogs("VIOLATION", violationSearchField, "PDF");
    }

    @FXML
    private void handleExportViolationLogsExcel() {
        exportLogs("VIOLATION", violationSearchField, "EXCEL");
    }

    @FXML
    private void handleExportSystemLogsPdf() {
        exportLogs("SYSTEM", systemSearchField, "PDF");
    }

    @FXML
    private void handleExportSystemLogsExcel() {
        exportLogs("SYSTEM", systemSearchField, "EXCEL");
    }

    @FXML
    private void handleExportSessionLogsPdf() {
        exportLogs("SESSION", sessionSearchField, "PDF");
    }

    @FXML
    private void handleExportSessionLogsExcel() {
        exportLogs("SESSION", sessionSearchField, "EXCEL");
    }

    @FXML
    private void handleExportAccessLogsPdf() {
        exportLogs("ACCESS", accessSearchField, "PDF");
    }

    @FXML
    private void handleExportAccessLogsExcel() {
        exportLogs("ACCESS", accessSearchField, "EXCEL");
    }

    @FXML
    private void handleExportAccountLogsPdf() {
        exportLogs("ACCOUNT", accountSearchField, "PDF");
    }

    @FXML
    private void handleExportAccountLogsExcel() {
        exportLogs("ACCOUNT", accountSearchField, "EXCEL");
    }

    @FXML
    private void handleExportRegistrarLogsPdf() {
        exportLogs("REGISTRAR", registrarSearchField, "PDF");
    }

    @FXML
    private void handleExportRegistrarLogsExcel() {
        exportLogs("REGISTRAR", registrarSearchField, "EXCEL");
    }

    @FXML
    private void handleExportCameraSessionsPdf() {
        exportLogs("CAMERA", cameraSearchField, "PDF");
    }

    @FXML
    private void handleExportCameraSessionsExcel() {
        exportLogs("CAMERA", cameraSearchField, "EXCEL");
    }


    private void exportLogs(
            String source,
            TextField searchField,
            String format
    ) {
        if (!isCustomRangeValid()) {
            return;
        }

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                Map<String, Object> body =
                        buildExportRequest(
                                source,
                                searchField,
                                format
                        );

                return adminApiService.exportMonitoringLogs(body);
            }
        };

        task.setOnSucceeded(event -> {
            byte[] bytes = task.getValue();

            if (bytes == null || bytes.length == 0) {
                showInfo("No export data received.");
                return;
            }

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            buildExportFileName(source, format),
                            "EXCEL".equalsIgnoreCase(format)
                                    ? "Excel Files"
                                    : "PDF Files",
                            "EXCEL".equalsIgnoreCase(format)
                                    ? "*.xlsx"
                                    : "*.pdf"
                    );

            if (saved) {
                showInfo("Export completed successfully.");
            }
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();

            if (ex != null) {
                ex.printStackTrace();
                showInfo("Export failed: " + ex.getMessage());
            } else {
                showInfo("Export failed.");
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private boolean saveBytesToFile(
            byte[] bytes,
            String defaultFileName,
            String extensionDescription,
            String extensionPattern
    ) {
        try {
            FileChooser fileChooser = new FileChooser();

            fileChooser.setTitle("Save Export");

            fileChooser.setInitialFileName(defaultFileName);

            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter(
                            extensionDescription,
                            extensionPattern
                    )
            );

            Window owner = null;

            if (allLogsTable != null &&
                    allLogsTable.getScene() != null) {

                owner = allLogsTable
                        .getScene()
                        .getWindow();
            }

            File file =
                    fileChooser.showSaveDialog(owner);

            if (file == null) {
                return false;
            }

            try (FileOutputStream outputStream =
                         new FileOutputStream(file)) {

                outputStream.write(bytes);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Unable to save export: " + e.getMessage());
            return false;
        }
    }

    private String buildExportFileName(
            String source,
            String format
    ) {
        String normalizedSource =
                source == null
                        ? "all"
                        : source.toLowerCase();

        String extension =
                "EXCEL".equalsIgnoreCase(format)
                        ? "xlsx"
                        : "pdf";

        String timestamp =
                java.time.ZonedDateTime.now(MANILA)
                        .format(
                                DateTimeFormatter.ofPattern(
                                        "yyyyMMdd-HHmmss"
                                )
                        );

        return "examguard-monitoring-" +
                normalizedSource +
                "-logs-" +
                timestamp +
                "." +
                extension;
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
    private void showCameraSessionsPane() {
        showPane(cameraSessionsPane, cameraSessionsTabButton);
        cameraSessionsPage = 0;
        loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
    }

    private void reloadVisibleTabFromFirstPage() {

        if (!isCustomRangeValid()) {
            return;
        }

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

        } else if (cameraSessionsPane.isVisible()) {
            cameraSessionsPage = 0;
            loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
        }
    }


    @FXML
    private void handleRefresh() {
        reloadVisibleTabCurrentPage();
    }


    // =============
    // HELPERS
    // =============

    private void reloadVisibleTabCurrentPage() {
        if (!isCustomRangeValid()) {
            return;
        }

        if (allLogsPane.isVisible()) {
            loadLogsAsync("ALL", allLogsTable, allLogsSearchField, allLogsPage);
        } else if (violationLogsPane.isVisible()) {
            loadLogsAsync("VIOLATION", violationLogsTable, violationSearchField, violationLogsPage);
        } else if (systemLogsPane.isVisible()) {
            loadLogsAsync("SYSTEM", systemLogsTable, systemSearchField, systemLogsPage);
        } else if (sessionsPane.isVisible()) {
            loadLogsAsync("SESSION", sessionLogsTable, sessionSearchField, sessionLogsPage);
        } else if (accessLogsPane.isVisible()) {
            loadLogsAsync("ACCESS", accessLogsTable, accessSearchField, accessLogsPage);
        } else if (accountLogsPane.isVisible()) {
            loadLogsAsync("ACCOUNT", accountLogsTable, accountSearchField, accountLogsPage);
        } else if (registrarLogsPane.isVisible()) {
            loadLogsAsync("REGISTRAR", registrarLogsTable, registrarSearchField, registrarLogsPage);
        } else if (cameraSessionsPane.isVisible()) {
            loadLogsAsync("CAMERA", cameraSessionsTable, cameraSearchField, cameraSessionsPage);
        }
    }

    private Map<String, Object> buildExportRequest(String source, TextField searchField, String format) {
        Map<String, Object> body = buildLogsRequest(source, 0, searchField);

        body.put("page", 0);
        body.put("size", 100000);
        body.put("format", format);

        return body;
    }

    private Map<String, Object> buildLogsRequest(String source, int page, TextField searchField) {
        Map<String, Object> body = new HashMap<>();

        body.put("page", page);
        body.put("size", PAGE_SIZE);
        body.put("source", source);
        body.put("range", valueOf(rangeComboBox, "Entire Period"));
        body.put("search", searchField == null ? "" : searchField.getText());

        body.put("role", roleForSource(source));
        body.put("status", statusForSource(source));
        body.put("action", actionForSource(source));
        body.put("module", moduleForSource(source));
        body.put("severity", severityForSource(source));
        body.put("violationType", violationTypeForSource(source));

        body.put("cameraStatus", valueOf(cameraStatusFilterComboBox, "All Statuses"));
        body.put("cameraDeviceType", valueOf(cameraDeviceTypeFilterComboBox, "All Devices"));
        body.put("cameraStreamRole", valueOf(cameraStreamRoleFilterComboBox, "All Stream Roles"));

        putCustomDates(body);

        return body;
    }

    private String roleForSource(String source) {
        return switch (source) {
            case "SYSTEM" -> valueOf(systemRoleFilterComboBox, "All Roles");
            case "SESSION" -> valueOf(sessionRoleFilterComboBox, "All Roles");
            case "ACCESS" -> valueOf(accessRoleFilterComboBox, "All Roles");
            case "ACCOUNT" -> valueOf(accountRoleFilterComboBox, "All Roles");
            default -> "All Roles";
        };
    }

    private String statusForSource(String source) {
        return switch (source) {
            case "VIOLATION" -> valueOf(violationStatusFilterComboBox, "All Statuses");
            case "SYSTEM" -> valueOf(systemStatusFilterComboBox, "All Statuses");
            case "SESSION" -> valueOf(sessionStatusFilterComboBox, "All Statuses");
            case "ACCESS" -> valueOf(accessStatusFilterComboBox, "All Statuses");
            case "ACCOUNT" -> valueOf(accountStatusFilterComboBox, "All Statuses");
            case "REGISTRAR" -> valueOf(registrarStatusFilterComboBox, "All Statuses");
            case "CAMERA" -> valueOf(cameraStatusFilterComboBox, "All Statuses");
            default -> "All Statuses";
        };
    }

    private String actionForSource(String source) {
        return switch (source) {
            case "ACCESS" -> valueOf(accessEventTypeFilterComboBox, "All Events");
            case "ACCOUNT" -> valueOf(accountActionFilterComboBox, "All Actions");
            case "REGISTRAR" -> valueOf(registrarSyncTypeFilterComboBox, "All Sync Types");
            default -> "All Actions";
        };
    }

    private String moduleForSource(String source) {
        return "SYSTEM".equals(source)
                ? valueOf(systemModuleFilterComboBox, "All Modules")
                : "All Modules";
    }

    private String severityForSource(String source) {
        return "VIOLATION".equals(source)
                ? valueOf(violationSeverityFilterComboBox, "All Severities")
                : "All Severities";
    }

    private String violationTypeForSource(String source) {
        return "VIOLATION".equals(source)
                ? valueOf(violationTypeFilterComboBox, "All Violations")
                : "All Violations";
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
                allLogsPane,
                violationLogsPane,
                systemLogsPane,
                sessionsPane,
                accessLogsPane,
                accountLogsPane,
                registrarLogsPane,
                cameraSessionsPane
        };

        Button[] buttons = {
                allLogsTabButton,
                violationLogsTabButton,
                systemLogsTabButton,
                sessionsTabButton,
                accessLogsTabButton,
                accountLogsTabButton,
                registrarLogsTabButton,
                cameraSessionsTabButton
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
        setBadgeColumn(cameraStatusColumn, this::statusBadgeClass);
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
            case "SYSTEM", "CAMERA" -> "badge-blue";
            case "VIOLATION" -> "badge-maroon";
            case "SESSION" -> "badge-gold";
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
        setUnconstrainedResize(cameraSessionsTable);
    }

    private void setFilterItems(
            ComboBox<String> comboBox,
            String defaultValue,
            java.util.List<String> values
    ) {
        if (comboBox == null) {
            return;
        }

        String previousValue = comboBox.getValue();

        ObservableList<String> items =
                FXCollections.observableArrayList();

        items.add(defaultValue);

        if (values != null) {
            items.addAll(values);
        }

        comboBox.setItems(items);

        if (previousValue != null && items.contains(previousValue)) {
            if (!previousValue.equals(comboBox.getValue())) {
                comboBox.setValue(previousValue);
            }
        } else {
            if (!defaultValue.equals(comboBox.getValue())) {
                comboBox.setValue(defaultValue);
            }
        }
    }

    private void setUnconstrainedResize(TableView<AdminLogRowDto> table) {
        if (table == null) return;

        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }
}