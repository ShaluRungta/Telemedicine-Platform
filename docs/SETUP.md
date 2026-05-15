# Telemedicine Platform - Setup Guide

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)
- Node.js 18+ (for frontend)

## Quick Start (Docker Compose)

### 1. Clone and Setup Environment

```bash
git clone <repository>
cd telemedicine-platform

# Copy environment template
cp .env.example .env

# Edit .env with your values
nano .env
```

### 2. Environment Variables

Create `.env` file:
```env
# JWT Configuration
JWT_SECRET=your-very-long-secret-key-at-least-256-bits-for-hs256

# Agora Video Configuration
AGORA_APP_ID=your-agora-app-id
AGORA_APP_CERTIFICATE=your-agora-certificate

# Payment Gateway (Stripe/Razorpay)
STRIPE_API_KEY=your-stripe-key
STRIPE_SECRET_KEY=your-stripe-secret

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# WhatsApp Business API
WHATSAPP_BUSINESS_API_URL=https://api.whatsapp.com
WHATSAPP_API_TOKEN=your-whatsapp-token
```

### 3. Start Services

```bash
# Build all services
mvn clean install

# Start with Docker Compose
docker-compose up -d

# Verify all services are running
docker-compose ps

# Check logs
docker-compose logs -f auth-service
```

### 4. Database Migrations

Migrations run automatically via Flyway on service startup.

Verify migrations:
```bash
docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "\dt"
```

### 5. Test API

```bash
# Register new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "phone": "+1234567890",
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
    "email": "patient@example.com",
    "password": "SecurePass123!"
  }'
```

## Local Development (Without Docker)

### 1. Install PostgreSQL

```bash
# macOS
brew install postgresql@16
brew services start postgresql@16

# Linux (Ubuntu/Debian)
sudo apt-get install postgresql-16
sudo systemctl start postgresql

# Windows: Download from https://www.postgresql.org/download/windows/
```

### 2. Create Databases

```bash
createdb telemedicine_auth
createdb telemedicine_consultation
createdb telemedicine_payment
createdb telemedicine_notification
```

### 3. Build and Run Individual Services

```bash
# Terminal 1: Auth Service
cd auth-service
mvn spring-boot:run

# Terminal 2: Consultation Service
cd consultation-service
mvn spring-boot:run

# Terminal 3: Payment Service
cd payment-service
mvn spring-boot:run

# Terminal 4: Notification Service
cd notification-service
mvn spring-boot:run

# Terminal 5: API Gateway
cd api-gateway
mvn spring-boot:run
```

## IDE Setup

### IntelliJ IDEA

1. Open project root
2. Configure JDK: File → Project Structure → JDK 21
3. Enable annotation processing: Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable
4. Maven: File → Settings → Build Tools → Maven → set JDK version
5. Run → Edit Configurations → Add Maven configs for each service

### Eclipse

1. Import as Existing Maven Projects
2. Maven → Update Project (Alt+F5)
3. Configure JDK 21 in Preferences

## Testing

### Unit Tests

```bash
mvn test
```

### Integration Tests

```bash
# Using Docker Compose
docker-compose up -d postgres
mvn verify
```

### API Testing with Postman

Import collection: `docs/postman-collection.json`

## Code Quality

### SonarQube Analysis

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=telemedicine-platform \
  -Dsonar.sources=. \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.login=your-sonar-token
```

### Code Coverage

```bash
mvn clean verify
# View report: target/site/jacoco/index.html
```

## Logging and Monitoring

### View Logs

```bash
# Docker
docker-compose logs -f --tail=100 auth-service

# Local
tail -f logs/application.log
```

### Prometheus Metrics

```bash
# Access metrics
http://localhost:8001/actuator/prometheus

# Common metrics:
- http_requests_total
- http_request_duration_seconds
- jvm_memory_used_bytes
```

### Health Checks

```bash
# Overall health
curl http://localhost:8080/actuator/health

# Individual services
curl http://localhost:8001/actuator/health  # Auth
curl http://localhost:8002/actuator/health  # Consultation
```

## Production Deployment

### Build Docker Images

```bash
# Build all services
docker build -t telemedicine/auth-service:1.0.0 ./auth-service
docker build -t telemedicine/consultation-service:1.0.0 ./consultation-service
docker build -t telemedicine/payment-service:1.0.0 ./payment-service
docker build -t telemedicine/notification-service:1.0.0 ./notification-service
docker build -t telemedicine/api-gateway:1.0.0 ./api-gateway

# Push to registry
docker push telemedicine/auth-service:1.0.0
```

### Kubernetes Deployment

See: `docs/deployment/kubernetes-manifests.yml`

```bash
kubectl apply -f docs/deployment/kubernetes-manifests.yml
```

## Troubleshooting

### Service won't start

```bash
# Check database connection
docker-compose logs postgres

# Check if port is in use
lsof -i :8001

# Clear Docker volumes and rebuild
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

### Migration failures

```bash
# Check migration status
docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "SELECT * FROM flyway_schema_history;"

# Repair schema (last resort)
docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "DELETE FROM flyway_schema_history WHERE success = false;"
```

### JWT token issues

Verify secret key is consistent across services:
```bash
docker-compose exec auth-service env | grep JWT_SECRET
```

## Next Steps

1. Review `/docs/architecture/ARCHITECTURE.md` for system design
2. Check `/docs/API.md` for endpoint documentation
3. Read `/docs/database/SCHEMA.md` for database structure
4. Review security checklist: `/docs/SECURITY.md`
