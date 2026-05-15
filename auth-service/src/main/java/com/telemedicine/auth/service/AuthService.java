package com.telemedicine.auth.service;

import com.telemedicine.auth.dto.LoginRequest;
import com.telemedicine.auth.dto.LoginResponse;
import com.telemedicine.auth.dto.RegisterRequest;
import com.telemedicine.auth.repository.UserRepository;
import com.telemedicine.auth.security.JwtTokenProvider;
import com.telemedicine.entity.User;
import com.telemedicine.exception.TeleMedicineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, Long> tokenBlacklist = new ConcurrentHashMap<>();

    public LoginResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new TeleMedicineException.BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new TeleMedicineException.ConflictException("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new TeleMedicineException.ConflictException("Phone already registered");
        }

        User.UserRole role = User.UserRole.valueOf(request.getRole().toUpperCase());

        User user = User.builder()
            .email(request.getEmail())
            .phone(request.getPhone())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(role)
            .status(User.AccountStatus.PENDING_VERIFICATION)
            .emailVerified(false)
            .phoneVerified(false)
            .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return generateLoginResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new TeleMedicineException.UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new TeleMedicineException.UnauthorizedException("Invalid email or password");
        }

        if (user.getStatus() == User.AccountStatus.SUSPENDED) {
            throw new TeleMedicineException.ForbiddenException("Account is suspended");
        }

        if (user.getStatus() == User.AccountStatus.DELETED) {
            throw new TeleMedicineException.ForbiddenException("Account has been deleted");
        }

        log.info("User logged in: {}", user.getEmail());
        return generateLoginResponse(user);
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new TeleMedicineException.UnauthorizedException("Invalid refresh token");
        }

        if (isTokenBlacklisted(refreshToken)) {
            throw new TeleMedicineException.UnauthorizedException("Token has been revoked");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("User not found"));

        return generateLoginResponse(user);
    }

    public void logout(String accessToken) {
        if (jwtTokenProvider.validateToken(accessToken)) {
            long expiryTime = jwtTokenProvider.isTokenExpired(accessToken) ? 0 : System.currentTimeMillis() + 3600000;
            tokenBlacklist.put(accessToken, expiryTime);
            log.info("User logged out");
        }
    }

    public void verifyEmail(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            throw new TeleMedicineException.BadRequestException("Invalid or expired verification token");
        }

        String userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new TeleMedicineException.NotFoundException("User not found"));

        user.setEmailVerified(true);
        user.setStatus(User.AccountStatus.VERIFIED);
        userRepository.save(user);
        log.info("Email verified for user: {}", user.getEmail());
    }

    private LoginResponse generateLoginResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole().toString());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return LoginResponse.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .role(user.getRole().toString())
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(3600L)
            .build();
    }

    private boolean isTokenBlacklisted(String token) {
        Long expiryTime = tokenBlacklist.get(token);
        if (expiryTime == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiryTime) {
            tokenBlacklist.remove(token);
            return false;
        }
        return true;
    }
}
