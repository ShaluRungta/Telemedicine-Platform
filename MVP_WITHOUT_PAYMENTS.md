# MVP Without Payments - Implementation Guide

## Overview

This document explains how to build and launch the MVP **without payment integration**. Payments (Razorpay) will be added in Phase 2 after user validation.

---

## What's Included in MVP ✅

### Core Features
- **Authentication**: Register, login, JWT tokens
- **Doctor Search**: Search, filter, view doctor profiles
- **Consultations**: Schedule, video call, medical records
- **WhatsApp Integration**: Doctor-patient communication
- **Email Notifications**: Appointment reminders, confirmations
- **Ratings & Reviews**: 1-5 star rating system
- **Consultation History**: Patient & doctor history

### Video Calls
- Agora integration for 1-on-1 video consultations
- Screen sharing (optional)
- Session recording support

### Medical Records
- Diagnosis storage
- Prescription generation (text-based)
- Medical notes

---

## What's NOT Included (To Add Later) ❌

- **Payments**: No Stripe/Razorpay integration yet
- **Doctor Payouts**: Manual settlement later
- **Invoicing**: Will implement with Razorpay
- **Admin Dashboard**: Only basic user management

---

## MVP Architecture (No Payment Service)

```
┌─────────────────────────────────────────┐
│         API Gateway (Port 8080)         │
└────────────┬────────────────────────────┘
             │
    ┌────────┼────────┬─────────┐
    │        │        │         │
    ▼        ▼        ▼         ▼
┌────┐  ┌────┐  ┌──────┐  ┌─────────┐
│Auth│  │User│  │Consult.│  │Notif.   │
│:8001   │:8002   │:8002    │:8004    │
└────┘  └────┘  └──────┘  └─────────┘
    │        │        │         │
    └────────┼────────┴────┬────┘
             │            │
        ┌────▼────┐   ┌────▼──────┐
        │PostgreSQL   │WhatsApp API│
        │           │Gmail/SMTP  │
        └───────────┘ └────────────┘
```

**Note**: Payment service is removed from MVP flow

---

## Step-by-Step MVP Setup

### 1. Environment Configuration

```bash
# Copy and edit .env
cp .env.example .env

# Minimal .env for MVP (NO payments)
JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET" >> .env

# Video (Agora) - REQUIRED
AGORA_APP_ID=your-agora-id
AGORA_APP_CERTIFICATE=your-agora-cert

# Notifications - REQUIRED
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# WhatsApp - REQUIRED
WHATSAPP_BUSINESS_PHONE_NUMBER=your-whatsapp-number
WHATSAPP_API_TOKEN=your-whatsapp-token

# SMS - OPTIONAL (not required for MVP)
# TWILIO_ACCOUNT_SID=optional
# TWILIO_AUTH_TOKEN=optional

# SKIP THESE FOR MVP:
# STRIPE_API_KEY - leave empty
# STRIPE_SECRET_KEY - leave empty
# RAZORPAY_KEY_ID - leave empty
```

### 2. Start Services (Without Payment Service)

```bash
# Option A: Start all services (payment service will not be used)
docker-compose up -d

# Option B: Start specific services only
docker-compose up -d postgres auth-service user-service \
  consultation-service notification-service api-gateway
```

### 3. Verify Health

```bash
# Check all services
curl http://localhost:8080/actuator/health

# Check individual services
curl http://localhost:8001/actuator/health  # Auth
curl http://localhost:8002/actuator/health  # Consultation
curl http://localhost:8004/actuator/health  # Notification
```

---

## MVP User Flows

### User Registration & Login (Works ✅)

```bash
# 1. Register as patient
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "phone": "+919876543210",
    "firstName": "Raj",
    "lastName": "Kumar",
    "password": "SecurePass123!",
    "confirmPassword": "SecurePass123!",
    "role": "PATIENT"
  }'

# Response: { userId, email, role, accessToken, refreshToken }

# 2. Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "patient@example.com",
    "password": "SecurePass123!"
  }'
```

### Schedule Consultation (Works ✅)

```bash
# Get access token from login
export TOKEN="your-access-token"
export PATIENT_ID="patient-user-id"
export DOCTOR_ID="doctor-user-id"

# Schedule consultation
curl -X POST http://localhost:8080/api/v1/consultations \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: $PATIENT_ID" \
  -d '{
    "doctorId": "'$DOCTOR_ID'",
    "patientQuery": "Experiencing early ejaculation",
    "scheduledAt": "2026-05-20T14:00:00"
  }'

# Response: { consultationId, status: "SCHEDULED", ... }
```

### Start Video Call (Works ✅)

```bash
# Doctor starts consultation
curl -X POST http://localhost:8080/api/v1/consultations/$CONSULTATION_ID/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: $DOCTOR_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "videoSessionId": "agora-session-xyz123"
  }'

# Response: { status: "IN_PROGRESS", videoSessionId: "agora-..." }
```

### Complete Consultation (Works ✅)

```bash
# Doctor completes consultation
curl -X POST http://localhost:8080/api/v1/consultations/$CONSULTATION_ID/complete \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: $DOCTOR_ID" \
  -d 'diagnosis=Premature ejaculation due to anxiety&prescription=1. Paroxetine 20mg once daily'

# Consultation is now COMPLETED
# Status: COMPLETED, paymentStatus: COMPLETED (but no actual payment yet)
```

### Rate Consultation (Works ✅)

```bash
# Patient rates consultation
curl -X POST http://localhost:8080/api/v1/consultations/$CONSULTATION_ID/rate \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-User-Id: $PATIENT_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "review": "Excellent doctor, very professional"
  }'
```

---

## Handling Payments in MVP (Manual)

### Option 1: Outside the App (Simplest)
1. Patient and doctor agree on price
2. Patient pays doctor directly via:
   - Bank transfer
   - UPI (PhonePe, Google Pay, Paytm)
   - WhatsApp Pay
3. Doctor marks consultation as paid manually

### Option 2: In-App Tracking (Better)

```java
// In consultation entity, track manual payment:
consultation.setPaymentStatus(Consultation.PaymentStatus.COMPLETED);
consultation.setPaymentNotes("Manual payment received via WhatsApp");
consultation.save();
```

### Option 3: Payment Instructions via WhatsApp

When consultation is completed, send WhatsApp message:

```
"Hi! Your consultation is complete. 

Diagnosis: [diagnosis]
Amount: ₹500

Please transfer payment to: [bank details or UPI]

Prescription: [attached as PDF]"
```

---

## Removing Payment Service from Docker Compose

If you want to skip the payment service entirely:

```yaml
# docker-compose.yml - Keep this:
services:
  postgres:
    # ... stays
  rabbitmq:
    # ... stays
  redis:
    # ... stays
  auth-service:
    # ... stays
  user-service:
    # ... stays
  consultation-service:
    # ... stays
  notification-service:
    # ... stays
  api-gateway:
    # ... stays

  # ❌ DELETE OR COMMENT OUT:
  # payment-service:
  #   ...
```

Then start without payment service:
```bash
docker-compose up -d --ignore-orphans
```

---

## Database Schema for MVP (No Payments)

The following tables are NOT used in MVP:
- `payments` (empty)
- `invoices` (empty)

These tables stay in DB for later use but queries won't touch them.

---

## Consultation Status Flow in MVP

```
SCHEDULED
    ↓
IN_PROGRESS (video call starts)
    ↓
COMPLETED (diagnosis + prescription added)
    ↓
RATED (patient rates doctor)
    
---OR---

SCHEDULED
    ↓
CANCELLED (if patient cancels before start)

---OR---

SCHEDULED
    ↓
NO_SHOW (if neither shows up)
```

**Payment Status Always:**
- `PENDING` → When scheduled
- `COMPLETED` → When consultation finished (no actual payment, just marked)

---

## MVP Code Changes (Already Done ✅)

The codebase has been updated to:

✅ Remove payment fee calculation
✅ Skip payment status updates in cancellation
✅ Simplify consultation completion flow
✅ Remove Stripe/payment dependencies from consultation service

**No additional code changes needed!**

---

## Testing the MVP

### 1. Test Auth Flow
```bash
./scripts/test-auth.sh
```

### 2. Test Consultation Flow
```bash
./scripts/test-consultation.sh
```

### 3. Test Video Integration
```bash
# Manual testing in frontend app
# 1. Login as patient
# 2. Search doctors
# 3. Book consultation
# 4. Start video call (Agora)
# 5. End call
# 6. View consultation history
```

### 4. Test WhatsApp
```bash
# Manual testing with real WhatsApp number
# Verify message formatting and delivery
```

---

## Adding Payments Later (Phase 2)

When ready to add payments (after MVP validation):

### Step 1: Setup Razorpay
```bash
RAZORPAY_KEY_ID=rzp_live_...
RAZORPAY_KEY_SECRET=...
```

### Step 2: Implement Payment Service
- Create payment intent endpoint
- Implement payment verification
- Add webhook handler
- Calculate doctor payout

### Step 3: Update Consultation Service
```java
// After completing consultation:
PaymentResponse payment = paymentService.createPaymentIntent(
    consultationId, 
    totalAmount
);

consultation.setPaymentIntentId(payment.getIntentId());
```

### Step 4: Update Frontend
- Show payment form after consultation completion
- Process payment with Razorpay
- Show receipt

---

## MVP Deployment Checklist

- [ ] Agora credentials working
- [ ] WhatsApp Business API approved & working
- [ ] Email (Gmail) sending successfully
- [ ] Database migrations running
- [ ] Auth service generating valid JWTs
- [ ] Video calls connecting properly
- [ ] Consultation history showing
- [ ] Ratings & reviews storing
- [ ] WhatsApp messages delivering
- [ ] Admin can view all consultations
- [ ] No payment errors in logs

---

## Success Metrics for MVP

| Metric | Target |
|--------|--------|
| Doctor sign-ups | 20-30 in beta |
| Patient sign-ups | 100+ in beta |
| Consultation completion rate | > 80% |
| Video call success rate | > 95% |
| WhatsApp delivery | > 99% |
| User satisfaction (NPS) | > 30 |

---

## Next Steps After MVP Launch

1. **Week 1-2**: Collect user feedback
2. **Week 3-4**: Plan Razorpay integration
3. **Week 5-6**: Implement payments
4. **Week 7**: Test thoroughly
5. **Week 8**: Launch payment feature

---

## Summary

✅ **MVP is fully functional WITHOUT payments**
✅ **Users can schedule, video call, get prescriptions**
✅ **Payment happens outside the app initially**
✅ **Easy to add Razorpay later**
✅ **Focus on core product before monetization**

Launch the MVP, validate with real users, then add payments! 🚀

