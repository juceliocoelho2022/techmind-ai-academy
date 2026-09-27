ALTER TABLE courses
    ADD COLUMN category VARCHAR(80) NOT NULL DEFAULT 'Geral',
    ADD COLUMN technology VARCHAR(80) NOT NULL DEFAULT 'Geral',
    ADD COLUMN level VARCHAR(30) NOT NULL DEFAULT 'INTERMEDIATE';

UPDATE courses
SET category = 'Backend', technology = 'Java', level = 'INTERMEDIATE'
WHERE slug = 'java-backend';

UPDATE courses
SET category = 'Backend', technology = 'Spring Boot', level = 'INTERMEDIATE'
WHERE slug = 'spring-boot';

UPDATE courses
SET category = 'Cloud', technology = 'AWS', level = 'BEGINNER'
WHERE slug = 'aws-cloud';

UPDATE courses
SET category = 'Dados & IA', technology = 'Dados e IA', level = 'INTERMEDIATE'
WHERE slug = 'data-ai';

ALTER TABLE courses
    ADD CONSTRAINT ck_course_level
    CHECK (level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED'));
