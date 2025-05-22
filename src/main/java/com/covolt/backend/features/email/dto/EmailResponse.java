package com.covolt.backend.features.email.dto;

import com.covolt.backend.features.email.enums.EmailPriority;
import com.covolt.backend.features.email.enums.EmailStatus;
import com.covolt.backend.features.email.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for email operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    
    private String emailId; // Unique identifier for this email
    private EmailType emailType;
    private EmailStatus status;
    private EmailPriority priority;
    
    // Recipients
    private String to;
    private List<String> cc;
    private List<String> bcc;
    
    // Content
    private String subject;
    private String templatePath;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    
    // Tracking info
    private boolean trackOpens;
    private boolean trackClicks;
    private int openCount;
    private int clickCount;
    private LocalDateTime lastOpenedAt;
    private LocalDateTime lastClickedAt;
    
    // Retry info
    private int retryCount;
    private int maxRetries;
    private LocalDateTime nextRetryAt;
    
    // Error info (if failed)
    private String errorMessage;
    private String errorCode;
    
    // Context info
    private String userId;
    private String companyId;
    private String referenceId;
    private String referenceType;
    
    // Sender info
    private String fromName;
    private String fromEmail;
    
    // Additional metadata
    private Map<String, Object> metadata;
    private String notes;
    
    // Processing info
    private String processingNode; // Which server processed this email
    private long processingTimeMs; // How long it took to process
    
    /**
     * Check if email can be retried
     */
    public boolean canRetry() {
        return status.isFailed() && retryCount < maxRetries;
    }
    
    /**
     * Check if email is still pending or processing
     */
    public boolean isPending() {
        return status == EmailStatus.PENDING || status == EmailStatus.SENDING;
    }
    
    /**
     * Get success rate for tracking
     */
    public double getSuccessRate() {
        if (status.isSuccessful()) {
            return 1.0;
        } else if (status.isFailed()) {
            return 0.0;
        } else {
            return 0.5; // Pending/processing
        }
    }
}
