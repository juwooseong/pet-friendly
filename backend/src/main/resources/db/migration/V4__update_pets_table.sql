-- Flyway V4__update_pets_table.sql: Add gender, birth_date, is_neutered, is_representative columns to pets table and add indexes

ALTER TABLE pets ADD COLUMN IF NOT EXISTS gender VARCHAR(20) NOT NULL DEFAULT 'MALE';
ALTER TABLE pets ADD COLUMN IF NOT EXISTS birth_date DATE;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS is_neutered BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pets ADD COLUMN IF NOT EXISTS is_representative BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_pets_user_id ON pets(user_id);
CREATE INDEX IF NOT EXISTS idx_pets_user_representative ON pets(user_id, is_representative);
