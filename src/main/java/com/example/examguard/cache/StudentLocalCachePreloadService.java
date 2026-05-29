package com.example.examguard.cache;

import com.example.examguard.model.profile.ProfileResponseDTO;
import com.example.examguard.model.student.StudentDashboardResponse;
import com.example.examguard.model.student.StudentExamResponse;
import com.example.examguard.service.AuthApiService;
import com.example.examguard.service.StudentApiService;
import com.example.examguard.utility.Session;

import java.util.List;

public class StudentLocalCachePreloadService {

    private final LocalCacheService localCacheService = new LocalCacheService();
    private final StudentApiService studentApiService = new StudentApiService();
    private final AuthApiService authApiService = new AuthApiService();

    public void preloadAfterLogin() {
        String schoolId = Session.getSchoolId();

        if (schoolId == null || schoolId.isBlank()) {
            return;
        }

        Thread thread = new Thread(() -> {
            preloadProfile(schoolId);
            preloadDashboard(schoolId);
            preloadExams(schoolId);
        });

        thread.setDaemon(true);
        thread.start();
    }

    private void preloadProfile(String schoolId) {
        try {
            ProfileResponseDTO profile = authApiService.getProfile();

            localCacheService.save(
                    StudentLocalCacheKeys.profile(schoolId),
                    String.valueOf(System.currentTimeMillis()),
                    profile
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    private void preloadDashboard(String schoolId) {
        try {
            StudentDashboardResponse dashboard = studentApiService.fetchDashboard();
            String version = String.valueOf(System.currentTimeMillis());

            localCacheService.save(
                    StudentLocalCacheKeys.dashboardProfile(schoolId),
                    version,
                    dashboard.getProfile()
            );

            localCacheService.save(
                    StudentLocalCacheKeys.dashboardUpcoming(schoolId),
                    version,
                    dashboard.getUpcomingExams()
            );

            localCacheService.save(
                    StudentLocalCacheKeys.dashboardResults(schoolId),
                    version,
                    dashboard.getResultSummary()
            );

            localCacheService.save(
                    StudentLocalCacheKeys.dashboardStats(schoolId),
                    version,
                    dashboard.getStats()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void preloadExams(String schoolId) {
        try {
            List<StudentExamResponse> exams = studentApiService.getStudentExams();

            localCacheService.save(
                    StudentLocalCacheKeys.exams(schoolId),
                    String.valueOf(System.currentTimeMillis()),
                    exams
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}