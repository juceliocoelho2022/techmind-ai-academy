package br.com.techmind.academy.learning;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class LessonResourceController {

    private final LessonResourceService resourceService;

    public LessonResourceController(LessonResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/api/v1/lessons/{lessonId}/resources")
    public List<LessonResourceResponse> list(@PathVariable Long lessonId) {
        return resourceService.list(lessonId);
    }

    @GetMapping("/api/v1/learning/resources/{resourceId}/download")
    public ResponseEntity<org.springframework.core.io.Resource> download(
            Authentication authentication,
            @PathVariable Long resourceId
    ) {
        var result = resourceService.download(authentication.getName(), resourceId);
        var metadata = result.metadata();

        var disposition = ContentDisposition.attachment()
                .filename(metadata.getOriginalFileName(), StandardCharsets.UTF_8)
                .build();

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(metadata.getContentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(metadata.getSizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(result.file());
    }
}
