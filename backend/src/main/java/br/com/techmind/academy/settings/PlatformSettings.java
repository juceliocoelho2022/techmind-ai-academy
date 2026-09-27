package br.com.techmind.academy.settings;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformSettings {

    @Id
    private Short id;

    @Column(name = "academy_name", nullable = false, length = 120)
    private String academyName;

    @Column(nullable = false, length = 240)
    private String tagline;

    @Column(name = "support_email", length = 200)
    private String supportEmail;

    @Column(name = "whatsapp_number", length = 30)
    private String whatsappNumber;

    @Column(name = "registration_enabled", nullable = false)
    private Boolean registrationEnabled;

    @Column(name = "default_lesson_xp", nullable = false)
    private Integer defaultLessonXp;

    @Column(name = "default_quiz_passing_score", nullable = false)
    private Integer defaultQuizPassingScore;

    @Column(name = "default_quiz_xp", nullable = false)
    private Integer defaultQuizXp;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        if (id == null) id = 1;
        if (updatedAt == null) updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
