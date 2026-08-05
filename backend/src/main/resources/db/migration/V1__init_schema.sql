-- Flyway V1__init_schema.sql: Initial PetSpot Schema with PostGIS Spatial Extension

-- 1. Ensure PostGIS is enabled
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Pets Table
CREATE TABLE pets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(20) NOT NULL DEFAULT 'DOG', -- 'DOG', 'CAT'
    breed VARCHAR(100) NOT NULL,
    weight_kg NUMERIC(4,1) NOT NULL,
    size_category VARCHAR(20) NOT NULL, -- 'SMALL' (<=10kg), 'MEDIUM' (<=20kg), 'LARGE' (>20kg)
    age_years INT DEFAULT 1,
    is_vaccinated BOOLEAN DEFAULT TRUE,
    photo_url TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Places Table with PostGIS Location Point
CREATE TABLE places (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_data_id VARCHAR(100) UNIQUE,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL, -- 'CAFE', 'HOTEL', 'PARK', 'HOSPITAL', 'SALON'
    category_name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    location GEOMETRY(Point, 4326) NOT NULL, -- PostGIS Spatial Point (WGS 84)
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    phone VARCHAR(50),
    operating_hours TEXT,
    image_url TEXT,
    description TEXT,
    rating NUMERIC(2,1) DEFAULT 5.0,
    review_count INT DEFAULT 0,
    facilities JSONB DEFAULT '[]',
    -- Pet Policy JSONB & Rules
    max_weight_limit_kg NUMERIC(4,1), -- NULL이면 제한 없음
    allowed_sizes JSONB DEFAULT '["SMALL", "MEDIUM", "LARGE"]',
    is_indoor_allowed BOOLEAN DEFAULT TRUE,
    is_outdoor_allowed BOOLEAN DEFAULT TRUE,
    has_off_leash_zone BOOLEAN DEFAULT FALSE,
    policy_checklist JSONB DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Place Reviews Table
CREATE TABLE place_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating NUMERIC(2,1) NOT NULL CHECK (rating >= 1.0 AND rating <= 5.0),
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. Spatial Indexing for Fast Radius Queries (< 50ms NFR)
CREATE INDEX idx_places_location ON places USING GIST (location);

-- 7. Additional B-Tree Indexes
CREATE INDEX idx_pets_user_id ON pets(user_id);
CREATE INDEX idx_places_category ON places(category);
CREATE INDEX idx_place_reviews_place_id ON place_reviews(place_id);
