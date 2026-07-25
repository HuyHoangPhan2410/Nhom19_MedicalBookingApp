package com.medbooking.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean emailVerified = true;

    @JsonIgnore
    @Column(name = "email_verification_otp_hash", length = 100)
    private String emailVerificationOtpHash;

    @JsonIgnore
    @Column(name = "email_verification_otp_expires_at")
    private LocalDateTime emailVerificationOtpExpiresAt;

    @JsonIgnore
    @Column(name = "otp_resend_count", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer otpResendCount = 0;

    @JsonIgnore
    @Column(name = "otp_resend_window_start")
    private LocalDateTime otpResendWindowStart;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        patient, doctor, admin
    }

    public User() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationOtpHash() {
        return emailVerificationOtpHash;
    }

    public void setEmailVerificationOtpHash(String emailVerificationOtpHash) {
        this.emailVerificationOtpHash = emailVerificationOtpHash;
    }

    public LocalDateTime getEmailVerificationOtpExpiresAt() {
        return emailVerificationOtpExpiresAt;
    }

    public void setEmailVerificationOtpExpiresAt(LocalDateTime emailVerificationOtpExpiresAt) {
        this.emailVerificationOtpExpiresAt = emailVerificationOtpExpiresAt;
    }

    public Integer getOtpResendCount() {
        return otpResendCount;
    }

    public void setOtpResendCount(Integer otpResendCount) {
        this.otpResendCount = otpResendCount;
    }

    public LocalDateTime getOtpResendWindowStart() {
        return otpResendWindowStart;
    }

    public void setOtpResendWindowStart(LocalDateTime otpResendWindowStart) {
        this.otpResendWindowStart = otpResendWindowStart;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
