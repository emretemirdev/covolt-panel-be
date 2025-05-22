package com.covolt.backend.features.email.dto;

import com.covolt.backend.features.email.enums.EmailPriority;
import com.covolt.backend.features.email.enums.EmailType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for sending emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @NotNull(message = "Email type is required")
    private EmailType emailType;
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Please provide a valid email address")
    private String to;
    
    // Optional additional recipients
    private List<String> cc;
    private List<String> bcc;
    
    // Subject (will use default from EmailType if not provided)
    private String subject;
    
    // Template variables for dynamic content
    @Builder.Default
    private Map<String, Object> templateVariables = Map.of();
    
    // Priority level
    @Builder.Default
    private EmailPriority priority = EmailPriority.NORMAL;
    
    // Scheduled sending time (optional)
    private LocalDateTime scheduledAt;
    
    // Retry configuration
    @Builder.Default
    private int maxRetries = 3;
    
    // Additional metadata
    private String userId; // User who triggered the email
    private String companyId; // Company context
    private String referenceId; // Reference to related entity (e.g., order ID, user ID)
    private String referenceType; // Type of reference (e.g., "USER", "ORDER", "SUBSCRIPTION")
    
    // Email tracking
    @Builder.Default
    private boolean trackOpens = true;
    
    @Builder.Default
    private boolean trackClicks = true;
    
    // Custom sender info (optional)
    private String fromName;
    private String fromEmail;
    
    // Attachments (file paths or URLs)
    private List<String> attachments;
    
    // Additional notes for logging/debugging
    private String notes;
}
