ALTER TABLE courses
    ADD COLUMN required_plan VARCHAR(20) NOT NULL DEFAULT 'FREE';

UPDATE courses
SET required_plan = 'PRO'
WHERE slug IN (
    'java-backend',
    'spring-boot',
    'spring-boot-profissional',
    'react-js',
    'postgresql',
    'aws-cloud',
    'aws-cloud-profissional',
    'devops',
    'ia-generativa',
    'android-kotlin',
    'data-ai'
);

UPDATE courses
SET required_plan = 'CAREER'
WHERE slug = 'entrevistas-tech';

ALTER TABLE courses
    ADD CONSTRAINT ck_course_required_plan
    CHECK (required_plan IN ('FREE', 'PRO', 'CAREER'));
