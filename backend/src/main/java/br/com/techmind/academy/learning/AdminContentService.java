package br.com.techmind.academy.learning;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.course.Course;
import br.com.techmind.academy.course.CourseRepository;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.quiz.LessonQuizRepository;
import br.com.techmind.academy.quiz.QuizAttemptRepository;
import br.com.techmind.academy.settings.PlatformSettingsService;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminContentService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonResourceRepository resourceRepository;
    private final LessonResourceStorageService storageService;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonQuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final PlatformSettingsService settingsService;
    private final AdminAuditService auditService;
    private final UserRepository userRepository;

    public AdminContentService(
            CourseRepository courseRepository,
            CourseModuleRepository moduleRepository,
            LessonRepository lessonRepository,
            LessonProgressRepository lessonProgressRepository,
            LessonResourceRepository resourceRepository,
            LessonResourceStorageService storageService,
            EnrollmentRepository enrollmentRepository,
            LessonQuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            PlatformSettingsService settingsService,
            AdminAuditService auditService,
            UserRepository userRepository
    ) {
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.lessonProgressRepository = lessonProgressRepository;
        this.resourceRepository = resourceRepository;
        this.storageService = storageService;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional
    public AdminCourseResponse createCourse(String email, AdminCourseRequest request) {
        requireAdmin(email);

        courseRepository.findBySlug(normalizeSlug(request.slug()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma trilha com este slug");
                });

        var course = Course.builder()
                .slug(normalizeSlug(request.slug()))
                .title(request.title().trim())
                .description(request.description().trim())
                .category(request.category().trim())
                .technology(request.technology().trim())
                .level(request.level())
                .requiredPlan(request.requiredPlan())
                .totalLessons(request.totalLessons() == null ? 0 : request.totalLessons())
                .build();

        var saved = courseRepository.save(course);
        auditService.record(email, "CREATE", "COURSE", saved.getId(), "Trilha criada: " + saved.getTitle());
        return AdminCourseResponse.from(saved);
    }

    @Transactional
    public AdminCourseResponse updateCourse(String email, Long courseId, AdminCourseRequest request) {
        requireAdmin(email);

        var course = findCourse(courseId);
        var normalizedSlug = normalizeSlug(request.slug());

        courseRepository.findBySlug(normalizedSlug)
                .filter(existing -> !existing.getId().equals(courseId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma trilha com este slug");
                });

        course.setSlug(normalizedSlug);
        course.setTitle(request.title().trim());
        course.setDescription(request.description().trim());
        course.setCategory(request.category().trim());
        course.setTechnology(request.technology().trim());
        course.setLevel(request.level());
        course.setRequiredPlan(request.requiredPlan());
        course.setTotalLessons(request.totalLessons() == null ? course.getTotalLessons() : request.totalLessons());

        var saved = courseRepository.save(course);
        auditService.record(email, "UPDATE", "COURSE", saved.getId(), "Trilha atualizada: " + saved.getTitle());
        return AdminCourseResponse.from(saved);
    }

    @Transactional
    public void deleteCourse(String email, Long courseId) {
        requireAdmin(email);

        var course = findCourse(courseId);
        if (enrollmentRepository.existsByCourseId(courseId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A trilha possui alunos matriculados e não pode ser excluída"
            );
        }

        var lessons = lessonRepository.findCurriculumByCourseId(courseId);
        ensureLessonsWithoutActivity(lessons);
        lessons.forEach(this::deleteLessonResources);

        courseRepository.delete(course);
        auditService.record(email, "DELETE", "COURSE", courseId, "Trilha removida: " + course.getTitle());
    }

    @Transactional
    public AdminModuleResponse createModule(String email, Long courseId, AdminModuleRequest request) {
        requireAdmin(email);

        var course = findCourse(courseId);
        int position = request.position() == null
                ? moduleRepository.findMaxPositionByCourseId(courseId) + 1
                : request.position();

        ensureModulePositionAvailable(courseId, position, null);

        var module = CourseModule.builder()
                .course(course)
                .title(request.title().trim())
                .description(request.description().trim())
                .position(position)
                .build();

        var saved = moduleRepository.save(module);
        auditService.record(email, "CREATE", "MODULE", saved.getId(), "Módulo criado: " + saved.getTitle());
        return AdminModuleResponse.from(saved);
    }

    @Transactional
    public AdminModuleResponse updateModule(String email, Long moduleId, AdminModuleRequest request) {
        requireAdmin(email);

        var module = findModule(moduleId);
        int position = request.position() == null ? module.getPosition() : request.position();

        ensureModulePositionAvailable(module.getCourse().getId(), position, moduleId);

        module.setTitle(request.title().trim());
        module.setDescription(request.description().trim());
        module.setPosition(position);

        var saved = moduleRepository.save(module);
        auditService.record(email, "UPDATE", "MODULE", saved.getId(), "Módulo atualizado: " + saved.getTitle());
        return AdminModuleResponse.from(saved);
    }

    @Transactional
    public void deleteModule(String email, Long moduleId) {
        requireAdmin(email);

        var module = findModule(moduleId);
        var lessons = lessonRepository.findByModuleIdOrderByPositionAsc(moduleId);

        ensureLessonsWithoutActivity(lessons);
        lessons.forEach(this::deleteLessonResources);

        moduleRepository.delete(module);
        auditService.record(email, "DELETE", "MODULE", moduleId, "Módulo removido: " + module.getTitle());
    }

    @Transactional
    public AdminLessonResponse createLesson(String email, Long moduleId, AdminLessonRequest request) {
        requireAdmin(email);

        var module = findModule(moduleId);
        var slug = normalizeSlug(request.slug());
        int position = request.position() == null
                ? lessonRepository.findMaxPositionByModuleId(moduleId) + 1
                : request.position();

        ensureLessonSlugAvailable(moduleId, slug, null);
        ensureLessonPositionAvailable(moduleId, position, null);

        var lesson = Lesson.builder()
                .module(module)
                .slug(slug)
                .title(request.title().trim())
                .summary(request.summary().trim())
                .position(position)
                .xpReward(
                        request.xpReward() == null
                                ? settingsService.defaultLessonXp()
                                : request.xpReward()
                )
                .build();

        var saved = lessonRepository.save(lesson);
        auditService.record(email, "CREATE", "LESSON", saved.getId(), "Aula criada: " + saved.getTitle());
        return AdminLessonResponse.from(saved);
    }

    @Transactional
    public AdminLessonResponse updateLesson(String email, Long lessonId, AdminLessonRequest request) {
        requireAdmin(email);

        var lesson = findLesson(lessonId);
        var moduleId = lesson.getModule().getId();
        var slug = normalizeSlug(request.slug());
        int position = request.position() == null ? lesson.getPosition() : request.position();

        ensureLessonSlugAvailable(moduleId, slug, lessonId);
        ensureLessonPositionAvailable(moduleId, position, lessonId);

        lesson.setSlug(slug);
        lesson.setTitle(request.title().trim());
        lesson.setSummary(request.summary().trim());
        lesson.setPosition(position);
        lesson.setXpReward(request.xpReward() == null ? lesson.getXpReward() : request.xpReward());

        var saved = lessonRepository.save(lesson);
        auditService.record(email, "UPDATE", "LESSON", saved.getId(), "Aula atualizada: " + saved.getTitle());
        return AdminLessonResponse.from(saved);
    }

    @Transactional
    public void deleteLesson(String email, Long lessonId) {
        requireAdmin(email);

        var lesson = findLesson(lessonId);
        if (hasStudentActivity(lesson)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A aula possui progresso ou tentativas de quiz e não pode ser excluída"
            );
        }

        deleteLessonResources(lesson);
        lessonRepository.delete(lesson);
        auditService.record(email, "DELETE", "LESSON", lessonId, "Aula removida: " + lesson.getTitle());
    }

    private void ensureLessonsWithoutActivity(java.util.List<Lesson> lessons) {
        boolean hasActivity = lessons.stream().anyMatch(this::hasStudentActivity);

        if (hasActivity) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Existem aulas com progresso ou tentativas de quiz. Edite o conteúdo em vez de excluí-lo"
            );
        }
    }

    private boolean hasStudentActivity(Lesson lesson) {
        if (lessonProgressRepository.existsByLessonId(lesson.getId())) {
            return true;
        }

        return quizRepository.findByLessonId(lesson.getId())
                .map(quiz -> quizAttemptRepository.existsByQuizId(quiz.getId()))
                .orElse(false);
    }

    private void deleteLessonResources(Lesson lesson) {
        var resources = resourceRepository.findByLessonIdOrderByPositionAsc(lesson.getId());
        resources.forEach(resource ->
                storageService.delete(lesson.getId(), resource.getStoredFileName())
        );
        resourceRepository.deleteAll(resources);
    }

    private void ensureModulePositionAvailable(Long courseId, int position, Long currentId) {
        moduleRepository.findByCourseIdAndPosition(courseId, position)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe um módulo nesta posição"
                    );
                });
    }

    private void ensureLessonPositionAvailable(Long moduleId, int position, Long currentId) {
        lessonRepository.findByModuleIdAndPosition(moduleId, position)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe uma aula nesta posição"
                    );
                });
    }

    private void ensureLessonSlugAvailable(Long moduleId, String slug, Long currentId) {
        lessonRepository.findByModuleIdAndSlug(moduleId, slug)
                .filter(existing -> currentId == null || !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Já existe uma aula com este slug neste módulo"
                    );
                });
    }

    private Course findCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trilha não encontrada"));
    }

    private CourseModule findModule(Long moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Módulo não encontrado"));
    }

    private Lesson findLesson(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada"));
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao administrador");
        }
    }
}
