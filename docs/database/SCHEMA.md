# Database Schema

## Overview

The telemedicine platform uses a microservices database pattern where each service has its own PostgreSQL database. This provides:
- Service independence and scalability
- Data isolation
- Easier schema evolution
- Reduced coupling between services

## Database Instances

1. **telemedicine_auth** - Authentication & user credentials
2. **telemedicine_consultation** - Consultation & doctor data
3. **telemedicine_payment** - Payment records & invoices
4. **telemedicine_notification** - Notification logs & templates

## Shared Tables

### users
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100),
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING_VERIFICATION', 'VERIFIED', 'SUSPENDED', 'DELETED')),
    profile_picture_url VARCHAR(500),
    email_verified BOOLEAN DEFAULT false,
    phone_verified BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);
```

**Columns:**
- `id`: Unique identifier (UUID v4)
- `email`: User email, must be unique
- `phone`: User phone number, must be unique
- `firstName`: User's first name (required)
- `lastName`: User's last name (optional)
- `passwordHash`: Bcrypt hashed password (never stored in plain text)
- `role`: User type (PATIENT, DOCTOR, ADMIN)
- `status`: Account lifecycle (PENDING_VERIFICATION → VERIFIED → SUSPENDED/DELETED)
- `profilePictureUrl`: URL to user's profile photo
- `emailVerified`: Flag indicating email verification
- `phoneVerified`: Flag indicating phone verification
- `isActive`: Soft delete flag
- `createdAt`: Account creation timestamp
- `updatedAt`: Last profile update timestamp

---

## Auth Service Database

### user_credentials
```sql
CREATE TABLE user_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    failed_login_attempts INTEGER DEFAULT 0,
    account_locked BOOLEAN DEFAULT false,
    last_login_ip VARCHAR(45),
    last_login_user_agent VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_credentials_user_id ON user_credentials(user_id);
```

**Purpose:** Additional authentication metadata

---

## Consultation Service Database

### doctors
```sql
CREATE TABLE doctors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    license_number VARCHAR(100) NOT NULL UNIQUE,
    license_country VARCHAR(100),
    specialization VARCHAR(200) NOT NULL,
    bio TEXT,
    years_of_experience INTEGER,
    consultation_fee_per_minute NUMERIC(10,2) NOT NULL,
    license_status VARCHAR(50) NOT NULL CHECK (license_status IN ('PENDING_VERIFICATION', 'VERIFIED', 'REJECTED', 'EXPIRED')),
    availability_status VARCHAR(50) NOT NULL CHECK (availability_status IN ('AVAILABLE', 'BUSY', 'OFFLINE')),
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
```

**Purpose:** Doctor profile, credentials, and availability

---

### consultations
```sql
CREATE TABLE consultations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    patient_query VARCHAR(500) NOT NULL,
    diagnosis VARCHAR(2000),
    prescription VARCHAR(2000),
    status VARCHAR(50) NOT NULL CHECK (status IN ('SCHEDULED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    scheduled_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_minutes INTEGER,
    total_amount NUMERIC(10,2) NOT NULL,
    doctor_earnings NUMERIC(10,2),
    platform_commission NUMERIC(10,2),
    video_session_id VARCHAR(500),
    whatsapp_number VARCHAR(20),
    patient_rating INTEGER CHECK (patient_rating >= 1 AND patient_rating <= 5),
    patient_review VARCHAR(500),
    payment_status VARCHAR(50) CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED')),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consultations_patient_id ON consultations(patient_id);
CREATE INDEX idx_consultations_doctor_id ON consultations(doctor_id);
CREATE INDEX idx_consultations_status ON consultations(status);
CREATE INDEX idx_consultations_scheduled_at ON consultations(scheduled_at);
CREATE INDEX idx_consultations_created_at ON consultations(created_at DESC);
```

**Purpose:** Consultation lifecycle and medical records

**Key Fields:**
- `patientQuery`: Chief complaint/initial symptom description (encrypted)
- `diagnosis`: Doctor's diagnosis (encrypted)
- `prescription`: Doctor's prescription (encrypted)
- `status`: Workflow state
- `totalAmount`: Consultation fee
- `doctorEarnings`: Doctor's share (80%)
- `platformCommission`: Platform's share (20%)
- `videoSessionId`: Agora session identifier
- `whatsappNumber`: WhatsApp contact for follow-up

---

### doctor_availability
```sql
CREATE TABLE doctor_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    doctor_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week >= 0 AND day_of_week <= 6),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_available BOOLEAN DEFAULT true,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_doctor_availability_doctor_id ON doctor_availability(doctor_id);
CREATE INDEX idx_doctor_availability_day_of_week ON doctor_availability(day_of_week);
```

**Purpose:** Recurring availability slots for each doctor

---

## Payment Service Database

### payments
```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consultation_id UUID NOT NULL UNIQUE,
    patient_id UUID NOT NULL REFERENCES users(id),
    doctor_id UUID NOT NULL REFERENCES users(id),
    amount NUMERIC(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'USD',
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED')),
    payment_method VARCHAR(50) NOT NULL CHECK (payment_method IN ('CARD', 'UPI', 'BANK_TRANSFER', 'WALLET')),
    transaction_id VARCHAR(255) UNIQUE,
    error_message VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_payments_consultation_id ON payments(consultation_id);
CREATE INDEX idx_payments_patient_id ON payments(patient_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);
```

---

### invoices
```sql
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES payments(id),
    doctor_id UUID NOT NULL REFERENCES users(id),
    consultation_fee NUMERIC(10,2) NOT NULL,
    platform_commission NUMERIC(10,2) NOT NULL,
    doctor_payout NUMERIC(10,2) NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    invoice_date TIMESTAMP NOT NULL,
    due_date TIMESTAMP NOT NULL,
    paid_date TIMESTAMP,
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoices_doctor_id ON invoices(doctor_id);
CREATE INDEX idx_invoices_invoice_date ON invoices(invoice_date DESC);
```

---

## Notification Service Database

### notifications
```sql
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_entity_id UUID,
    channel VARCHAR(50) NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'WHATSAPP', 'PUSH', 'IN_APP')),
    status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'SENT', 'DELIVERED', 'FAILED', 'BOUNCED')),
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    error_message VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient_id ON notifications(recipient_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
```

---

## Data Relationships

```
User (1) ──┬──────→ (1) Doctor
           ├──────→ (1) UserCredential
           ├──────→ (M) Consultation (as patient)
           ├──────→ (M) Consultation (as doctor)
           ├──────→ (M) DoctorAvailability
           ├──────→ (M) Payment (as patient)
           ├──────→ (M) Invoice (as doctor)
           └──────→ (M) Notification

Consultation (1) ──→ (1) Payment
Payment (1) ──────→ (1) Invoice
```

## Retention Policies

| Table | Retention | Notes |
|-------|-----------|-------|
| users | Forever | Never delete user accounts, soft delete only |
| consultations | 730 days (2 years) | HIPAA requirement for medical records |
| payments | 2555 days (7 years) | Tax & financial audit requirement |
| notifications | 90 days | Rotate old logs to archive storage |
| doctor_availability | Active only | Soft delete when doctor goes inactive |

## Backup Strategy

- **Daily incremental backups** to AWS S3
- **Weekly full backups** with verification
- **Monthly archive** to Glacier for long-term retention
- **Point-in-time recovery** available for 30 days
- **RPO** (Recovery Point Objective): < 1 hour
- **RTO** (Recovery Time Objective): < 4 hours

## Query Performance

### Most Common Queries

1. **List available doctors**
   ```sql
   SELECT d.*, COUNT(c.id) as consultation_count
   FROM doctors d
   LEFT JOIN consultations c ON d.user_id = c.doctor_id AND c.status = 'COMPLETED'
   WHERE d.availability_status = 'AVAILABLE'
   AND d.license_status = 'VERIFIED'
   GROUP BY d.id
   ORDER BY d.rating DESC;
   ```

2. **Get patient consultation history**
   ```sql
   SELECT c.*, u.first_name, u.last_name
   FROM consultations c
   JOIN users u ON c.doctor_id = u.id
   WHERE c.patient_id = ?
   ORDER BY c.created_at DESC
   LIMIT 50;
   ```

3. **Calculate doctor earnings**
   ```sql
   SELECT 
     SUM(doctor_earnings) as total_earnings,
     COUNT(*) as total_consultations
   FROM consultations
   WHERE doctor_id = ?
   AND status = 'COMPLETED'
   AND created_at >= NOW() - INTERVAL '30 days';
   ```

## Encryption

### At-Rest Encryption
- Column-level encryption for: diagnosis, prescription, patient_query
- Uses: AES-256 with key rotation annually

### Transit Encryption
- All connections use TLS 1.3
- Application uses prepared statements (prevents SQL injection)
