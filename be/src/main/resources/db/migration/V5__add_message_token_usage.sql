ALTER TABLE messages
    ADD COLUMN prompt_tokens INTEGER,
    ADD COLUMN completion_tokens INTEGER,
    ADD COLUMN total_tokens INTEGER;
