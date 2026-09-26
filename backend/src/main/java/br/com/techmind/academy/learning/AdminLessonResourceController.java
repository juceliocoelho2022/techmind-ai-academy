package br.com.techmind.academy.learning;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminLessonResourceController {

    private final LessonResourceService resourceService;

    public AdminLessonResourceController(LessonResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @PostMapping(
            value = "/lessons/{lessonId}/resources",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public LessonResourceResponse upload(
            Authentication authentication,
            @PathVariable Long lessonId,
            @RequestParam LessonResourceType type,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file
    ) {
        return resourceService.upload(
                authentication.getName(),
                lessonId,
                type,
                title,
                description,
                file
        );
    }

    @DeleteMapping("/resources/{resourceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            Authentication authentication,
            @PathVariable Long resourceId
    ) {
        resourceService.delete(authentication.getName(), resourceId);
    }
}
