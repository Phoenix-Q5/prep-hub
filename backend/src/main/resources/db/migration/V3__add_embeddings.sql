-- pgvector extension (also in init.sql, idempotent)
CREATE EXTENSION IF NOT EXISTS "vector";

-- Question embeddings table
CREATE TABLE question_embeddings (
    question_id     UUID PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    embedding       vector(384) NOT NULL,
    model_name      VARCHAR(100) NOT NULL DEFAULT 'BAAI/bge-small-en-v1.5',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- IVFFlat index for approximate nearest neighbor search
-- Lists = 4 * sqrt(n). At ~1000 questions, lists=128 is generous.
-- Rebuild with more lists as data grows.
CREATE INDEX idx_question_embeddings_ivfflat
    ON question_embeddings
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 128);

-- Also create an HNSW index (better recall, slightly more memory)
CREATE INDEX idx_question_embeddings_hnsw
    ON question_embeddings
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
