ALTER TABLE users
    ADD COLUMN preferred_language VARCHAR(100),
    ADD COLUMN response_style VARCHAR(100),
    ADD COLUMN technical_background TEXT,
    ADD COLUMN career_goal TEXT,
    ADD COLUMN custom_instructions TEXT;