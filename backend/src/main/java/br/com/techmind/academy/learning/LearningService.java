package br.com.techmind.academy.learning;

import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.enrollment.Enrollment;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LearningService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonResourceRepository lessonResourceRepository;

    public LearningService(
            CourseRepository courseRepository,
            CourseModuleRepository moduleRepository,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonResourceRepository lessonResourceRepository
    ) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.lessonResourceRepository = lessonResourceRepository;
    }

    @Transactional(readOnly = true)
    public CourseCurriculumResponse curriculum(Long courseId) {
        var course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trilha não encontrada"));

        var modules = moduleRepository.findByCourseIdOrderByPositionAsc(courseId);
        var lessons = lessonRepository.findCurriculumByCourseId(courseId);

        Map<Long, List<Lesson>> lessonsByModule = lessons.stream()
                .collect(Collectors.groupingBy(lesson -> lesson.getModule().getId()));

        Map<Long, List<LessonResource>> resourcesByLesson = lessonResourceRepository.findByCourseId(courseId)
                .stream()
                .collect(Collectors.groupingBy(resource -> resource.getLesson().getId()));

        var moduleResponses = modules.stream()
                .map(module -> new LearningModuleResponse(
                        module.getId(),
                        module.getTitle(),
                        module.getDescription(),
                        module.getPosition(),
                        lessonsByModule.getOrDefault(module.getId(), List.of())
                                .stream()
                                .map(lesson -> LessonResponse.from(
                                        lesson,
                                        resourcesByLesson.getOrDefault(lesson.getId(), List.of())
                                ))
                                .toList()
                ))
                .toList();

        return new CourseCurriculumResponse(
                course.getId(),
                course.getSlug(),
                course.getTitle(),
                course.getTotalLessons(),
                lessons.size(),
                moduleResponses
        );
    }

    @Transactional(readOnly = true)
    public LearningProgressResponse progress(String email, Long courseId) {
        var enrollment = enrollmentRepository.findByUserEmailAndCourseId(email, courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula não encontrada"));

        var completedIds = lessonProgressRepository.findCompletedLessonIds(email, courseId);
        int completed = completedIds.size();
        int total = enrollment.getCourse().getTotalLessons();

        return new LearningProgressResponse(
                courseId,
                completedIds,
                completed,
                total,
                enrollment.getXp(),
                percentage(completed, total)
        );
    }

    @Transactional
    public CompleteLessonResponse completeLesson(String email, Long lessonId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada"));

        var course = lesson.getModule().getCourse();
        var enrollment = enrollmentRepository.findForUpdateByUserEmailAndCourseId(email, course.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Matricule-se na trilha antes de concluir aulas"
                ));

        var existing = lessonProgressRepository.findByUserIdAndLessonId(user.getId(), lessonId);
        if (existing.isPresent()) {
            return responseFor(lessonId, false, 0, enrollment);
        }

        lessonProgressRepository.save(
                LessonProgress.builder()
                        .user(user)
                        .lesson(lesson)
                        .xpAwarded(lesson.getXpReward())
                        .build()
        );

        int completed = Math.min(enrollment.getCompletedLessons() + 1, course.getTotalLessons());
        enrollment.setCompletedLessons(completed);
        enrollment.setXp(enrollment.getXp() + lesson.getXpReward());
        enrollmentRepository.save(enrollment);

        return responseFor(lessonId, true, lesson.getXpReward(), enrollment);
    }

    private CompleteLessonResponse responseFor(
            Long lessonId,
            boolean newlyCompleted,
            int xpAwarded,
            Enrollment enrollment
    ) {
        int total = enrollment.getCourse().getTotalLessons();
        int completed = Math.min(enrollment.getCompletedLessons(), total);

        return new CompleteLessonResponse(
                lessonId,
                newlyCompleted,
                xpAwarded,
                completed,
                enrollment.getXp(),
                percentage(completed, total)
        );
    }

    private double percentage(int completed, int total) {
        return total == 0 ? 0 : completed * 100.0 / total;
    }
}
