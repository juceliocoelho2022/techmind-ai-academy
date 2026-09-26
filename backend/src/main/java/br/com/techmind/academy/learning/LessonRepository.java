package br.com.techmind.academy.learning;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("""
            select l
            from Lesson l
            join fetch l.module m
            join fetch m.course c
            where c.id = :courseId
            order by m.position asc, l.position asc
            """)
    List<Lesson> findCurriculumByCourseId(@Param("courseId") Long courseId);

    List<Lesson> findByModuleIdOrderByPositionAsc(Long moduleId);

    Optional<Lesson> findByModuleIdAndPosition(Long moduleId, Integer position);

    Optional<Lesson> findByModuleIdAndSlug(Long moduleId, String slug);

    @Query("""
            select coalesce(max(l.position), 0)
            from Lesson l
            where l.module.id = :moduleId
            """)
    int findMaxPositionByModuleId(@Param("moduleId") Long moduleId);

    @Override
    @EntityGraph(attributePaths = {"module", "module.course"})
    Optional<Lesson> findById(Long id);
}
