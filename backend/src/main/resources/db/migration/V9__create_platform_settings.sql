CREATE TABLE platform_settings (
    id SMALLINT PRIMARY KEY,
    academy_name VARCHAR(120) NOT NULL,
    tagline VARCHAR(240) NOT NULL,
    support_email VARCHAR(200),
    registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    default_lesson_xp INTEGER NOT NULL DEFAULT 10,
    default_quiz_passing_score INTEGER NOT NULL DEFAULT 70,
    default_quiz_xp INTEGER NOT NULL DEFAULT 50,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_platform_settings_singleton CHECK (id = 1),
    CONSTRAINT ck_default_lesson_xp_nonnegative CHECK (default_lesson_xp >= 0),
    CONSTRAINT ck_default_quiz_passing_score CHECK (default_quiz_passing_score BETWEEN 0 AND 100),
    CONSTRAINT ck_default_quiz_xp_nonnegative CHECK (default_quiz_xp >= 0)
);

INSERT INTO platform_settings (
    id,
    academy_name,
    tagline,
    support_email,
    registration_enabled,
    default_lesson_xp,
    default_quiz_passing_score,
    default_quiz_xp
) VALUES (
    1,
    'TechMind AI Academy',
    'Do conteúdo ao projeto real.',
    NULL,
    TRUE,
    10,
    70,
    50
);
