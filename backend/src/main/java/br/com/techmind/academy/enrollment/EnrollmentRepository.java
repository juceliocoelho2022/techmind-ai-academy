package br.com.techmind.academy.enrollment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserEmailOrderByStartedAtDesc(String email);

    @EntityGraph(attributePaths = "course")
    List<Enrollment> findByUserIdOrderByStartedAtDesc(Long userId);

    @EntityGraph(attributePaths = "course")
    @Query("select e from Enrollment e")
    List<Enrollment> findAllForAnalytics();

    @EntityGraph(attributePaths = "course")
    Optional<Enrollment> findByUserEmailAndCourseId(String email, Long courseId);

    boolean existsByUserEmailAndCourseId(String email, Long courseId);

    boolean existsByCourseId(Long courseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from Enrollment e
            join fetch e.course
            where e.user.email = :email
              and e.course.id = :courseId
            """)
    Optional<Enrollment> findForUpdateByUserEmailAndCourseId(
            @Param("email") String email,
            @Param("courseId") Long courseId
    );
}
