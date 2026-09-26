package br.com.techmind.academy.enrollment;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        String courseSlug,
        String courseTitle,
        int completedLessons,
        int totalLessons,
        int xp,
        double percentage
) {
    public static EnrollmentResponse from(Enrollment enrollment) {
        int total = enrollment.getCourse().getTotalLessons();
        int completed = Math.min(enrollment.getCompletedLessons(), total);
        double percentage = total == 0 ? 0 : completed * 100.0 / total;
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getSlug(),
                enrollment.getCourse().getTitle(),
                completed,
                total,
                enrollment.getXp(),
                percentage
        );
    }
}
