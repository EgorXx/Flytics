CREATE TABLE tasks
(
    id           BIGSERIAL PRIMARY KEY,
    payload      JSONB       NOT NULL,
    priority     INT         NOT NULL DEFAULT 0,
    status       VARCHAR(32) NOT NULL,
    attempts     INT         NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    scheduled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_tasks_status
        CHECK (status IN ('READY', 'RUNNING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_tasks_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_tasks_ready_priority_created_at
    ON tasks (priority DESC, created_at ASC)
    WHERE status = 'READY';

CREATE INDEX idx_tasks_ready_scheduled_at
    ON tasks (scheduled_at)
    WHERE status = 'READY';

CREATE INDEX idx_tasks_completed_at
    ON tasks (completed_at)
    WHERE status = 'COMPLETED';