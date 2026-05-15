CREATE TABLE consultations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    patient_query VARCHAR(500) NOT NULL,
    diagnosis VARCHAR(2000),
    prescription VARCHAR(2000),
    status VARCHAR(50) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_minutes INTEGER,
    total_amount NUMERIC(10,2) NOT NULL,
    doctor_earnings NUMERIC(10,2),
    platform_commission NUMERIC(10,2),
    video_session_id VARCHAR(500),
    whatsapp_number VARCHAR(20),
    patient_rating INTEGER,
    patient_review VARCHAR(500),
    payment_status VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consultations_patient_id ON consultations(patient_id);
CREATE INDEX idx_consultations_doctor_id ON consultations(doctor_id);
CREATE INDEX idx_consultations_status ON consultations(status);
CREATE INDEX idx_consultations_scheduled_at ON consultations(scheduled_at);
CREATE INDEX idx_consultations_created_at ON consultations(created_at DESC);
