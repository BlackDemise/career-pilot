ALTER TABLE cvs
    ADD COLUMN structured_extraction_json TEXT,
    ADD COLUMN structured_extraction_version INTEGER,
    ADD COLUMN extraction_status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    ADD COLUMN extraction_error TEXT;

ALTER TABLE cvs
    ADD CONSTRAINT cvs_extraction_status_check
    CHECK (extraction_status IN ('NOT_STARTED', 'PROCESSING', 'COMPLETED', 'FAILED'));

CREATE TABLE cv_extraction_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    cv_id UUID NOT NULL REFERENCES cvs (id),
    status VARCHAR(32) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    stage VARCHAR(64) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cv_extraction_jobs_user_id ON cv_extraction_jobs (user_id);
CREATE INDEX idx_cv_extraction_jobs_cv_id ON cv_extraction_jobs (cv_id);