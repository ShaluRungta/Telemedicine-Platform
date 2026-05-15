package com.telemedicine.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private String licenseNumber;

    private String licenseCountry;

    @Column(nullable = false)
    private String specialization;

    private String bio;

    private Integer yearsOfExperience;

    @Column(nullable = false)
    private BigDecimal consultationFeePerMinute;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LicenseStatus licenseStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AvailabilityStatus availabilityStatus;

    private LocalDateTime licenseVerificationDate;

    private Double rating = 0.0;

    private Long totalConsultations = 0L;

    private Long totalRatings = 0L;

    private String educationBackground;

    public enum LicenseStatus {
        PENDING_VERIFICATION, VERIFIED, REJECTED, EXPIRED
    }

    public enum AvailabilityStatus {
        AVAILABLE, BUSY, OFFLINE
    }
}
