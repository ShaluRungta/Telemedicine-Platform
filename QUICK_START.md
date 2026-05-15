# Quick Start Guide

## 30-Minute Setup

### 1. Clone & Configure (2 min)
```bash
cd telemedicine-platform

# Copy and edit environment file
cp .env.example .env

# IMPORTANT: Update these in .env:
# - JWT_SECRET (change to random 256+ bit string)
# - AGORA_APP_ID & AGORA_APP_CERTIFICATE
# - Stripe/Razorpay keys
# - Email configuration
```

### 2. Start Services (5 min)
```bash
# Build all modules
mvn clean install -DskipTests

# Start with Docker Compose
docker-compose up -d

# Wait for health checks
docker-compose ps
```

### 3. Verify Services (3 min)
```bash
# Check API Gateway is responding
curl http://localhost:8080/actuator/health

# View logs
docker-compose logs -f auth-service
```

### 4. Test Registration (5 min)
```bash
# Register a patient
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@test.com",
    "phone": "+12025551234",
    "firstName": "John",
    "lastName": "Doe",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "role": "PATIENT"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@test.com",
    "password": "SecurePass123!"
  }'
```

### 5. Create Your First Consultation (15 min)

```bash
# 1. Register a doctor
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "doctor@test.com",
    "phone": "+12025559876",
    "firstName": "Dr",
    "lastName": "Smith",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "role": "DOCTOR"
  }'

# 2. Get the JWT token from login response
# Replace with your actual token:
export ACCESS_TOKEN="eyJhbGciOiJIUzI1NiJ9..."
export PATIENT_ID="550e8400-e29b-41d4-a716-446655440000"
export DOCTOR_ID="550e8400-e29b-41d4-a716-446655440001"

# 3. Schedule a consultation
curl -X POST http://localhost:8080/api/v1/consultations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $PATIENT_ID" \
  -d '{
    "doctorId": "'$DOCTOR_ID'",
    "patientQuery": "Experiencing issues with erectile dysfunction",
    "scheduledAt": "2026-05-20T14:00:00"
  }'

# 4. Start consultation (as doctor)
curl -X POST http://localhost:8080/api/v1/consultations/{CONSULTATION_ID}/start \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $DOCTOR_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "videoSessionId": "agora-session-12345"
  }'

# 5. Complete consultation
curl -X POST http://localhost:8080/api/v1/consultations/{CONSULTATION_ID}/complete \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $DOCTOR_ID" \
  -d '{
    "diagnosis": "Erectile dysfunction related to stress and anxiety",
    "prescription": "1. Sildenafil 50mg once daily\n2. Lifestyle changes recommended"
  }'

# 6. Rate consultation (as patient)
curl -X POST http://localhost:8080/api/v1/consultations/{CONSULTATION_ID}/rate \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $PATIENT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "review": "Excellent consultation, very professional and helpful"
  }'
```

## Key Endpoints

### Auth Service
- `POST /api/v1/auth/register` - Sign up
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh-token` - Refresh JWT
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/verify-email/{token}` - Verify email

### Consultation Service
- `POST /api/v1/consultations` - Schedule consultation
- `GET /api/v1/consultations/{id}` - Get consultation details
- `GET /api/v1/consultations/patient/history` - Patient's consultation history
- `GET /api/v1/consultations/doctor/history` - Doctor's consultation history
- `POST /api/v1/consultations/{id}/start` - Start video call
- `POST /api/v1/consultations/{id}/complete` - Complete consultation with diagnosis
- `POST /api/v1/consultations/{id}/rate` - Rate consultation
- `DELETE /api/v1/consultations/{id}` - Cancel consultation

## Architecture Overview

```
Frontend Apps (iOS, Android, Web)
         ↓
    API Gateway (Port 8080)
         ↓
  ┌──────┼──────┬─────────┬──────────┐
  ↓      ↓      ↓         ↓          ↓
Auth   Consult Payment   Notify    User
:8001  :8002   :8003     :8004     :8002
  ↓      ↓      ↓         ↓          ↓
PostgreSQL (4 databases + RabbitMQ + Redis)
```

## Common Tasks

### View Logs
```bash
docker-compose logs -f auth-service
docker-compose logs -f consultation-service
docker-compose logs -f --tail=100 api-gateway
```

### Restart Services
```bash
docker-compose restart auth-service
docker-compose restart consultation-service
```

### Stop Everything
```bash
docker-compose down
```

### Cleanup and Start Fresh
```bash
docker-compose down -v  # Remove volumes
docker-compose build --no-cache
docker-compose up -d
```

### View Database
```bash
docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "SELECT * FROM users;"
```

### Check Health
```bash
# Overall
curl http://localhost:8080/actuator/health

# By service
curl http://localhost:8001/actuator/health  # Auth
curl http://localhost:8002/actuator/health  # Consultation
curl http://localhost:8003/actuator/health  # Payment
curl http://localhost:8004/actuator/health  # Notification
```

## Next Steps

1. **Read Architecture**: [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md)
2. **Security Checklist**: [`docs/SECURITY.md`](docs/SECURITY.md)
3. **Database Schema**: [`docs/database/SCHEMA.md`](docs/database/SCHEMA.md)
4. **Full Setup Guide**: [`docs/SETUP.md`](docs/SETUP.md)
5. **API Documentation**: See Postman collection (to be added)

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Port 8080 already in use | `lsof -i :8080` and kill the process |
| Database connection error | Check `docker-compose logs postgres` |
| JWT token invalid | Ensure JWT_SECRET is the same across services |
| Services won't start | Run `docker-compose down -v && docker-compose build --no-cache && docker-compose up` |
| Docker image build fails | Clear cache: `docker system prune -a` |

## Performance Tips

- Use Redis cache: `redis-cli FLUSHALL` to clear cache
- Check DB query performance: `docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "EXPLAIN ANALYZE SELECT..."`
- Monitor with Prometheus: http://localhost:9090

## Security Reminders

⚠️ **Before Production:**
1. Change all default passwords
2. Update JWT_SECRET to a strong random value
3. Enable HTTPS/TLS
4. Configure CORS properly
5. Set up database backups
6. Enable encryption at rest
7. Review and update HIPAA compliance
8. Run security scans: `mvn dependency-check:check`

## Getting Help

- Check logs: `docker-compose logs -f service-name`
- Read docs in `/docs` folder
- Review code in relevant service folder
- Check database with: `docker-compose exec postgres psql`

---

**Time to first consultation:** ~30 minutes ✓
