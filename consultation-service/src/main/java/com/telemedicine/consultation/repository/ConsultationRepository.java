package com.telemedicine.consultation.repository;

import com.telemedicine.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, String> {

    Page<Consultation> findByPatientId(String patientId, Pageable pageable);

    Page<Consultation> findByDoctorId(String doctorId, Pageable pageable);

    List<Consultation> findByStatusAndScheduledAtBefore(
        Consultation.ConsultationStatus status,
        LocalDateTime dateTime
    );

    @Query("SELECT c FROM Consultation c WHERE c.doctor.id = :doctorId AND " +
        "c.status = :status AND c.scheduledAt BETWEEN :startDate AND :endDate")
    List<Consultation> findDoctorConsultationsByStatusAndDateRange(
        @Param("doctorId") String doctorId,
        @Param("status") Consultation.ConsultationStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    long countByDoctorIdAndStatus(String doctorId, Consultation.ConsultationStatus status);
}
