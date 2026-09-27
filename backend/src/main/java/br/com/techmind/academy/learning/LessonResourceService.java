package br.com.techmind.academy.learning;

import br.com.techmind.academy.audit.AdminAuditService;
import br.com.techmind.academy.enrollment.EnrollmentRepository;
import br.com.techmind.academy.user.UserRepository;
import br.com.techmind.academy.user.UserRole;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class LessonResourceService {

    private final LessonRepository lessonRepository;
    private final LessonResourceRepository resourceRepository;
    private final LessonResourceStorageService storageService;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AdminAuditService auditService;

    public LessonResourceService(
            LessonRepository lessonRepository,
            LessonResourceRepository resourceRepository,
            LessonResourceStorageService storageService,
            UserRepository userRepository,
            EnrollmentRepository enrollmentRepository,
            AdminAuditService auditService
    ) {
        this.lessonRepository = lessonRepository;
        this.resourceRepository = resourceRepository;
        this.storageService = storageService;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<LessonResourceResponse> list(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada");
        }

        return resourceRepository.findByLessonIdOrderByPositionAsc(lessonId)
                .stream()
                .map(LessonResourceResponse::from)
                .toList();
    }

    @Transactional
    public LessonResourceResponse upload(
            String email,
            Long lessonId,
            LessonResourceType type,
            String title,
            String description,
            MultipartFile file
    ) {
        requireAdmin(email);

        var lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aula não encontrada"));

        var stored = storageService.store(lessonId, type, file);
        int nextPosition = resourceRepository.findMaxPositionByLessonId(lessonId) + 1;

        try {
            var resource = LessonResource.builder()
                    .lesson(lesson)
                    .type(type)
                    .title(requireTitle(title))
                    .description(normalizeDescription(description))
                    .originalFileName(stored.originalFileName())
                    .storedFileName(stored.storedFileName())
                    .contentType(stored.contentType())
                    .sizeBytes(stored.sizeBytes())
                    .position(nextPosition)
                    .build();

            var saved = resourceRepository.save(resource);
            auditService.record(
                    email,
                    "UPLOAD",
                    "LESSON_RESOURCE",
                    saved.getId(),
                    "Material " + saved.getType().name() + " enviado para a aula " + lessonId
            );
            return LessonResourceResponse.from(saved);
        } catch (RuntimeException exception) {
            storageService.delete(lessonId, stored.storedFileName());
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public DownloadResult download(String email, Long resourceId) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        var metadata = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));

        var courseId = metadata.getLesson().getModule().getCourse().getId();
        boolean admin = user.getRole() == UserRole.ADMIN;
        boolean enrolled = enrollmentRepository.existsByUserEmailAndCourseId(email, courseId);

        if (!admin && !enrolled) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Matricule-se na trilha para baixar este material"
            );
        }

        Resource file = storageService.load(metadata.getLesson().getId(), metadata.getStoredFileName());
        return new DownloadResult(metadata, file);
    }

    @Transactional
    public void delete(String email, Long resourceId) {
        requireAdmin(email);

        var metadata = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));

        storageService.delete(metadata.getLesson().getId(), metadata.getStoredFileName());
        resourceRepository.delete(metadata);
        auditService.record(
                email,
                "DELETE",
                "LESSON_RESOURCE",
                resourceId,
                "Material removido da aula " + metadata.getLesson().getId()
        );
    }

    private void requireAdmin(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        if (user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito ao administrador");
        }
    }

    private String requireTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título do material é obrigatório");
        }

        var normalized = title.trim();
        if (normalized.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Título do material excede 200 caracteres");
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        var normalized = description.trim();
        if (normalized.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Descrição excede 500 caracteres");
        }
        return normalized;
    }

    public record DownloadResult(LessonResource metadata, Resource file) {
    }
}
