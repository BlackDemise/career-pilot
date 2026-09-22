CREATE TABLE cv_analysis_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    cv_id UUID NOT NULL REFERENCES cvs (id),
    type VARCHAR(32) NOT NULL CHECK (type IN ('REVIEW', 'JD_MATCH')),
    status VARCHAR(32) NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED')),
    stage VARCHAR(64) NOT NULL,
    job_description TEXT,
    analysis_id UUID UNIQUE REFERENCES cv_analyses (id),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cv_analysis_jobs_user_id ON cv_analysis_jobs (user_id);
CREATE INDEX idx_cv_analysis_jobs_cv_id ON cv_analysis_jobs (cv_id);