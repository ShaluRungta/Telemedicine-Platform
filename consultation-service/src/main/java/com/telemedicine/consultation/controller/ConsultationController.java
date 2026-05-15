package com.telemedicine.consultation.controller;

import com.telemedicine.consultation.dto.ConsultationRequest;
import com.telemedicine.consultation.dto.ConsultationResponse;
import com.telemedicine.consultation.dto.RatingRequest;
import com.telemedicine.consultation.service.ConsultationService;
import com.telemedicine.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/consultations")
@RequiredArgsConstructor
public class ConsultationController {

    private final ConsultationService consultationService;

    @PostMapping
    public ResponseEntity<ApiResponse<ConsultationResponse>> scheduleConsultation(
        @RequestHeader("X-User-Id") String patientId,
        @Valid @RequestBody ConsultationRequest request) {
        ConsultationResponse response = consultationService.scheduleConsultation(patientId, request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(response, "Consultation scheduled successfully"));
    }

    @GetMapping("/{consultationId}")
    public ResponseEntity<ApiResponse<ConsultationResponse>> getConsultation(@PathVariable String consultationId) {
        ConsultationResponse response = consultationService.getConsultation(consultationId);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation retrieved"));
    }

    @GetMapping("/patient/history")
    public ResponseEntity<ApiResponse<Page<ConsultationResponse>>> getPatientConsultations(
        @RequestHeader("X-User-Id") String patientId,
        Pageable pageable) {
        Page<ConsultationResponse> response = consultationService.getPatientConsultations(patientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Patient consultations retrieved"));
    }

    @GetMapping("/doctor/history")
    public ResponseEntity<ApiResponse<Page<ConsultationResponse>>> getDoctorConsultations(
        @RequestHeader("X-User-Id") String doctorId,
        Pageable pageable) {
        Page<ConsultationResponse> response = consultationService.getDoctorConsultations(doctorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response, "Doctor consultations retrieved"));
    }

    @PostMapping("/{consultationId}/start")
    public ResponseEntity<ApiResponse<ConsultationResponse>> startConsultation(
        @PathVariable String consultationId,
        @RequestParam String videoSessionId) {
        ConsultationResponse response = consultationService.startConsultation(consultationId, videoSessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation started"));
    }

    @PostMapping("/{consultationId}/complete")
    public ResponseEntity<ApiResponse<ConsultationResponse>> completeConsultation(
        @PathVariable String consultationId,
        @RequestParam String diagnosis,
        @RequestParam String prescription) {
        ConsultationResponse response = consultationService.completeConsultation(consultationId, diagnosis, prescription);
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation completed"));
    }

    @PostMapping("/{consultationId}/rate")
    public ResponseEntity<ApiResponse<ConsultationResponse>> rateConsultation(
        @PathVariable String consultationId,
        @Valid @RequestBody RatingRequest request) {
        ConsultationResponse response = consultationService.rateConsultation(
            consultationId, request.getRating(), request.getReview());
        return ResponseEntity.ok(ApiResponse.success(response, "Consultation rated"));
    }

    @DeleteMapping("/{consultationId}")
    public ResponseEntity<ApiResponse<Void>> cancelConsultation(@PathVariable String consultationId) {
        consultationService.cancelConsultation(consultationId);
        return ResponseEntity.ok(ApiResponse.success(null, "Consultation cancelled"));
    }
}
