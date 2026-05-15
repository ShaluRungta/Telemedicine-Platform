package com.telemedicine.consultation.service;

import com.telemedicine.consultation.dto.ConsultationRequest;
import com.telemedicine.consultation.dto.ConsultationResponse;
import com.telemedicine.consultation.repository.ConsultationRepository;
import com.telemedicine.entity.Consultation;
import com.telemedicine.entity.User;
import com.telemedicine.exception.TeleMedicineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ConsultationService {

    private final ConsultationRepository consultationRepository;
    private static final BigDecimal PLATFORM_COMMISSION_PERCENTAGE = new BigDecimal("0.20");

    public ConsultationResponse scheduleConsultation(String patientId, ConsultationRequest request) {
        User patient = new User();
        patient.setId(patientId);

        User doctor = new User();
        doctor.setId(request.getDoctorId());

        var existingConsultation = consultationRepository.findByPatientId(patientId, null);
        if (existingConsultation != null && existingConsultation.hasContent()) {
            log.warn("Patient {} already has pending consultations", patientId);
        }

        Consultation consultation = Consultation.builder()
            .patient(patient)
            .doctor(doctor)
            .patientQuery(request.getPatientQuery())
            .scheduledAt(request.getScheduledAt())
            .status(Consultation.ConsultationStatus.SCHEDULED)
            .totalAmount(calculateConsultationFee(60))
            .paymentStatus(Consultation.PaymentStatus.PENDING)
            .build();

        consultation = consultationRepository.save(consultation);
        log.info("Consultation scheduled: {} between patient {} and doctor {}",
            consultation.getId(), patientId, request.getDoctorId());

        return mapToResponse(consultation);
    }

    public ConsultationResponse getConsultation(String consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("Consultation not found"));
        return mapToResponse(consultation);
    }

    public Page<ConsultationResponse> getPatientConsultations(String patientId, Pageable pageable) {
        return consultationRepository.findByPatientId(patientId, pageable)
            .map(this::mapToResponse);
    }

    public Page<ConsultationResponse> getDoctorConsultations(String doctorId, Pageable pageable) {
        return consultationRepository.findByDoctorId(doctorId, pageable)
            .map(this::mapToResponse);
    }

    public ConsultationResponse startConsultation(String consultationId, String videoSessionId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("Consultation not found"));

        if (consultation.getStatus() != Consultation.ConsultationStatus.SCHEDULED) {
            throw new TeleMedicineException.BadRequestException("Consultation cannot be started in current status");
        }

        consultation.setStatus(Consultation.ConsultationStatus.IN_PROGRESS);
        consultation.setStartedAt(LocalDateTime.now());
        consultation.setVideoSessionId(videoSessionId);

        consultation = consultationRepository.save(consultation);
        log.info("Consultation started: {}", consultationId);

        return mapToResponse(consultation);
    }

    public ConsultationResponse completeConsultation(String consultationId, String diagnosis, String prescription) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("Consultation not found"));

        if (consultation.getStatus() != Consultation.ConsultationStatus.IN_PROGRESS) {
            throw new TeleMedicineException.BadRequestException("Consultation is not in progress");
        }

        LocalDateTime completedAt = LocalDateTime.now();
        long durationMinutes = java.time.temporal.ChronoUnit.MINUTES.between(consultation.getStartedAt(), completedAt);

        consultation.setStatus(Consultation.ConsultationStatus.COMPLETED);
        consultation.setCompletedAt(completedAt);
        consultation.setDurationMinutes((int) durationMinutes);
        consultation.setDiagnosis(diagnosis);
        consultation.setPrescription(prescription);
        consultation.setPaymentStatus(Consultation.PaymentStatus.COMPLETED);

        consultation = consultationRepository.save(consultation);
        log.info("Consultation completed: {} - Duration: {} minutes", consultationId, durationMinutes);

        return mapToResponse(consultation);
    }

    public ConsultationResponse rateConsultation(String consultationId, Integer rating, String review) {
        if (rating < 1 || rating > 5) {
            throw new TeleMedicineException.BadRequestException("Rating must be between 1 and 5");
        }

        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("Consultation not found"));

        consultation.setPatientRating(rating);
        consultation.setPatientReview(review);

        consultation = consultationRepository.save(consultation);
        log.info("Consultation rated: {} - Rating: {}", consultationId, rating);

        return mapToResponse(consultation);
    }

    public void cancelConsultation(String consultationId) {
        Consultation consultation = consultationRepository.findById(consultationId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("Consultation not found"));

        if (consultation.getStatus() != Consultation.ConsultationStatus.SCHEDULED) {
            throw new TeleMedicineException.BadRequestException("Only scheduled consultations can be cancelled");
        }

        consultation.setStatus(Consultation.ConsultationStatus.CANCELLED);
        consultation.setPaymentStatus(Consultation.PaymentStatus.REFUNDED);

        consultationRepository.save(consultation);
        log.info("Consultation cancelled: {}", consultationId);
    }

    private BigDecimal calculateConsultationFee(int durationMinutes) {
        return new BigDecimal(durationMinutes).multiply(new BigDecimal("5.00"));
    }

    private ConsultationResponse mapToResponse(Consultation consultation) {
        return ConsultationResponse.builder()
            .id(consultation.getId())
            .patientId(consultation.getPatient().getId())
            .doctorId(consultation.getDoctor().getId())
            .patientQuery(consultation.getPatientQuery())
            .diagnosis(consultation.getDiagnosis())
            .prescription(consultation.getPrescription())
            .status(consultation.getStatus())
            .scheduledAt(consultation.getScheduledAt())
            .startedAt(consultation.getStartedAt())
            .completedAt(consultation.getCompletedAt())
            .durationMinutes(consultation.getDurationMinutes())
            .totalAmount(consultation.getTotalAmount())
            .doctorEarnings(consultation.getDoctorEarnings())
            .platformCommission(consultation.getPlatformCommission())
            .videoSessionId(consultation.getVideoSessionId())
            .whatsappNumber(consultation.getWhatsappNumber())
            .patientRating(consultation.getPatientRating())
            .patientReview(consultation.getPatientReview())
            .paymentStatus(consultation.getPaymentStatus())
            .build();
    }
}
