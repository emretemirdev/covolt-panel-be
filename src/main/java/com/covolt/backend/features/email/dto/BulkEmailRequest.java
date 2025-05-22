package com.covolt.backend.features.email.dto;

import com.covolt.backend.features.email.enums.EmailPriority;
import com.covolt.backend.features.email.enums.EmailType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for sending bulk emails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailRequest {
    
    @NotNull(message = "Email type is required")
    private EmailType emailType;
    
    @NotEmpty(message = "At least one recipient is required")
    private List<BulkEmailRecipient> recipients;
    
    // Common subject for all emails (will use default from EmailType if not provided)
    private String subject;
    
    // Common template variables (can be overridden per recipient)
    @Builder.Default
    private Map<String, Object> commonTemplateVariables = Map.of();
    
    // Priority level
    @Builder.Default
    private EmailPriority priority = EmailPriority.NORMAL;
    
    // Scheduled sending time (optional)
    private LocalDateTime scheduledAt;
    
    // Batch processing settings
    @Builder.Default
    private int batchSize = 50; // How many emails to send at once
    
    @Builder.Default
    private long batchDelayMs = 1000; // Delay between batches (to avoid rate limiting)
    
    // Retry configuration
    @Builder.Default
    private int maxRetries = 3;
    
    // Context info
    private String userId; // User who triggered the bulk email
    private String companyId; // Company context
    private String campaignId; // Campaign identifier
    private String campaignName; // Campaign name
    
    // Email tracking
    @Builder.Default
    private boolean trackOpens = true;
    
    @Builder.Default
    private boolean trackClicks = true;
    
    // Custom sender info (optional)
    private String fromName;
    private String fromEmail;
    
    // Additional notes for logging/debugging
    private String notes;
    
    /**
     * Individual recipient data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkEmailRecipient {
        
        private String email;
        private String name; // Optional recipient name
        
        // Recipient-specific template variables
        @Builder.Default
        private Map<String, Object> templateVariables = Map.of();
        
        // Recipient-specific metadata
        private String userId;
        private String referenceId;
        private String referenceType;
        
        // Optional custom subject for this recipient
        private String customSubject;
    }
}
