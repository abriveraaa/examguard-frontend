package com.example.examguard.controller.faculty;

import com.example.examguard.model.faculty.dto.reports.*;
import com.example.examguard.model.faculty.dto.students.FacultyAcademicPeriodDTO;
import com.example.examguard.service.FacultyApiService;
import com.example.examguard.model.faculty.dto.students.FacultyStudentDTO;

import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;

import java.util.List;

public class FacultyReportsController {

    @FXML private ComboBox<String> academicPeriodCombo;
    @FXML private ComboBox<CourseOption> courseCombo;
    @FXML private ComboBox<SectionOption> sectionCombo;
    @FXML private ComboBox<String> submissionExamCombo;
    @FXML private ComboBox<String> violationExamCombo;

    @FXML private Label averageScoreLabel;
    @FXML private Label totalViolationsLabel;
    @FXML private Label pendingReviewLabel;
    @FXML private Label submissionRateLabel;
    @FXML private Label penalizedLabel;

    @FXML private StackPane loadingOverlay;
    @FXML private VBox reportsContent;

    @FXML private PieChart submissionPieChart;
    @FXML private BarChart<String, Number> takersCountBarChart;
    @FXML private BarChart<Number, String> violationTypeBarChart;
    @FXML private LineChart<String, Number> averageScoreLineChart;

    private final ObservableList<StudentRow> allPeriodStudents = FXCollections.observableArrayList();
    private final FacultyApiService facultyApiService = new FacultyApiService();

    private boolean loadingDropdowns = false;

    @FXML
    public void initialize() {
        loadFilters();
        reloadReportsAsync();
    }

    @FXML
    private void handleRefresh() {
        reloadReportsAsync();
    }

    @FXML
    private void handleViewGradebook() {
        showGradebookExamPicker(false);
    }

    @FXML
    private void handleExportGradebook() {
        showGradebookExamPicker(true);
    }

    @FXML
    private void handleViewClassRoster() {
        showAlert(
                "Class Roster Preview",
                "Preview class roster will be added here. For now, use Export."
        );
    }

    @FXML
    private void handleExportClassRoster() {
        exportClassRoster("pdf");
    }

    private void exportClassRoster(String type) {
        try {
            FacultyReportFilter filter = getBaseFilter();

            if (filter.academicYear() == null || filter.term() == null) {
                showAlert("Export Failed", "Please select an academic period first.");
                return;
            }

            String programCode = null;
            String yearLevel = null;
            String sectionName = null;

            SectionOption selectedSection = sectionCombo.getValue();

            if (selectedSection != null && !selectedSection.all()) {
                programCode = selectedSection.programCode();
                yearLevel = selectedSection.yearLevel();
                sectionName = selectedSection.sectionName();
            }

            byte[] bytes = facultyApiService.exportStudentsRoster(
                    filter.academicYear(),
                    filter.term(),
                    filter.courseCode(),
                    programCode,
                    yearLevel,
                    sectionName,
                    type
            );

            String fileName =
                    buildReportFileName(
                            "class-roster",
                            "pdf"
                    );

            boolean saved = saveBytesToFile(
                    bytes,
                    fileName,
                    "PDF Files",
                    "*.pdf"
            );

            if (!saved) {
                return;
            }

            showAlert(
                    "Export Successful",
                    "Class roster exported successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Failed", "Unable to export class roster: " + e.getMessage());
        }
    }

    private boolean saveBytesToFile(
            byte[] bytes,
            String initialFileName,
            String description,
            String extension
    ) throws Exception {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.setInitialFileName(initialFileName);

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(description, extension)
        );

        File file = fileChooser.showSaveDialog(
                reportsContent.getScene().getWindow()
        );

        if (file == null) {
            return false;
        }

        Files.write(file.toPath(), bytes);
        return true;
    }

    @FXML
    private void handleViewExamPortfolio() {
        showExamPortfolioPicker(false);
    }

    @FXML
    private void handleExportExamPortfolio() {
        showExamPortfolioPicker(true);
    }

    private void showExamPortfolioPicker(boolean exportMode) {

        FacultyReportFilter filter = getBaseFilter();

        if (filter.academicYear() == null || filter.term() == null) {
            showAlert(
                    "Exam Portfolio",
                    "Please select an academic period first."
            );
            return;
        }

        Task<List<ReportExamOptionDTO>> task = new Task<>() {
            @Override
            protected List<ReportExamOptionDTO> call() throws Exception {
                return facultyApiService.getFacultyReportExamOptions(filter);
            }
        };

        task.setOnSucceeded(e -> {
            List<ReportExamOptionDTO> exams = task.getValue();

            if (exams == null || exams.isEmpty()) {
                showAlert(
                        "Exam Portfolio",
                        "No exams found for the selected filters."
                );
                return;
            }

            ChoiceDialog<ReportExamOptionDTO> examDialog =
                    new ChoiceDialog<>(
                            exams.get(0),
                            exams
                    );

            examDialog.setTitle(
                    exportMode
                            ? "Export Exam Portfolio"
                            : "View Exam Portfolio"
            );

            examDialog.setHeaderText("Select the exam to print.");
            examDialog.setContentText("Exam:");

            examDialog.showAndWait().ifPresent(selectedExam -> {

                long offeringCount =
                        selectedExam.classOfferingCount() == null
                                ? 1
                                : selectedExam.classOfferingCount();

                if (offeringCount <= 1) {

                    String mode = "MERGED";

                    if (exportMode) {
                        exportExamPortfolio(
                                selectedExam.examId(),
                                selectedExam.title(),
                                mode,
                                null
                        );
                    } else {
                        viewExamPortfolio(
                                selectedExam.examId(),
                                selectedExam.title(),
                                mode
                        );
                    }

                    return;
                }

                showPortfolioModePicker(
                        selectedExam,
                        exportMode
                );
            });
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();

            showAlert(
                    "Exam Portfolio",
                    "Failed to load exams."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showPortfolioModePicker(
            ReportExamOptionDTO selectedExam,
            boolean exportMode
    ) {

        ChoiceDialog<String> modeDialog =
                new ChoiceDialog<>(
                        "MERGED",
                        "MERGED",
                        "SEPARATE"
                );

        modeDialog.setTitle(
                exportMode
                        ? "Export Exam Portfolio"
                        : "View Exam Portfolio"
        );

        modeDialog.setHeaderText(
                "Choose portfolio mode for:\n" +
                        selectedExam.title()
        );

        modeDialog.setContentText("Mode:");

        modeDialog.showAndWait().ifPresent(mode -> {

            if ("MERGED".equalsIgnoreCase(mode)) {

                if (exportMode) {
                    exportExamPortfolio(
                            selectedExam.examId(),
                            selectedExam.title(),
                            "MERGED",
                            null
                    );
                } else {
                    viewExamPortfolio(
                            selectedExam.examId(),
                            selectedExam.title(),
                            "MERGED"
                    );
                }

                return;
            }

            showPortfolioSectionPicker(
                    selectedExam,
                    exportMode
            );
        });
    }

    private void showPortfolioSectionPicker(
            ReportExamOptionDTO selectedExam,
            boolean exportMode
    ) {

        List<SectionOption> sections =
                sectionCombo.getItems()
                        .stream()
                        .filter(section -> !section.all())
                        .toList();

        if (sections.isEmpty()) {
            showAlert(
                    "Exam Portfolio",
                    "No sections found for separate export."
            );
            return;
        }

        ChoiceDialog<SectionOption> sectionDialog =
                new ChoiceDialog<>(
                        sections.get(0),
                        sections
                );

        sectionDialog.setTitle(
                exportMode
                        ? "Export Exam Portfolio"
                        : "View Exam Portfolio"
        );

        sectionDialog.setHeaderText(
                "Select section for:\n" +
                        selectedExam.title()
        );

        sectionDialog.setContentText("Section:");

        sectionDialog.showAndWait().ifPresent(section -> {

            if (exportMode) {
                exportExamPortfolio(
                        selectedExam.examId(),
                        selectedExam.title(),
                        "SEPARATE",
                        section.classOfferingId()
                );
            } else {
                viewExamPortfolio(
                        selectedExam.examId(),
                        selectedExam.title(),
                        "SEPARATE"
                );
            }
        });
    }

    private void exportExamPortfolio(
            Long examId,
            String examTitle,
            String mode,
            String classOfferingId
    ) {
        try {
            byte[] bytes =
                    facultyApiService.exportExamPortfolio(
                            examId,
                            mode,
                            classOfferingId
                    );

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            buildReportFileName(
                                    examTitle,
                                    mode
                            ),
                            "PDF Files",
                            "*.pdf"
                    );

            if (!saved) {
                return;
            }

            showAlert(
                    "Export Successful",
                    "Exam portfolio exported successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showAlert(
                    "Export Failed",
                    "Unable to export exam portfolio: " + e.getMessage()
            );
        }
    }

    private void viewExamPortfolio(
            Long examId,
            String examTitle,
            String mode
    ) {
        showAlert(
                "View Exam Portfolio",
                "Preview will be added later.\n\nSelected exam: " +
                        examTitle +
                        "\nMode: " +
                        mode
        );
    }

    @FXML
    private void handleViewViolationReport() {
        showAlert(
                "Violation Report Preview",
                "Preview violation report will be added here. For now, use Export."
        );
    }

    @FXML
    private void handleExportViolationReport() {
        exportViolationReport("pdf");
    }

    private void exportViolationReport(String type) {
        try {
            FacultyReportFilter filter = getCurrentFilterForViolation();

            if (filter.academicYear() == null || filter.term() == null) {
                showAlert("Export Failed", "Please select an academic period first.");
                return;
            }

            byte[] bytes = facultyApiService.exportViolationReport(filter, type);

            saveBytesToFile(
                    bytes,
                    "violation-report." + ("excel".equalsIgnoreCase(type) ? "xlsx" : "pdf"),
                    "excel".equalsIgnoreCase(type) ? "Excel Files" : "PDF Files",
                    "excel".equalsIgnoreCase(type) ? "*.xlsx" : "*.pdf"
            );

            showAlert("Export Successful", "Violation report exported successfully.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Export Failed", "Unable to export violation report: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewExamResultsSummary() {
        showAlert(
                "Exam Results Summary Preview",
                "Preview exam results summary will be added here. For now, use Export."
        );
    }

    @FXML
    private void handleExportExamResultsSummary() {
        showExamResultSummaryPicker();
    }

    private void showExamResultSummaryPicker() {

        FacultyReportFilter filter = getBaseFilter();

        if (filter.academicYear() == null || filter.term() == null) {
            showAlert(
                    "Exam Result Summary",
                    "Please select an academic period first."
            );
            return;
        }

        Task<List<ReportExamOptionDTO>> task = new Task<>() {
            @Override
            protected List<ReportExamOptionDTO> call() throws Exception {
                return facultyApiService.getFacultyReportExamOptions(filter);
            }
        };

        task.setOnSucceeded(e -> {
            List<ReportExamOptionDTO> exams = task.getValue();

            if (exams == null || exams.isEmpty()) {
                showAlert(
                        "Exam Result Summary",
                        "No exams found for the selected filters."
                );
                return;
            }

            ChoiceDialog<ReportExamOptionDTO> examDialog =
                    new ChoiceDialog<>(
                            exams.get(0),
                            exams
                    );

            examDialog.setTitle("Export Exam Result Summary");
            examDialog.setHeaderText("Select the exam to summarize.");
            examDialog.setContentText("Exam:");

            examDialog.showAndWait().ifPresent(selectedExam -> {

                long offeringCount =
                        selectedExam.classOfferingCount() == null
                                ? 1
                                : selectedExam.classOfferingCount();

                if (offeringCount <= 1) {
                    exportSingleExamResultSummary(
                            selectedExam.examId(),
                            selectedExam.title(),
                            null,
                            "MERGED"
                    );
                    return;
                }

                showExamResultSummaryModePicker(selectedExam);
            });
        });

        task.setOnFailed(e -> {
            task.getException().printStackTrace();

            showAlert(
                    "Exam Result Summary",
                    "Failed to load exams."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showExamResultSummaryModePicker(
            ReportExamOptionDTO selectedExam
    ) {

        ChoiceDialog<String> modeDialog =
                new ChoiceDialog<>(
                        "MERGED",
                        "MERGED",
                        "SEPARATE"
                );

        modeDialog.setTitle("Export Exam Result Summary");

        modeDialog.setHeaderText(
                "Choose summary mode for:\n" +
                        selectedExam.title()
        );

        modeDialog.setContentText("Mode:");

        modeDialog.showAndWait().ifPresent(mode -> {

            if ("MERGED".equalsIgnoreCase(mode)) {
                exportSingleExamResultSummary(
                        selectedExam.examId(),
                        selectedExam.title(),
                        null,
                        "MERGED"
                );
                return;
            }

            showExamResultSummarySectionPicker(selectedExam);
        });
    }

    private void showExamResultSummarySectionPicker(
            ReportExamOptionDTO selectedExam
    ) {

        List<SectionOption> sections =
                sectionCombo.getItems()
                        .stream()
                        .filter(section -> !section.all())
                        .toList();

        if (sections.isEmpty()) {
            showAlert(
                    "Exam Result Summary",
                    "No sections found for separate export."
            );
            return;
        }

        ChoiceDialog<SectionOption> sectionDialog =
                new ChoiceDialog<>(
                        sections.get(0),
                        sections
                );

        sectionDialog.setTitle("Export Exam Result Summary");
        sectionDialog.setHeaderText(
                "Select section for:\n" +
                        selectedExam.title()
        );
        sectionDialog.setContentText("Section:");

        sectionDialog.showAndWait().ifPresent(section -> {
            exportSingleExamResultSummary(
                    selectedExam.examId(),
                    selectedExam.title(),
                    section.classOfferingId(),
                    "SEPARATE"
            );
        });
    }

    private void exportSingleExamResultSummary(
            Long examId,
            String examTitle,
            String classOfferingId,
            String mode
    ) {
        try {
            byte[] bytes =
                    facultyApiService.exportExamResultSummary(
                            examId,
                            classOfferingId
                    );

            boolean saved =
                    saveBytesToFile(
                            bytes,
                            buildReportFileName(
                                    examTitle + "-result-summary",
                                    "pdf"
                            ),
                            "PDF Files",
                            "*.pdf"
                    );

            if (!saved) {
                return;
            }

            showAlert(
                    "Export Successful",
                    "Exam result summary exported successfully."
            );

        } catch (Exception e) {
            e.printStackTrace();

            showAlert(
                    "Export Failed",
                    "Unable to export exam result summary: " + e.getMessage()
            );
        }
    }

    private void renderParticipation(List<ExamParticipationDTO> data) {
        takersCountBarChart.getData().clear();
        averageScoreLineChart.getData().clear();

        XYChart.Series<String, Number> takersSeries = new XYChart.Series<>();
        XYChart.Series<String, Number> averageSeries = new XYChart.Series<>();

        for (ExamParticipationDTO row : data) {
            takersSeries.getData().add(
                    new XYChart.Data<>(row.examTitle(), row.totalTakers())
            );

            averageSeries.getData().add(
                    avgPoint(row.examTitle(), row.averageScore().intValue())
            );
        }

        takersCountBarChart.getData().add(takersSeries);
        averageScoreLineChart.getData().add(averageSeries);

        addChartValueLabels(takersSeries);
    }

    private void renderSubmissionStatus(List<SubmissionStatusDTO> data) {
        submissionPieChart.setData(
                FXCollections.observableArrayList(
                        data.stream()
                                .map(row -> new PieChart.Data(row.status(), row.count()))
                                .toList()
                )
        );

        Platform.runLater(this::updatePieLabels);
    }

    private void renderViolations(List<ViolationTypeDTO> data) {
        violationTypeBarChart.getData().clear();

        XYChart.Series<Number, String> series = new XYChart.Series<>();

        for (ViolationTypeDTO row : data) {
            series.getData().add(
                    new XYChart.Data<>(
                            row.count(),
                            formatViolationType(row.violationType())
                    )
            );
        }

        violationTypeBarChart.getData().add(series);
        addChartValueLabels(series);
    }

    private String formatViolationType(String value) {
        if (value == null) {
            return "Unknown";
        }

        return value
                .replace("_", " ")
                .toUpperCase();
    }

    private void loadFilters() {
        try {
            List<FacultyAcademicPeriodDTO> periods =
                    facultyApiService.getStudentAcademicPeriods();

            List<String> periodItems = periods.stream()
                    .filter(p -> isActiveOffering(p.classOfferingStatus()))
                    .sorted((a, b) -> {
                        int ayCompare = academicYearStart(b.academicYear())
                                .compareTo(academicYearStart(a.academicYear()));

                        if (ayCompare != 0) return ayCompare;

                        return Integer.compare(
                                termRank(a.term()),
                                termRank(b.term())
                        );
                    })
                    .map(p -> p.term() + ", AY " + p.academicYear())
                    .distinct()
                    .toList();

            academicPeriodCombo.getItems().clear();
            academicPeriodCombo.getItems().addAll(periodItems);

            if (!periodItems.isEmpty()) {
                academicPeriodCombo.getSelectionModel().selectFirst();
            }

            loadStudentsForSelectedPeriodAsync();

            courseCombo.setItems(FXCollections.observableArrayList(
                    new CourseOption("ALL", "All Courses", true)
            ));
            courseCombo.getSelectionModel().selectFirst();

            sectionCombo.setItems(FXCollections.observableArrayList(
                    new SectionOption(
                            "ALL",
                            "ALL",
                            null,
                            "ALL",
                            "All Sections",
                            true
                    )
            ));
            sectionCombo.getSelectionModel().selectFirst();

            submissionExamCombo.setItems(FXCollections.observableArrayList("All Exams"));
            submissionExamCombo.getSelectionModel().selectFirst();

            violationExamCombo.setItems(FXCollections.observableArrayList("All Exams"));
            violationExamCombo.getSelectionModel().selectFirst();

            loadExamDropdownsAsync();

            academicPeriodCombo.setOnAction(e -> {
                loadStudentsForSelectedPeriodAsync();
                loadExamDropdownsAsync();
                reloadReportsAsync();
            });

            courseCombo.setOnAction(e -> {
                if (loadingDropdowns) return;

                populateSectionsFromLoadedStudents();
                loadExamDropdownsAsync();
                reloadReportsAsync();
            });

            sectionCombo.setOnAction(e -> {
                if (loadingDropdowns) return;

                loadExamDropdownsAsync();
                reloadReportsAsync();
            });

            submissionExamCombo.setOnAction(e -> reloadSubmissionStatusAsync());
            violationExamCombo.setOnAction(e -> reloadViolationsAsync());

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Reports Error", "Failed to load report filters.");
        }
    }

    private void reloadReportsAsync() {

        FacultyReportFilter filter = getBaseFilter();

        if (filter.academicYear() == null || filter.term() == null) {
            return;
        }

        showLoading(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                FacultyReportSummaryDTO summary =
                        facultyApiService.getFacultyReportSummary(filter);

                List<ExamParticipationDTO> participation =
                        facultyApiService.getFacultyReportParticipation(filter);

                List<SubmissionStatusDTO> submissions =
                        facultyApiService.getFacultyReportSubmissionStatus(filter);

                List<ViolationTypeDTO> violations =
                        facultyApiService.getFacultyReportViolations(filter);

                Platform.runLater(() -> {
                    renderSummary(summary);
                    renderParticipation(participation);
                    renderSubmissionStatus(submissions);
                    renderViolations(violations);
                });

                return null;
            }
        };

        task.setOnSucceeded(e -> showLoading(false));

        task.setOnFailed(e -> {
            showLoading(false);
            task.getException().printStackTrace();
            showAlert("Reports Error", "Failed to load reports.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadSubmissionStatusAsync() {

        FacultyReportFilter filter =
                getCurrentFilterForSubmission();

        if (filter.academicYear() == null || filter.term() == null) {
            return;
        }

        Task<List<SubmissionStatusDTO>> task = new Task<>() {
            @Override
            protected List<SubmissionStatusDTO> call() throws Exception {
                return facultyApiService.getFacultyReportSubmissionStatus(filter);
            }
        };

        task.setOnSucceeded(e ->
                renderSubmissionStatus(task.getValue())
        );

        task.setOnFailed(e ->
                task.getException().printStackTrace()
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void reloadViolationsAsync() {

        FacultyReportFilter filter =
                getCurrentFilterForViolation();

        if (filter.academicYear() == null || filter.term() == null) {
            return;
        }

        Task<List<ViolationTypeDTO>> task = new Task<>() {
            @Override
            protected List<ViolationTypeDTO> call() throws Exception {
                return facultyApiService.getFacultyReportViolations(filter);
            }
        };

        task.setOnSucceeded(e ->
                renderViolations(task.getValue())
        );

        task.setOnFailed(e ->
                task.getException().printStackTrace()
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadExamDropdownsAsync() {

        FacultyReportFilter filter = getCurrentFilter();

        if (filter.academicYear() == null || filter.term() == null) {
            return;
        }

        Task<List<ReportExamOptionDTO>> task = new Task<>() {
            @Override
            protected List<ReportExamOptionDTO> call() throws Exception {
                return facultyApiService.getFacultyReportExamOptions(filter);
            }
        };

        task.setOnSucceeded(e -> {
            List<String> items = task.getValue()
                    .stream()
                    .map(exam -> exam.examId() + " - " + exam.title())
                    .toList();

            submissionExamCombo.getItems().setAll("All Exams");
            submissionExamCombo.getItems().addAll(items);
            submissionExamCombo.getSelectionModel().selectFirst();

            violationExamCombo.getItems().setAll("All Exams");
            violationExamCombo.getItems().addAll(items);
            violationExamCombo.getSelectionModel().selectFirst();
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void loadStudentsForSelectedPeriodAsync() {

        FacultyReportFilter filter = getPeriodOnlyFilter();

        if (filter.academicYear() == null || filter.term() == null) {
            return;
        }

        Task<List<FacultyStudentDTO>> task = new Task<>() {
            @Override
            protected List<FacultyStudentDTO> call() throws Exception {
                return facultyApiService.getStudentsByPeriod(
                        filter.academicYear(),
                        filter.term()
                );
            }
        };

        task.setOnSucceeded(e -> {
            allPeriodStudents.setAll(
                    task.getValue()
                            .stream()
                            .map(student -> new StudentRow(
                                    student.studentId(),
                                    student.fullName(),
                                    student.emailAddress(),
                                    student.collegeCode(),
                                    student.collegeName(),
                                    student.programCode(),
                                    student.programName(),
                                    student.yearLevel(),
                                    student.sectionName(),
                                    student.courseCode(),
                                    student.courseDescription(),
                                    student.classOfferingId()
                            ))
                            .toList()
            );

            populateCoursesFromLoadedStudents();
            populateSectionsFromLoadedStudents();
        });

        task.setOnFailed(e -> task.getException().printStackTrace());

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateCoursesFromLoadedStudents() {
        loadingDropdowns = true;

        CourseOption current = courseCombo.getValue();

        List<CourseOption> options = new ArrayList<>();
        options.add(new CourseOption("ALL", "All Courses", true));

        allPeriodStudents.stream()
                .filter(s -> s.courseCode() != null && !s.courseCode().isBlank())
                .collect(Collectors.toMap(
                        StudentRow::courseCode,
                        s -> new CourseOption(
                                s.courseCode(),
                                s.courseDescription(),
                                false
                        ),
                        (a, b) -> a,
                        java.util.TreeMap::new
                ))
                .values()
                .forEach(options::add);

        courseCombo.setItems(FXCollections.observableArrayList(options));

        if (current != null) {
            options.stream()
                    .filter(o -> Objects.equals(o.courseCode(), current.courseCode()))
                    .findFirst()
                    .ifPresentOrElse(
                            o -> courseCombo.getSelectionModel().select(o),
                            () -> courseCombo.getSelectionModel().selectFirst()
                    );
        } else {
            courseCombo.getSelectionModel().selectFirst();
        }

        loadingDropdowns = false;
    }

    private void populateSectionsFromLoadedStudents() {
        loadingDropdowns = true;

        CourseOption selectedCourse = courseCombo.getValue();
        SectionOption current = sectionCombo.getValue();

        List<SectionOption> options = new ArrayList<>();

        options.add(new SectionOption(
                "ALL",
                "ALL",
                null,
                "ALL",
                "All Sections",
                true
        ));

        allPeriodStudents.stream()
                .filter(s ->
                        selectedCourse == null ||
                                selectedCourse.all() ||
                                selectedCourse.courseCode().equals(s.courseCode())
                )
                .filter(s ->
                        s.programCode() != null &&
                                s.yearLevel() != null &&
                                s.sectionName() != null
                )
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.programCode() + "|" + s.yearLevel() + "|" + s.sectionName(),
                        s -> new SectionOption(
                                s.classOfferingId(),
                                s.programCode(),
                                s.yearLevel(),
                                s.sectionName(),
                                s.programCode() + " " + s.yearLevel() + "-" + s.sectionName(),
                                false
                        ),
                        (existing, duplicate) -> existing,
                        java.util.LinkedHashMap::new
                ))
                .values()
                .forEach(options::add);

        sectionCombo.setItems(FXCollections.observableArrayList(options));

        if (current != null) {
            options.stream()
                    .filter(o ->
                            o.all() == current.all() &&
                                    Objects.equals(o.programCode(), current.programCode()) &&
                                    Objects.equals(o.yearLevel(), current.yearLevel()) &&
                                    Objects.equals(o.sectionName(), current.sectionName())
                    )
                    .findFirst()
                    .ifPresentOrElse(
                            sectionCombo.getSelectionModel()::select,
                            () -> sectionCombo.getSelectionModel().selectFirst()
                    );
        } else {
            sectionCombo.getSelectionModel().selectFirst();
        }

        loadingDropdowns = false;
    }

    private FacultyReportFilter getCurrentFilter() {

        String selectedPeriod =
                academicPeriodCombo.getSelectionModel().getSelectedItem();

        String academicYear = null;
        String term = null;

        if (selectedPeriod != null && selectedPeriod.contains(", AY ")) {
            String[] parts = selectedPeriod.split(", AY ");
            term = parts[0].trim();
            academicYear = parts[1].trim();
        }

        String courseCode = null;

        CourseOption selectedCourse = courseCombo.getValue();
        if (selectedCourse != null && !selectedCourse.all()) {
            courseCode = selectedCourse.courseCode();
        }

        String classOfferingId = null;

        SectionOption selectedSection = sectionCombo.getValue();
        if (selectedSection != null && !selectedSection.all()) {
            classOfferingId = selectedSection.classOfferingId();
        }

        Long examId = parseSelectedExamId(
                submissionExamCombo.getValue()
        );

        return new FacultyReportFilter(
                academicYear,
                term,
                courseCode,
                classOfferingId,
                examId
        );
    }

    private FacultyReportFilter getCurrentFilterForSubmission() {
        FacultyReportFilter base = getBaseFilter();

        return new FacultyReportFilter(
                base.academicYear(),
                base.term(),
                base.courseCode(),
                base.classOfferingId(),
                parseSelectedExamId(submissionExamCombo.getValue())
        );
    }

    private FacultyReportFilter getCurrentFilterForViolation() {
        FacultyReportFilter base = getBaseFilter();

        return new FacultyReportFilter(
                base.academicYear(),
                base.term(),
                base.courseCode(),
                base.classOfferingId(),
                parseSelectedExamId(violationExamCombo.getValue())
        );
    }

    private void renderSummary(FacultyReportSummaryDTO dto) {

        averageScoreLabel.setText(
                String.format(
                        "%.1f%%",
                        dto.averageScore()
                )
        );

        submissionRateLabel.setText(
                String.format(
                        "%.0f%%",
                        dto.submissionRate()
                )
        );

        totalViolationsLabel.setText(
                String.valueOf(
                        dto.totalViolations()
                )
        );

        pendingReviewLabel.setText(
                String.valueOf(
                        dto.pendingReview()
                )
        );

        penalizedLabel.setText(
                String.valueOf(
                        dto.penalized()
                )
        );
    }

    private void updatePieLabels() {

        double total = submissionPieChart.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();

        for (PieChart.Data data : submissionPieChart.getData()) {
            double percentage = (data.getPieValue() / total) * 100;
            data.setName(String.format( "%s\n%d (%.0f%%)", data.getName(), (int)data.getPieValue(), percentage));
        }
    }

    private void showGradebookExamPicker(boolean exportMode) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(exportMode ? "Export Class Record" : "View Class Record");
        dialog.setHeaderText("Select exams to include in the class record.");

        CheckBox selectAllBox = new CheckBox("Select All");
        selectAllBox.setSelected(true);

        List<CheckBox> examBoxes = List.of(
                new CheckBox("Quiz 1"),
                new CheckBox("Quiz 2"),
                new CheckBox("Quiz 3"),
                new CheckBox("Quiz 4")
        );

        examBoxes.forEach(cb -> cb.setSelected(true));

        selectAllBox.selectedProperty().addListener((obs, oldVal, selected) -> {
            examBoxes.forEach(cb -> cb.setSelected(selected));
        });

        CheckBox includeAverage = new CheckBox("Include total average");
        includeAverage.setSelected(true);

        CheckBox includeStatus = new CheckBox("Include submission status");
        includeStatus.setSelected(true);

        VBox content = new VBox(12);
        content.getChildren().add(selectAllBox);
        content.getChildren().addAll(examBoxes);
        content.getChildren().add(new Separator());
        content.getChildren().addAll(includeAverage, includeStatus);

        dialog.getDialogPane().setContent(content);

        ButtonType previewButton = new ButtonType("Preview", ButtonBar.ButtonData.OK_DONE);
        ButtonType exportPdfButton = new ButtonType("Export PDF", ButtonBar.ButtonData.APPLY);
        ButtonType exportExcelButton = new ButtonType("Export Excel", ButtonBar.ButtonData.APPLY);

        if (exportMode) {
            dialog.getDialogPane().getButtonTypes().addAll(
                    exportPdfButton,
                    exportExcelButton,
                    ButtonType.CANCEL
            );
        } else {
            dialog.getDialogPane().getButtonTypes().addAll(
                    previewButton,
                    ButtonType.CANCEL
            );
        }

        dialog.showAndWait().ifPresent(result -> {
            List<String> selectedExams = examBoxes.stream()
                    .filter(CheckBox::isSelected)
                    .map(CheckBox::getText)
                    .toList();

            if (selectedExams.isEmpty()) {
                showAlert("No Exams Selected", "Please select at least one exam.");
                return;
            }

            if (result == previewButton) {
                previewGradebook(selectedExams);
            } else if (result == exportPdfButton) {
                exportGradebook(selectedExams, "pdf");
            } else if (result == exportExcelButton) {
                exportGradebook(selectedExams, "excel");
            }
        });
    }

    private void previewGradebook(List<String> selectedExams) {
        String columns = "Name | " + String.join(" | ", selectedExams) + " | Average";

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Class Record Preview");
        alert.setHeaderText("Generated Columns");
        alert.setContentText(columns);
        alert.showAndWait();
    }

    private void exportGradebook(List<String> selectedExams, String type) {
        showAlert(
                "Export Gradebook",
                "Exporting " + type.toUpperCase() + " with exams: " +
                        String.join(", ", selectedExams)
        );
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisible(show);
        loadingOverlay.setManaged(show);
        reportsContent.setDisable(show);
    }

    private <X, Y> void addChartValueLabels(XYChart.Series<X, Y> series) {

        Platform.runLater(() -> {

            for (XYChart.Data<X, Y> data : series.getData()) {

                if (!(data.getNode() instanceof StackPane bar)) {
                    continue;
                }

                boolean verticalBar = data.getYValue() instanceof Number;

                String value = verticalBar
                        ? data.getYValue().toString()
                        : data.getXValue().toString();

                Label label = new Label(value);
                label.getStyleClass().add("chart-value-label");

                if (verticalBar) {
                    StackPane.setAlignment(label, Pos.BOTTOM_CENTER);
                    label.setTranslateY(-14);
                } else {
                    StackPane.setAlignment(label, Pos.CENTER_RIGHT);
                    label.setTranslateX(18);
                }

                bar.getChildren().removeIf(node ->
                        node instanceof Label &&
                                node.getStyleClass().contains("chart-value-label")
                );

                bar.getChildren().add(label);
            }
        });
    }

    private XYChart.Data<String, Number> avgPoint(String exam, int score) {
        XYChart.Data<String, Number> data = new XYChart.Data<>(exam, score);

        StackPane point = new StackPane();
        point.setAlignment(Pos.CENTER);
        point.setPickOnBounds(false);

        Region dot = new Region();
        dot.getStyleClass().add("avg-score-dot");

        Label label = new Label(score + "%");
        label.getStyleClass().add("avg-score-point-label");
        label.setTranslateY(18);
        label.setMouseTransparent(true);

        point.getChildren().addAll(dot, label);
        data.setNode(point);

        return data;
    }

    private boolean isActiveOffering(String status) {
        return status != null && status.equalsIgnoreCase("ACTIVE");
    }

    private int termRank(String term) {
        if (term == null) {
            return 99;
        }

        String normalized = term.trim().toUpperCase();

        return switch (normalized) {
            case "SUMMER", "SUMMER TERM" -> 1;

            case "SECOND SEMESTER",
                 "2ND SEMESTER",
                 "SECOND" -> 2;

            case "FIRST SEMESTER",
                 "1ST SEMESTER",
                 "FIRST" -> 3;

            case "FOURTH QUARTER",
                 "4TH QUARTER",
                 "Q4" -> 1;

            case "THIRD QUARTER",
                 "3RD QUARTER",
                 "Q3" -> 2;

            case "SECOND QUARTER",
                 "2ND QUARTER",
                 "Q2" -> 3;

            case "FIRST QUARTER",
                 "1ST QUARTER",
                 "Q1" -> 4;

            default -> 99;
        };
    }

    private Integer academicYearStart(String academicYear) {

        if (academicYear == null || academicYear.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    academicYear.substring(0, 4)
            );
        } catch (Exception e) {
            return 0;
        }
    }

    private Long parseSelectedExamId(String value) {
        if (value == null || value.startsWith("All")) {
            return null;
        }

        try {
            return Long.parseLong(value.split(" - ")[0].trim());
        } catch (Exception e) {
            return null;
        }
    }

    private FacultyReportFilter getBaseFilter() {
        String selectedPeriod =
                academicPeriodCombo.getSelectionModel().getSelectedItem();

        String academicYear = null;
        String term = null;

        if (selectedPeriod != null && selectedPeriod.contains(", AY ")) {
            String[] parts = selectedPeriod.split(", AY ");
            term = parts[0].trim();
            academicYear = parts[1].trim();
        }

        String courseCode = null;

        CourseOption selectedCourse = courseCombo.getValue();
        if (selectedCourse != null && !selectedCourse.all()) {
            courseCode = selectedCourse.courseCode();
        }

        String classOfferingId = null;

        SectionOption selectedSection = sectionCombo.getValue();
        if (selectedSection != null && !selectedSection.all()) {
            classOfferingId = selectedSection.classOfferingId();
        }

        return new FacultyReportFilter(
                academicYear,
                term,
                courseCode,
                classOfferingId,
                null
        );
    }

    private String buildReportFileName(
            String reportName,
            String extension
    ) {

        List<String> parts = new ArrayList<>();

        parts.add(reportName);

        // Academic Period
        String selectedPeriod =
                academicPeriodCombo.getValue();

        if (selectedPeriod != null &&
                selectedPeriod.contains(", AY ")) {

            String[] periodParts =
                    selectedPeriod.split(", AY ");

            String term =
                    sanitizeFileName(
                            periodParts[0]
                    );

            String academicYear =
                    sanitizeFileName(
                            periodParts[1]
                    );

            parts.add(term);
            parts.add("AY-" + academicYear);
        }

        // Course
        CourseOption course =
                courseCombo.getValue();

        if (course != null && !course.all()) {
            parts.add(
                    sanitizeFileName(
                            course.courseCode()
                    )
            );
        }

        // Section
        SectionOption section =
                sectionCombo.getValue();

        if (section != null && !section.all()) {

            String sectionLabel =
                    section.programCode()
                            + "-"
                            + section.yearLevel()
                            + "-"
                            + section.sectionName();

            parts.add(
                    sanitizeFileName(
                            sectionLabel
                    )
            );
        }

        return String.join(
                "_",
                parts
        ) + "." + extension;
    }

    private String sanitizeFileName(String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "-");
    }

    private FacultyReportFilter getPeriodOnlyFilter() {
        String selectedPeriod =
                academicPeriodCombo.getSelectionModel().getSelectedItem();

        String academicYear = null;
        String term = null;

        if (selectedPeriod != null && selectedPeriod.contains(", AY ")) {
            String[] parts = selectedPeriod.split(", AY ");
            term = parts[0].trim();
            academicYear = parts[1].trim();
        }

        return new FacultyReportFilter(
                academicYear,
                term,
                null,
                null,
                null
        );
    }

    public record CourseOption(
            String courseCode,
            String courseDescription,
            boolean all
    ) {
        @Override
        public String toString() {
            return all ? "All Courses" : courseCode + " - " + courseDescription;
        }
    }

    public record SectionOption(
            String classOfferingId,
            String programCode,
            String yearLevel,
            String sectionName,
            String label,
            boolean all
    ) {
        @Override
        public String toString() {
            return label;
        }
    }

    public record StudentRow(
            String studentNo,
            String fullName,
            String email,
            String collegeCode,
            String collegeName,
            String programCode,
            String programName,
            String yearLevel,
            String sectionName,
            String courseCode,
            String courseDescription,
            String classOfferingId
    ) {}

}