package br.com.techmind.academy.enrollment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserEmailOrderByStartedAtDesc(String email);

    @EntityGraph(attributePaths = "course")
    Optional<Enrollment> findByUserEmailAndCourseId(String email, Long courseId);
}
