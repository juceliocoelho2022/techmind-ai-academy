package br.com.techmind.academy.analytics;

import java.time.OffsetDateTime;
import java.util.List;

public record AdminAnalyticsResponse(
        OffsetDateTime generatedAt,
        PlatformSummary summary,
        List<CoursePerformance> courses
) {
    public record PlatformSummary(
            long totalUsers,
            long students,
            long admins,
            long courses,
            long enrollments,
            long completedLessons,
            long totalXp,
            long quizzes,
            long quizAttempts,
            double averageQuizScore,
            double quizApprovalRate
    ) {}

    public record CoursePerformance(
            Long courseId,
            String title,
            String category,
            String technology,
            String level,
            int plannedLessons,
            long enrollments,
            long completedLessons,
            double completionRate,
            long totalXp,
            long quizzes,
            long quizAttempts,
            double averageQuizScore,
            double quizApprovalRate
    ) {}
}
