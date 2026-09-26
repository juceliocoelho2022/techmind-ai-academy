CREATE TABLE lesson_resources (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL UNIQUE,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    position INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lesson_resource_position UNIQUE (lesson_id, position),
    CONSTRAINT ck_lesson_resource_size_nonnegative CHECK (size_bytes >= 0),
    CONSTRAINT ck_lesson_resource_position_positive CHECK (position > 0),
    CONSTRAINT ck_lesson_resource_type CHECK (type IN ('PROJECT_ZIP', 'IMAGE', 'EBOOK'))
);

CREATE INDEX idx_lesson_resources_lesson_id ON lesson_resources(lesson_id);
