CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_conversations_user_id ON conversations (user_id);

CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES conversations (id),
    role VARCHAR(32) NOT NULL CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM')),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_messages_conversation_id ON messages (conversation_id);

CREATE TABLE cvs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    file_name VARCHAR(255) NOT NULL,
    extracted_text TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cvs_user_id ON cvs (user_id);

CREATE TABLE cv_analyses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cv_id UUID NOT NULL REFERENCES cvs (id),
    type VARCHAR(32) NOT NULL CHECK (type IN ('REVIEW', 'JD_MATCH')),
    job_description TEXT,
    result_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_cv_analyses_cv_id ON cv_analyses (cv_id);

CREATE TABLE interview_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users (id),
    role VARCHAR(255) NOT NULL,
    level VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    difficulty VARCHAR(255) NOT NULL,
    num_questions INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('SETUP', 'IN_PROGRESS', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_interview_sessions_user_id ON interview_sessions (user_id);

CREATE TABLE interview_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES interview_sessions (id),
    order_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    topic VARCHAR(255),
    difficulty VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_interview_questions_session_id ON interview_questions (session_id);

CREATE TABLE interview_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL UNIQUE REFERENCES interview_questions (id),
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE interview_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    answer_id UUID NOT NULL UNIQUE REFERENCES interview_answers (id),
    score INTEGER NOT NULL,
    strengths TEXT,
    weaknesses TEXT,
    feedback TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
