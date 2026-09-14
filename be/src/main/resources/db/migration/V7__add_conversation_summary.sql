ALTER TABLE conversations
    ADD COLUMN summary TEXT,
    ADD COLUMN summarized_through_count INTEGER NOT NULL DEFAULT 0;
