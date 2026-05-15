package com.telemedicine.auth.entity;

import com.telemedicine.entity.BaseEntity;
import com.telemedicine.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_credentials")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCredential extends BaseEntity {

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    private String passwordHash;

    private Integer failedLoginAttempts = 0;

    private Boolean accountLocked = false;

    private String lastLoginIp;

    private String lastLoginUserAgent;
}
