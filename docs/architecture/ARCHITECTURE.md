# Telemedicine Platform - Architecture

## Overview
Production-ready Java telemedicine application using microservices architecture, designed for sexual health consultations between patients and doctors.

## System Architecture

```
┌─────────────────────────────────────────────────┐
│         API Gateway (Spring Cloud Gateway)      │
│         - Request routing, rate limiting        │
│         - JWT validation at gateway level       │
└────────────┬────────────────────────────────────┘
             │
    ┌────────┼────────┬──────────┬────────────┐
    │        │        │          │            │
┌───▼──┐ ┌───▼──┐ ┌───▼───┐ ┌───▼────┐ ┌────▼─────┐
│ Auth │ │User  │ │Consul-│ │Payment │ │Notif.    │
│Svc   │ │Svc   │ │tation │ │Svc     │ │Svc       │
│:8001 │ │:8002 │ │Svc    │ │:8003   │ │:8004     │
│      │ │      │ │:8002  │ │        │ │          │
└───┬──┘ └───┬──┘ └───┬───┘ └───┬────┘ └────┬─────┘
    │        │        │         │            │
    └────────┼────────┼─────────┼────────────┘
             │
        ┌────▼────────────────────────┐
        │    PostgreSQL Database      │
        │  (4 databases per service)  │
        └─────────────────────────────┘
             │
    ┌────────┼──────────┐
    │        │          │
┌───▼──┐ ┌───▼──┐ ┌────▼─────┐
│Redis │ │RabbitMQ│ │External  │
│Cache │ │Async Msg│ │APIs      │
└──────┘ └────────┘ └──────────┘
         (Agora, WhatsApp)
```

## Microservices

### 1. **Auth Service** (Port: 8001)
Handles user authentication, JWT token generation, and account verification.

**Responsibilities:**
- User registration and login
- Password hashing and validation
- JWT token generation and refresh
- Email verification
- Account status management

**Tech Stack:**
- Spring Security
- JJWT (JWT Library)
- PostgreSQL
- Flyway migrations

**API Endpoints:**
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh-token` - Refresh JWT
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/verify-email/{token}` - Email verification

### 2. **User Service** (Port: 8002)
Manages patient and doctor profiles, specializations, and availability.

**Responsibilities:**
- User profile management
- Doctor onboarding and verification
- Doctor specialization and qualifications
- User availability and scheduling
- Search and discovery of doctors

**Tech Stack:**
- Spring Data JPA
- OpenFeign for inter-service communication
- PostgreSQL

### 3. **Consultation Service** (Port: 8002)
Core business logic for scheduling, conducting, and managing consultations.

**Responsibilities:**
- Schedule consultations between patients and doctors
- Video session management (Agora integration)
- Consultation lifecycle (scheduled → in-progress → completed)
- Diagnosis and prescription storage
- Rating and review system
- WhatsApp follow-up coordination

**Tech Stack:**
- Spring Data JPA
- Spring WebFlux (for real-time updates)
- Agora SDK integration
- PostgreSQL

**API Endpoints:**
- `POST /api/v1/consultations` - Schedule consultation
- `GET /api/v1/consultations/{id}` - Get consultation details
- `POST /api/v1/consultations/{id}/start` - Start video session
- `POST /api/v1/consultations/{id}/complete` - Complete consultation
- `POST /api/v1/consultations/{id}/rate` - Rate consultation
- `DELETE /api/v1/consultations/{id}` - Cancel consultation

### 4. **Payment Service** (Port: 8003)
Handles payment processing, billing, and commission calculations.

**Responsibilities:**
- Payment gateway integration
- Invoice generation
- Commission split (doctor vs platform)
- Refund processing
- Payment history and reconciliation

**Tech Stack:**
- Spring Data JPA
- Payment gateway SDK (Stripe/Razorpay)
- PostgreSQL

### 5. **Notification Service** (Port: 8004)
Handles all communication channels: email, SMS, WhatsApp, and push notifications.

**Responsibilities:**
- Email notifications (appointment reminders, confirmations)
- SMS alerts
- WhatsApp Business API integration
- Push notifications
- Notification history and preferences

**Tech Stack:**
- Spring AMQP (RabbitMQ consumer)
- Spring Mail
- PostgreSQL
- RabbitMQ for async messaging

### 6. **API Gateway** (Port: 8080)
Single entry point for all client requests with cross-cutting concerns.

**Responsibilities:**
- Request routing to microservices
- JWT token validation
- Rate limiting
- CORS configuration
- Request/response logging
- Circuit breaker patterns

**Tech Stack:**
- Spring Cloud Gateway
- Spring Security
- Spring Cloud Eureka (service discovery)

## Database Schema

### Users Table (shared)
- id (UUID)
- email (unique)
- phone (unique)
- firstName, lastName
- passwordHash
- role (PATIENT, DOCTOR, ADMIN)
- status (PENDING_VERIFICATION, VERIFIED, SUSPENDED, DELETED)
- profilePictureUrl
- emailVerified, phoneVerified
- createdAt, updatedAt

### Doctors Table
- id (UUID)
- userId (FK to users)
- licenseNumber (unique)
- specialization
- yearsOfExperience
- consultationFeePerMinute
- licenseStatus
- availabilityStatus
- rating, totalConsultations
- bio, educationBackground

### Consultations Table
- id (UUID)
- patientId (FK)
- doctorId (FK)
- patientQuery (chief complaint)
- diagnosis, prescription
- status (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- scheduledAt, startedAt, completedAt
- durationMinutes
- totalAmount, doctorEarnings, platformCommission
- videoSessionId (Agora)
- whatsappNumber
- patientRating, patientReview
- paymentStatus

## Security Architecture

### JWT Tokens
- **Access Token**: 1 hour expiration, contains user ID, email, role
- **Refresh Token**: 24 hours expiration, used to obtain new access tokens
- **Token Blacklist**: Logout tokens added to Redis cache

### Password Security
- BCrypt hashing with strength 12
- Password validation: min 8 chars, must include numbers/symbols

### API Security
- Gateway validates JWT before routing requests
- Per-service authentication as defense-in-depth
- CORS configured for trusted origins only
- Rate limiting: 1000 req/min per IP, 100 req/min per user
- Request signing for payment endpoints

### Data Protection
- Encrypted communication (HTTPS/TLS 1.3)
- Sensitive fields encrypted at rest (diagnosis, prescriptions)
- Audit logging for sensitive operations
- GDPR compliance: right to deletion, data portability

## Deployment

### Docker Compose (Development)
```bash
docker-compose up -d
```

Services will auto-start in correct order with health checks.

### Kubernetes (Production)
See: `docs/deployment/kubernetes-manifests.yml`

## Performance Considerations

### Caching
- Redis for JWT blacklist
- Database query result caching for doctor listings
- Consultation history pagination (50 per page)

### Database
- Connection pooling: HikariCP with 20 connections
- Indexing on frequently queried fields (patient_id, doctor_id, status)
- Query timeouts: 30 seconds

### Load Testing
Expected throughput:
- Auth service: 5K logins/min
- Consultation service: 1K consultations/min
- Payment service: 500 payments/min

## Monitoring & Observability

### Metrics
- Prometheus endpoint: `/actuator/prometheus`
- Custom metrics: consultation completion rate, payment success rate
- Response times tracked per endpoint

### Logging
- Centralized logging (ELK or CloudWatch)
- Log levels: DEBUG (local), INFO (staging), WARN (production)
- Request correlation IDs propagated across services

### Health Checks
- `/actuator/health` - overall service health
- Liveness: service responds to requests
- Readiness: database connected, migrations complete

## Future Enhancements

1. **Real-time Features**: WebSocket for live consultation updates
2. **Analytics**: Doctor performance dashboards, revenue tracking
3. **AI/ML**: Symptom pre-screening, doctor matching algorithm
4. **Mobile Apps**: Native iOS/Android with offline capabilities
5. **Internationalization**: Multi-language support, timezone handling
6. **Telemedicine Standards**: HL7 FHIR compliance, medical record interoperability
