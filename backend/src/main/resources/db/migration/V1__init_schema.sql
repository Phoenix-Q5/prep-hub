-- Enable required extensions (also done at container init, idempotent here)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";
CREATE EXTENSION IF NOT EXISTS "btree_gin";
CREATE EXTENSION IF NOT EXISTS "citext";

-- Users
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username        VARCHAR(64)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    display_name    VARCHAR(100),
    bio             VARCHAR(500),
    avatar_key      VARCHAR(255),
    role            VARCHAR(16)  NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);
CREATE INDEX idx_users_username_trgm ON users USING gin (lower(username) gin_trgm_ops);

-- Portfolios (1:1 with users)
CREATE TABLE portfolios (
    user_id                       UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    posts_count                   INT  NOT NULL DEFAULT 0,
    suggestions_count             INT  NOT NULL DEFAULT 0,
    accepted_suggestions_count    INT  NOT NULL DEFAULT 0,
    likes_received                INT  NOT NULL DEFAULT 0,
    reputation                    INT  NOT NULL DEFAULT 0,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Topics
CREATE TABLE topics (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(120) NOT NULL,
    description     VARCHAR(500),
    color_hex       VARCHAR(7),
    parent_id       UUID REFERENCES topics(id) ON DELETE SET NULL,
    question_count  INT  NOT NULL DEFAULT 0,
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_topics_slug UNIQUE (slug)
);
CREATE INDEX idx_topics_parent ON topics(parent_id);
CREATE INDEX idx_topics_featured ON topics(is_featured) WHERE is_featured = TRUE;

-- Questions
CREATE TABLE questions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(300) NOT NULL,
    content         TEXT NOT NULL,
    topic_id        UUID NOT NULL REFERENCES topics(id) ON DELETE RESTRICT,
    author_id       UUID NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
    difficulty      VARCHAR(16) NOT NULL DEFAULT 'MEDIUM',
    status          VARCHAR(16) NOT NULL DEFAULT 'PUBLISHED',
    view_count      BIGINT NOT NULL DEFAULT 0,
    like_count      INT    NOT NULL DEFAULT 0,
    answer_count    INT    NOT NULL DEFAULT 0,
    tags            TEXT[] NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_questions_topic   ON questions(topic_id);
CREATE INDEX idx_questions_author  ON questions(author_id);
CREATE INDEX idx_questions_status  ON questions(status);
CREATE INDEX idx_questions_created ON questions(created_at DESC);
CREATE INDEX idx_questions_tags    ON questions USING gin (tags);
CREATE INDEX idx_questions_title_trgm ON questions USING gin (lower(title) gin_trgm_ops);

-- Answers
CREATE TABLE answers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    author_id       UUID NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    is_official     BOOLEAN NOT NULL DEFAULT FALSE,
    like_count      INT NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_answers_question ON answers(question_id);

-- Likes
CREATE TABLE question_likes (
    user_id         UUID NOT NULL REFERENCES users(id)     ON DELETE CASCADE,
    question_id     UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, question_id)
);
CREATE INDEX idx_qlikes_question ON question_likes(question_id);

-- Suggestions
CREATE TABLE suggestions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    type            VARCHAR(32) NOT NULL,
    user_id         UUID NOT NULL REFERENCES users(id),
    question_id     UUID REFERENCES questions(id) ON DELETE SET NULL,
    payload         JSONB NOT NULL DEFAULT '{}'::jsonb,
    rationale       VARCHAR(500),
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewed_by     UUID REFERENCES users(id),
    reviewed_at     TIMESTAMPTZ,
    review_notes    VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_suggestions_status ON suggestions(status);
CREATE INDEX idx_suggestions_user   ON suggestions(user_id);
CREATE INDEX idx_suggestions_payload_gin ON suggestions USING gin (payload);
