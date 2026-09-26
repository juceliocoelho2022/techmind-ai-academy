package br.com.techmind.academy.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    boolean existsByLessonId(Long lessonId);

    Optional<LessonProgress> findByUserIdAndLessonId(Long userId, Long lessonId);

    @Query("""
            select lp.lesson.id
            from LessonProgress lp
            where lp.user.email = :email
              and lp.lesson.module.course.id = :courseId
            order by lp.completedAt asc
            """)
    List<Long> findCompletedLessonIds(
            @Param("email") String email,
            @Param("courseId") Long courseId
    );
}
