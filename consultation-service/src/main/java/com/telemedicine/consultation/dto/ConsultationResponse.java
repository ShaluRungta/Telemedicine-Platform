package com.telemedicine.consultation.dto;

import com.telemedicine.entity.Consultation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationResponse {

    private String id;
    private String patientId;
    private String doctorId;
    private String patientQuery;
    private String diagnosis;
    private String prescription;
    private Consultation.ConsultationStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private Integer durationMinutes;
    private BigDecimal totalAmount;
    private BigDecimal doctorEarnings;
    private BigDecimal platformCommission;
    private String videoSessionId;
    private String whatsappNumber;
    private Integer patientRating;
    private String patientReview;
    private Consultation.PaymentStatus paymentStatus;
}
