ALTER TABLE users
    ADD COLUMN reminder_days INTEGER NOT NULL DEFAULT 3
    CHECK (reminder_days BETWEEN 0 AND 30);
