ALTER TABLE lesson_resources
DROP CONSTRAINT ck_lesson_resource_type;

ALTER TABLE lesson_resources
ADD CONSTRAINT ck_lesson_resource_type
CHECK (type IN ('VIDEO', 'PROJECT_ZIP', 'IMAGE', 'EBOOK'));
