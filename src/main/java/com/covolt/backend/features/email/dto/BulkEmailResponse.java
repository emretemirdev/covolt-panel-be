package com.covolt.backend.features.email.dto;

import com.covolt.backend.features.email.enums.EmailPriority;
import com.covolt.backend.features.email.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for bulk email operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailResponse {
    
    private String bulkEmailId; // Unique identifier for this bulk email operation
    private EmailType emailType;
    private EmailPriority priority;
    
    // Campaign info
    private String campaignId;
    private String campaignName;
    
    // Processing info
    private LocalDateTime createdAt;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    // Statistics
    private int totalRecipients;
    private int processedCount;
    private int successCount;
    private int failedCount;
    private int pendingCount;
    
    // Batch processing info
    private int batchSize;
    private int totalBatches;
    private int completedBatches;
    private long batchDelayMs;
    
    // Individual email results
    private List<EmailResponse> emailResults;
    
    // Error summary
    private List<BulkEmailError> errors;
    
    // Context info
    private String userId;
    private String companyId;
    
    // Processing metrics
    private long totalProcessingTimeMs;
    private double averageProcessingTimeMs;
    private String processingNode;
    
    // Additional metadata
    private Map<String, Object> metadata;
    private String notes;
    
    /**
     * Calculates the percentage of successfully sent emails out of the total recipients.
     *
     * @return the success rate as a percentage, or 0.0 if there are no recipients
     */
    public double getSuccessRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) successCount / totalRecipients * 100.0;
    }
    
    /**
     * Calculates the failure rate of the bulk email operation as a percentage.
     *
     * @return the percentage of failed emails out of the total recipients, or 0.0 if there are no recipients
     */
    public double getFailureRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) failedCount / totalRecipients * 100.0;
    }
    
    /**
     * Determines whether the bulk email operation has completed.
     *
     * @return true if the completion timestamp is set and all recipients have been processed; false otherwise
     */
    public boolean isCompleted() {
        return completedAt != null && processedCount == totalRecipients;
    }
    
    /**
     * Determines whether the bulk email operation has started but not yet completed.
     *
     * @return true if the operation has a start timestamp and no completion timestamp; false otherwise
     */
    public boolean isInProgress() {
        return startedAt != null && completedAt == null;
    }
    
    /**
     * Calculates the percentage of recipients that have been processed in the bulk email operation.
     *
     * @return the progress percentage, or 0.0 if there are no recipients
     */
    public double getProgressPercentage() {
        if (totalRecipients == 0) return 0.0;
        return (double) processedCount / totalRecipients * 100.0;
    }
    
    /**
     * Error details for bulk email
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkEmailError {
        private String email;
        private String errorMessage;
        private String errorCode;
        private LocalDateTime occurredAt;
        private int retryCount;
    }
}
