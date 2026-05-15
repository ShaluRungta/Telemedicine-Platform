# Telemedicine Platform

A production-ready Java application connecting patients with doctors for sexual health consultations via video calls and WhatsApp.

## 🎯 Mission

Remove taboo around sexual health issues and provide easy, private, and accessible medical guidance.

## 🏗️ Architecture

**Microservices** with **Spring Boot 3.2**, designed for scale and security:

- **Auth Service** - JWT-based authentication and authorization
- **Consultation Service** - Video scheduling, Agora integration, consultation lifecycle
- **User Service** - Patient and doctor profiles, onboarding
- **Payment Service** - Payment processing, billing, commission splits
- **Notification Service** - Email, SMS, WhatsApp, push notifications
- **API Gateway** - Request routing, rate limiting, cross-cutting concerns

**Database**: PostgreSQL (separate DB per service)  
**Message Queue**: RabbitMQ (async notifications)  
**Cache**: Redis (JWT blacklist, session data)  
**Deployment**: Docker Compose (dev), Kubernetes (production)

See full architecture: [`docs/architecture/ARCHITECTURE.md`](docs/architecture/ARCHITECTURE.md)

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Run with Docker Compose

```bash
# Set environment variables
cp .env.example .env
nano .env  # Edit with your API keys

# Start all services
docker-compose up -d

# Verify all services
docker-compose ps

# View logs
docker-compose logs -f auth-service
```

All services will start in order with health checks. Access the API at `http://localhost:8080`.

### Local Development (Without Docker)

```bash
# Install PostgreSQL 16
brew install postgresql@16
brew services start postgresql@16

# Create databases
createdb telemedicine_auth
createdb telemedicine_consultation
createdb telemedicine_payment
createdb telemedicine_notification

# Build all modules
mvn clean install

# Run each service (in separate terminals)
cd auth-service && mvn spring-boot:run
cd consultation-service && mvn spring-boot:run
cd payment-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

See detailed setup: [`docs/SETUP.md`](docs/SETUP.md)

## 📚 API Documentation

### Register User

```bash
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
```

### Login

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "password": "SecurePass123!"
  }'
```

Response:
```json
{
  "status": 200,
  "message": "Login successful",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "patient@example.com",
    "role": "PATIENT",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 3600
  }
}
```

### Schedule Consultation

```bash
curl -X POST http://localhost:8080/api/v1/consultations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "X-User-Id: $USER_ID" \
  -d '{
    "doctorId": "550e8400-e29b-41d4-a716-446655440001",
    "patientQuery": "Experiencing premature ejaculation for 2 months",
    "scheduledAt": "2026-05-20T14:00:00"
  }'
```

See full API docs: `docs/api/ENDPOINTS.md` (to be created)

## 🔒 Security

### Key Features
- ✅ JWT token-based authentication
- ✅ Encrypted data at rest (AES-256)
- ✅ TLS 1.3 for all communication
- ✅ HIPAA compliance ready
- ✅ SQL injection prevention (parameterized queries)
- ✅ XSS protection via output encoding
- ✅ Rate limiting per IP and user
- ✅ Audit logging for all sensitive operations
- ✅ Password hashing with BCrypt

### Security Checklist
See: [`docs/SECURITY.md`](docs/SECURITY.md)

## 📊 Monitoring

### Health Checks

```bash
curl http://localhost:8080/actuator/health
```

### Metrics (Prometheus)

```bash
curl http://localhost:8001/actuator/prometheus
```

### Logs

```bash
# Docker
docker-compose logs -f auth-service

# Local
tail -f logs/application.log
```

## 🧪 Testing

### Run Tests

```bash
# Unit tests
mvn test

# Integration tests (requires Docker)
mvn verify

# Code coverage
mvn jacoco:report
# View: target/site/jacoco/index.html
```

### Postman Collection

Import: `docs/postman-collection.json` (to be created)

## 📦 Project Structure

```
telemedicine-platform/
├── auth-service/              # Authentication & JWT
│   ├── src/
│   │   ├── controller/        # REST endpoints
│   │   ├── service/           # Business logic
│   │   ├── repository/        # Database access
│   │   ├── entity/            # JPA entities
│   │   └── security/          # JWT & security configs
│   └── resources/
│       ├── application.yml    # Configuration
│       └── db/migration/      # Flyway migrations
├── consultation-service/      # Consultation management & video
├── user-service/              # Patient & doctor profiles
├── payment-service/           # Payment processing
├── notification-service/      # Emails, SMS, WhatsApp
├── api-gateway/               # Request routing & auth
├── common-lib/                # Shared DTOs & entities
├── docs/                      # Documentation
│   ├── architecture/          # System design
│   ├── database/              # Schema diagrams
│   ├── api/                   # API documentation
│   └── deployment/            # K8s manifests
├── scripts/                   # Utility scripts
└── docker-compose.yml         # Local dev environment
```

## 🛠️ Technology Stack

| Component | Technology |
|-----------|-----------|
| **Runtime** | Java 21 |
| **Framework** | Spring Boot 3.2 |
| **Database** | PostgreSQL 16 |
| **Message Queue** | RabbitMQ |
| **Cache** | Redis |
| **Authentication** | JWT (JJWT) |
| **API Gateway** | Spring Cloud Gateway |
| **Migrations** | Flyway |
| **ORM** | Hibernate (JPA) |
| **Logging** | SLF4J + Logback |
| **Monitoring** | Spring Actuator + Prometheus |
| **Container** | Docker |
| **Orchestration** | Docker Compose / Kubernetes |

## 📈 Performance Targets

- **Latency**: p99 < 200ms for consultations
- **Throughput**: 5K logins/min, 1K consultations/min
- **Availability**: 99.9% uptime
- **Database**: < 50ms query response time
- **Cache Hit Rate**: > 80% for doctor searches

## 🚢 Deployment

### Docker Build

```bash
mvn clean install
docker build -t telemedicine/auth-service:1.0.0 ./auth-service
docker-compose push
```

### Kubernetes

```bash
kubectl apply -f docs/deployment/kubernetes-manifests.yml
kubectl get pods -n telemedicine
kubectl logs -f deployment/auth-service -n telemedicine
```

### CI/CD Pipeline

- GitHub Actions workflows: `.github/workflows/`
- Build on every push
- Run tests and security scans
- Deploy to staging on PR
- Production deploy on merge to main

## 📝 Development Workflow

1. **Fork & Clone** the repository
2. **Create feature branch**: `git checkout -b feature/your-feature`
3. **Make changes** with tests
4. **Run checks**: `mvn clean verify`
5. **Commit**: `git commit -m "feat: description"`
6. **Push & Open PR**: Automated CI will run
7. **Merge after review**

### Code Style

- Follow Google Java Style Guide
- Use descriptive variable names
- Keep methods small and focused (< 30 lines)
- Add comments for complex logic only
- 100% test coverage for business logic

## 🐛 Troubleshooting

### Services won't start

```bash
# Check port conflicts
lsof -i :8001

# Clear containers and restart
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

### Database connection error

```bash
# Verify PostgreSQL is running
docker-compose logs postgres

# Check migration status
docker-compose exec postgres psql -U postgres -d telemedicine_auth -c "SELECT * FROM flyway_schema_history;"
```

### JWT token issues

```bash
# Verify secret is consistent
docker-compose exec auth-service env | grep JWT_SECRET
```

## 📞 Support

- **Documentation**: `/docs` folder
- **Issues**: GitHub Issues
- **Security**: Contact security@telemedicine.com

## 📄 License

Proprietary - All Rights Reserved

## 🙏 Contributing

See: [`CONTRIBUTING.md`](CONTRIBUTING.md) (to be created)

---

**Status**: Production Ready ✓  
**Last Updated**: May 2026  
**Maintainers**: Development Team
