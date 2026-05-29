package com.example.examguard.cache;

import com.example.examguard.model.faculty.dto.*;
import com.example.examguard.service.FacultyApiService;

import java.util.List;

public class FacultyLocalCachePreloadService {

    private final LocalCacheService cacheService;
    private final FacultyApiService facultyApiService;

    public FacultyLocalCachePreloadService(
            LocalCacheService cacheService,
            FacultyApiService facultyApiService
    ) {
        this.cacheService = cacheService;
        this.facultyApiService = facultyApiService;
    }

    public void preloadAfterLogin(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            return;
        }

        preloadDashboardProfile(employeeId);
        preloadDashboardStats(employeeId);
        preloadDashboardActiveExams(employeeId);
        preloadDashboardNeedsReview(employeeId);
        preloadDashboardClasses(employeeId);
        preloadExams(employeeId);
    }

    private void preloadDashboardProfile(String employeeId) {
        try {
            FacultyProfileDTO data = facultyApiService.getDashboardProfile();

            cacheService.save(
                    FacultyLocalCacheKeys.dashboardProfile(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty dashboard profile preload skipped: " + e.getMessage());
        }
    }

    private void preloadDashboardStats(String employeeId) {
        try {
            FacultyDashboardStatsDTO data = facultyApiService.getDashboardStats();

            cacheService.save(
                    FacultyLocalCacheKeys.dashboardStats(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty dashboard stats preload skipped: " + e.getMessage());
        }
    }

    private void preloadDashboardActiveExams(String employeeId) {
        try {
            List<FacultyExamSummaryDTO> data =
                    facultyApiService.getDashboardActiveExams();

            cacheService.save(
                    FacultyLocalCacheKeys.dashboardActiveExams(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty active exams preload skipped: " + e.getMessage());
        }
    }

    private void preloadDashboardNeedsReview(String employeeId) {
        try {
            List<FacultyViolationReviewDTO> data =
                    facultyApiService.getDashboardNeedsReview();

            cacheService.save(
                    FacultyLocalCacheKeys.dashboardNeedsReview(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty needs review preload skipped: " + e.getMessage());
        }
    }

    private void preloadDashboardClasses(String employeeId) {
        try {
            List<FacultyClassDTO> data =
                    facultyApiService.getDashboardClasses();

            cacheService.save(
                    FacultyLocalCacheKeys.dashboardClasses(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty classes preload skipped: " + e.getMessage());
        }
    }

    private void preloadExams(String employeeId) {
        try {
            List<FacultyExamSummaryDTO> data =
                    facultyApiService.getExams();

            cacheService.save(
                    FacultyLocalCacheKeys.exams(employeeId),
                    FacultyLocalCacheKeys.VERSION,
                    data
            );

        } catch (Exception e) {
            System.out.println("Faculty exams preload skipped: " + e.getMessage());
        }
    }
}