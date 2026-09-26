package br.com.techmind.academy.learning;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class LessonResourceStorageService {

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "avi", "webm");
    private static final Set<String> ZIP_EXTENSIONS = Set.of("zip");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> EBOOK_EXTENSIONS = Set.of("pdf", "epub");

    private final Path root;
    private final long maxBytes;

    public LessonResourceStorageService(
            @Value("${app.storage.learning-resources-root:./storage/learning-resources}") String root,
            @Value("${app.storage.learning-resources-max-bytes:524288000}") long maxBytes
    ) {
        this.root = Paths.get(root).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    public StoredFile store(Long lessonId, LessonResourceType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo obrigatório");
        }

        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo excede o limite permitido");
        }

        var original = sanitizeOriginalName(file.getOriginalFilename());
        var extension = extensionOf(original);
        validateExtension(type, extension);

        var storedFileName = UUID.randomUUID() + "." + extension;
        var lessonDirectory = root.resolve(String.valueOf(lessonId)).normalize();
        var target = lessonDirectory.resolve(storedFileName).normalize();

        if (!target.startsWith(lessonDirectory)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido");
        }

        try {
            Files.createDirectories(lessonDirectory);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao armazenar material");
        }

        var contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new StoredFile(original, storedFileName, contentType, file.getSize());
    }

    public Resource load(Long lessonId, String storedFileName) {
        var lessonDirectory = root.resolve(String.valueOf(lessonId)).normalize();
        var file = lessonDirectory.resolve(storedFileName).normalize();

        if (!file.startsWith(lessonDirectory) || !Files.exists(file) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado");
        }

        return new FileSystemResource(file);
    }

    public void delete(Long lessonId, String storedFileName) {
        var lessonDirectory = root.resolve(String.valueOf(lessonId)).normalize();
        var file = lessonDirectory.resolve(storedFileName).normalize();

        if (!file.startsWith(lessonDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao remover material");
        }
    }

    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido");
        }

        var normalized = originalName.replace("\\", "/");
        var name = normalized.substring(normalized.lastIndexOf('/') + 1).trim();

        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome de arquivo inválido");
        }

        return name;
    }

    private String extensionOf(String fileName) {
        int index = fileName.lastIndexOf('.');
        if (index < 0 || index == fileName.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo sem extensão suportada");
        }
        return fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private void validateExtension(LessonResourceType type, String extension) {
        boolean valid = switch (type) {
            case VIDEO -> VIDEO_EXTENSIONS.contains(extension);
            case PROJECT_ZIP -> ZIP_EXTENSIONS.contains(extension);
            case IMAGE -> IMAGE_EXTENSIONS.contains(extension);
            case EBOOK -> EBOOK_EXTENSIONS.contains(extension);
        };

        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Extensão incompatível com o tipo de material"
            );
        }
    }

    public record StoredFile(
            String originalFileName,
            String storedFileName,
            String contentType,
            long sizeBytes
    ) {
    }
}
