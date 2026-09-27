package br.com.techmind.academy.learning;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/course-templates")
public class CourseTemplateCatalogController {

    private final CourseTemplateCatalogService service;

    public CourseTemplateCatalogController(CourseTemplateCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public List<CourseTemplateResponse> catalog(Authentication authentication) {
        return service.catalog(authentication.getName());
    }

    @PostMapping("/{templateKey}/instantiate")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCourseResponse instantiate(
            Authentication authentication,
            @PathVariable String templateKey,
            @Valid @RequestBody CourseTemplateInstantiateRequest request
    ) {
        return service.instantiate(authentication.getName(), templateKey, request);
    }
}
