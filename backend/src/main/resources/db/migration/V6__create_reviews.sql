-- Flyway V6__create_reviews.sql: Create reviews table connecting users and places with rating and soft delete support

CREATE TABLE reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    content VARCHAR(1000) NOT NULL,
    image_url TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_reviews_user_place UNIQUE (user_id, place_id)
);

CREATE INDEX idx_review_place ON reviews(place_id);
CREATE INDEX idx_review_user ON reviews(user_id);
