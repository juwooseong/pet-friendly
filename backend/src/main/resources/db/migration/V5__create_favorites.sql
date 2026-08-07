-- Flyway V5__create_favorites.sql: Create favorites table connecting users and places

CREATE TABLE favorites (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorites_user_place UNIQUE (user_id, place_id)
);

CREATE INDEX idx_favorite_user ON favorites(user_id);
CREATE INDEX idx_favorite_place ON favorites(place_id);
