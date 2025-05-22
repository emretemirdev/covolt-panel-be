package com.covolt.backend.modules.platform_administration.company_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for password reset operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetResponse {
    
    private UUID userId;
    private String email;
    private String username;
    private String fullName;
    private String operation; // "PASSWORD_RESET"
    private String status; // "SUCCESS", "FAILED"
    private String message;
    private LocalDateTime operationTime;
    
    // Company information
    private UUID companyId;
    private String companyName;
    
    // Reset details
    private boolean emailSent;
    private boolean forcePasswordChange;
    private String resetToken; // Only for testing/debugging (should not be exposed in production)
    
    // Additional details
    private Object details; // Can contain operation-specific information
}
