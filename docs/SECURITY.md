# Security Guidelines

## Overview
This document outlines security best practices and requirements for the telemedicine platform handling sensitive health information.

## HIPAA / Medical Data Compliance

### Covered Entities
The platform is a Business Associate for healthcare providers handling Protected Health Information (PHI).

### Compliance Requirements
- Ensure all consultations are encrypted in transit (TLS 1.3)
- Encrypt diagnosis and prescription data at rest (AES-256)
- Maintain audit logs for all PHI access (indefinite retention)
- Implement access controls (minimum necessary principle)
- Data breach notification within 60 days
- Business Associate Agreements with integrations

### Implementation
```java
@Entity
public class Consultation {
    @Encrypted // Using TDE or column-level encryption
    private String diagnosis;
    
    @Encrypted
    private String prescription;
}
```

## Authentication & Authorization

### JWT Implementation
- **Algorithm**: HS256 (HMAC-SHA256)
- **Secret**: Min 256-bit entropy
- **Access Token TTL**: 1 hour
- **Refresh Token TTL**: 24 hours
- **Signature Verification**: Always validate on every request

### Password Policy
- Minimum 8 characters
- Must include: uppercase, lowercase, digit, special character
- No dictionary words or user information
- BCrypt with strength 12
- Never store plain text

### Role-Based Access Control (RBAC)
```java
@PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN')")
public ConsultationResponse startConsultation(...) {}

@PreAuthorize("hasRole('PATIENT')")
public ConsultationResponse scheduleConsultation(...) {}
```

## API Security

### Rate Limiting
- Per IP: 1,000 requests/minute
- Per User: 100 requests/minute
- Per Endpoint: Service-specific limits

### Input Validation
- Validate all inputs with @Validated annotations
- Whitelist allowed characters
- SQL injection prevention via parameterized queries (JPA)
- XSS prevention via output encoding

### CORS Configuration
```yaml
cors:
  allowed-origins:
    - https://app.telemedicine.com
    - https://doctor.telemedicine.com
  allowed-methods: GET,POST,PUT,DELETE
  allowed-headers: Content-Type,Authorization
  max-age: 3600
```

### API Key Management
- Never commit keys to Git
- Use environment variables or secrets manager
- Rotate keys quarterly
- Audit all key access

## Data Protection

### Encryption at Rest
- Database: Transparent Data Encryption (TDE)
- Backup files: AES-256
- Key rotation: Annual or after breach

### Encryption in Transit
- TLS 1.3 mandatory for all communication
- Certificate pinning for mobile apps
- Perfect forward secrecy enabled

### Data Minimization
- Collect only necessary information
- PII: Last 4 digits phone, masked email in logs
- Delete old consultation data after retention period (2 years default)

### PII Handling
```java
// Correct: Mask sensitive data
String maskedPhone = phone.substring(phone.length() - 4);
String maskedEmail = email.split("@")[0].replaceAll(".", "*") + "@...";

// Incorrect: Never log full PII
log.info("User logged in: {}", user.getEmail()); // BAD
log.info("User login: ID={}", user.getId()); // GOOD
```

## Database Security

### Access Control
- Database users have minimum required privileges
- No direct database access from applications (use connection pooling)
- Separate read-only replica for reporting

### Connection Security
```yaml
datasource:
  url: jdbc:postgresql://localhost/telemedicine?sslmode=require
  hikari:
    maximum-pool-size: 20
    minimum-idle: 5
    connection-timeout: 30000
```

### Query Parameterization
```java
// Correct: Using JPA prevents SQL injection
List<User> users = repository.findByEmail(email);

// Incorrect: String concatenation (NEVER DO THIS)
String query = "SELECT * FROM users WHERE email = '" + email + "'";
```

## Infrastructure Security

### Firewall Rules
- API Gateway on public subnet
- Services on private subnet
- Databases restricted to application servers only

### Secrets Management
- Use HashiCorp Vault or AWS Secrets Manager
- Never commit secrets to Git
- Rotate secrets quarterly
- Audit secret access

### Logging Security
- Never log passwords, tokens, or PII
- Sanitize query parameters in logs
- Central log aggregation with encryption
- Log retention: 90 days hot, 2 years archive

## Third-Party Security

### Agora Video Integration
- HTTPS only
- Token signing with certificate
- IP whitelist for Agora servers
- Regular security audits

### Payment Gateway
- PCI DSS Level 1 compliance
- Tokenization to avoid storing card numbers
- Webhook signature verification
- TLS 1.2+ for all payments

### WhatsApp Business API
- API rate limiting
- Message encryption
- Webhook IP whitelist
- Regular compliance audits

## Security Testing

### OWASP Top 10 Prevention
1. **Injection**: Use parameterized queries ✓
2. **Broken Auth**: JWT + refresh tokens ✓
3. **Sensitive Data**: Encryption at rest and transit ✓
4. **XML External Entities**: Disable XML parsing
5. **Broken Access Control**: RBAC + endpoint authorization ✓
6. **Security Misconfiguration**: Hardened configs ✓
7. **Cross-Site Scripting**: Output encoding ✓
8. **Insecure Deserialization**: Never deserialize untrusted data
9. **Using Components with Known Vulns**: Regular dependency updates ✓
10. **Insufficient Logging**: Comprehensive audit logging ✓

### Security Scanning
```bash
# OWASP Dependency Check
mvn dependency-check:check

# SonarQube Security Analysis
mvn sonar:sonar

# Trivy container scanning
trivy image telemedicine/auth-service:latest

# ZAP API scanning
docker run -v $(pwd):/zap/wrk:rw \
  -t owasp/zap2docker-stable zap-api-scan.py \
  -t http://localhost:8080/swagger-ui.html
```

## Incident Response

### Breach Response Plan
1. Identify scope: affected users, systems, data
2. Contain: isolate systems, revoke credentials
3. Eradicate: patch vulnerabilities
4. Recover: restore from clean backups
5. Notify: users and regulators within 60 days
6. Post-mortem: document lessons learned

### Security Incident Contacts
- Security Lead: [email]
- DevOps: [email]
- Legal/Compliance: [email]
- External: [incident-response-company]

## Regular Security Tasks

### Daily
- Monitor security logs and alerts
- Check for failed authentication attempts
- Monitor database access patterns

### Weekly
- Review new CVEs from NVD
- Scan images for vulnerabilities
- Test backup restoration

### Monthly
- Security team review of code changes
- Dependency updates
- Access control audits

### Quarterly
- Penetration testing
- Security training for team
- Key/secret rotation
- Compliance audits

### Annually
- Third-party security assessment
- HIPAA audit
- Disaster recovery drill

## Compliance Checklist

- [ ] All data encrypted in transit (TLS 1.3)
- [ ] At-rest encryption enabled (AES-256)
- [ ] No hardcoded secrets
- [ ] Input validation on all endpoints
- [ ] Rate limiting configured
- [ ] CORS restricted to trusted origins
- [ ] Audit logging enabled
- [ ] Database access controlled
- [ ] API keys rotated
- [ ] Dependency vulnerabilities addressed
- [ ] Security headers configured
- [ ] Error messages don't leak sensitive info
- [ ] HIPAA Business Associate Agreement signed
- [ ] Incident response plan documented
- [ ] Staff security training completed
