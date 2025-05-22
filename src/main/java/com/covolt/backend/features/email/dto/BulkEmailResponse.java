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
     * Get success rate as percentage
     */
    public double getSuccessRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) successCount / totalRecipients * 100.0;
    }
    
    /**
     * Get failure rate as percentage
     */
    public double getFailureRate() {
        if (totalRecipients == 0) return 0.0;
        return (double) failedCount / totalRecipients * 100.0;
    }
    
    /**
     * Check if bulk email operation is completed
     */
    public boolean isCompleted() {
        return completedAt != null && processedCount == totalRecipients;
    }
    
    /**
     * Check if bulk email operation is still in progress
     */
    public boolean isInProgress() {
        return startedAt != null && completedAt == null;
    }
    
    /**
     * Get progress percentage
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
