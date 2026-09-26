package br.com.techmind.academy.learning;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonResourceRepository extends JpaRepository<LessonResource, Long> {

    List<LessonResource> findByLessonIdOrderByPositionAsc(Long lessonId);

    @Query("""
            select r
            from LessonResource r
            join fetch r.lesson l
            join fetch l.module m
            join fetch m.course c
            where c.id = :courseId
            order by l.id asc, r.position asc
            """)
    List<LessonResource> findByCourseId(@Param("courseId") Long courseId);

    @Query("""
            select coalesce(max(r.position), 0)
            from LessonResource r
            where r.lesson.id = :lessonId
            """)
    int findMaxPositionByLessonId(@Param("lessonId") Long lessonId);

    @Override
    @EntityGraph(attributePaths = {"lesson", "lesson.module", "lesson.module.course"})
    Optional<LessonResource> findById(Long id);
}
