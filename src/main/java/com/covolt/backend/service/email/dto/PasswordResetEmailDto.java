package com.covolt.backend.service.email.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for password reset email data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetEmailDto {
    
    private String email;
    private String fullName;
    private String resetToken;
    private String resetUrl;
    private LocalDateTime requestTime;
    private int expirationMinutes;
    private String companyName;
    private String requestedBy; // Admin who requested the reset
}
