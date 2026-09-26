package br.com.techmind.academy.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

    List<CourseModule> findByCourseIdOrderByPositionAsc(Long courseId);

    Optional<CourseModule> findByCourseIdAndPosition(Long courseId, Integer position);

    @Query("""
            select coalesce(max(m.position), 0)
            from CourseModule m
            where m.course.id = :courseId
            """)
    int findMaxPositionByCourseId(@Param("courseId") Long courseId);
}
