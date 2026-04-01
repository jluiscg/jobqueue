-- V1: Initial jobs table schema
--
-- Design notes:
-- - UUID primary keys for distributed-safe uniqueness
-- - JSONB for flexible payload/result storage (enables future querying)
-- - CHECK constraints enforce state machine and time invariants at DB level

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE jobs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type             VARCHAR(100)  NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'QUEUED',
    priority         INT           NOT NULL DEFAULT 5,
    payload          JSONB,
    result           JSONB,
    attempt_count    INT           NOT NULL DEFAULT 0,
    max_retries      INT           NOT NULL DEFAULT 3,
    run_at           TIMESTAMP WITH TIME ZONE,
    started_at       TIMESTAMP WITH TIME ZONE,
    completed_at     TIMESTAMP WITH TIME ZONE,
    locked_by        VARCHAR(255),
    lock_expires_at  TIMESTAMP WITH TIME ZONE,
    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_jobs_status
        CHECK (status IN ('QUEUED', 'RUNNING', 'RETRYING', 'COMPLETED', 'DEAD', 'CANCELLED')),
    CONSTRAINT chk_jobs_priority
        CHECK (priority >= 0),
    CONSTRAINT chk_jobs_attempt_count
        CHECK (attempt_count >= 0),
    CONSTRAINT chk_jobs_max_retries
        CHECK (max_retries >= 0),
    CONSTRAINT chk_jobs_completed_after_start
        CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at),
    CONSTRAINT chk_jobs_lock_expires_after_start
        CHECK (lock_expires_at IS NULL OR started_at IS NULL OR lock_expires_at >= started_at)
);

-- Claim path index: workers claim queued jobs by priority/FIFO.
CREATE INDEX idx_jobs_queued_priority_created
    ON jobs (priority DESC, created_at ASC)
    WHERE status = 'QUEUED';
