CREATE TABLE doctors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) NOT NULL UNIQUE,
    license_country VARCHAR(100),
    specialization VARCHAR(200) NOT NULL,
    bio TEXT,
    years_of_experience INTEGER,
    consultation_fee_per_minute NUMERIC(10,2) NOT NULL,
    license_status VARCHAR(50) NOT NULL,
    availability_status VARCHAR(50) NOT NULL,
    license_verification_date TIMESTAMP,
    rating NUMERIC(3,2) DEFAULT 0,
    total_consultations BIGINT DEFAULT 0,
    total_ratings BIGINT DEFAULT 0,
    education_background TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doctors_user_id ON doctors(user_id);
CREATE INDEX idx_doctors_license_status ON doctors(license_status);
CREATE INDEX idx_doctors_availability ON doctors(availability_status);
CREATE INDEX idx_doctors_rating ON doctors(rating DESC);
