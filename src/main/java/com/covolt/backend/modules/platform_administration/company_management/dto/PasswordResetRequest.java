package com.covolt.backend.modules.platform_administration.company_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for password reset operation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {
    
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6-100 characters")
    private String newPassword;
    
    // Whether to send email notification to the user
    @Builder.Default
    private boolean sendNotification = true;
    
    // Reason for password reset (for audit purposes)
    private String resetReason;
    
    // Whether to force user to change password on next login
    @Builder.Default
    private boolean forcePasswordChange = true;
}
