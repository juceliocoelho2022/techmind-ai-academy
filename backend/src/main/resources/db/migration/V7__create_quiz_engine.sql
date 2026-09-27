CREATE TABLE lesson_quizzes (
    id BIGSERIAL PRIMARY KEY,
    lesson_id BIGINT NOT NULL UNIQUE REFERENCES lessons(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    passing_score INTEGER NOT NULL DEFAULT 70,
    xp_reward INTEGER NOT NULL DEFAULT 50,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_quiz_passing_score CHECK (passing_score BETWEEN 0 AND 100),
    CONSTRAINT ck_quiz_xp_nonnegative CHECK (xp_reward >= 0)
);

CREATE TABLE quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES lesson_quizzes(id) ON DELETE CASCADE,
    prompt VARCHAR(1000) NOT NULL,
    position INTEGER NOT NULL,
    CONSTRAINT uk_quiz_question_position UNIQUE (quiz_id, position),
    CONSTRAINT ck_quiz_question_position_positive CHECK (position > 0)
);

CREATE TABLE quiz_options (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_text VARCHAR(500) NOT NULL,
    position INTEGER NOT NULL,
    correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_quiz_option_position UNIQUE (question_id, position),
    CONSTRAINT ck_quiz_option_position_positive CHECK (position > 0)
);

CREATE TABLE quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES lesson_quizzes(id) ON DELETE RESTRICT,
    user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    score NUMERIC(5,2) NOT NULL,
    correct_answers INTEGER NOT NULL,
    total_questions INTEGER NOT NULL,
    passed BOOLEAN NOT NULL,
    xp_awarded INTEGER NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_quiz_attempt_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT ck_quiz_attempt_correct_nonnegative CHECK (correct_answers >= 0),
    CONSTRAINT ck_quiz_attempt_total_positive CHECK (total_questions > 0),
    CONSTRAINT ck_quiz_attempt_xp_nonnegative CHECK (xp_awarded >= 0)
);

CREATE TABLE quiz_attempt_answers (
    id BIGSERIAL PRIMARY KEY,
    attempt_id BIGINT NOT NULL REFERENCES quiz_attempts(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES quiz_questions(id) ON DELETE RESTRICT,
    option_id BIGINT NOT NULL REFERENCES quiz_options(id) ON DELETE RESTRICT,
    correct BOOLEAN NOT NULL,
    CONSTRAINT uk_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE INDEX idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);
CREATE INDEX idx_quiz_options_question_id ON quiz_options(question_id);
CREATE INDEX idx_quiz_attempts_quiz_user ON quiz_attempts(quiz_id, user_id);
CREATE INDEX idx_quiz_attempt_answers_attempt_id ON quiz_attempt_answers(attempt_id);
