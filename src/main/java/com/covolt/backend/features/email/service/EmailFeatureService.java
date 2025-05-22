package com.covolt.backend.features.email.service;

import com.covolt.backend.features.email.dto.*;
import com.covolt.backend.features.email.enums.EmailType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Main service interface for email feature
 * This is the new centralized email service that handles all email operations
 */
public interface EmailFeatureService {

    // === SINGLE EMAIL OPERATIONS ===

    /**
     * Send a single email
     *
     * @param request Email request with all details
     * @return Email response with status and tracking info
     */
    EmailResponse sendEmail(EmailRequest request);

    /**
     * Send a single email asynchronously
     *
     * @param request Email request with all details
     * @return Email ID for tracking
     */
    String sendEmailAsync(EmailRequest request);

    /**
     * Schedule an email to be sent later
     *
     * @param request Email request with scheduled time
     * @return Email response with scheduling info
     */
    EmailResponse scheduleEmail(EmailRequest request);

    // === BULK EMAIL OPERATIONS ===

    /**
     * Send bulk emails
     *
     * @param request Bulk email request
     * @return Bulk email response with statistics
     */
    BulkEmailResponse sendBulkEmail(BulkEmailRequest request);

    /**
     * Send bulk emails asynchronously
     *
     * @param request Bulk email request
     * @return Bulk email ID for tracking
     */
    String sendBulkEmailAsync(BulkEmailRequest request);

    // === QUICK SEND METHODS (for common use cases) ===

    /**
     * Send welcome email to new user
     */
    EmailResponse sendWelcomeEmail(String email, String fullName, String companyName,
                                 String temporaryPassword, Map<String, Object> additionalData);

    /**
     * Send email verification email
     */
    EmailResponse sendEmailVerification(String email, String fullName, String verificationToken,
                                      String verificationUrl);

    /**
     * Send password reset email
     */
    EmailResponse sendPasswordResetEmail(String email, String fullName, String resetToken,
                                       String resetUrl, int expirationMinutes);

    /**
     * Send password changed notification
     */
    EmailResponse sendPasswordChangedNotification(String email, String fullName,
                                                 LocalDateTime changedAt, String ipAddress);

    /**
     * Send account locked notification
     */
    EmailResponse sendAccountLockedNotification(String email, String fullName, String reason,
                                              LocalDateTime lockedAt);

    /**
     * Send company transfer notification
     */
    EmailResponse sendCompanyTransferNotification(String email, String fullName,
                                                 String fromCompany, String toCompany, String reason);

    /**
     * Send role updated notification
     */
    EmailResponse sendRoleUpdatedNotification(String email, String fullName, String companyName,
                                            List<String> newRoles, String reason);

    /**
     * Send subscription notification
     */
    EmailResponse sendSubscriptionNotification(EmailType subscriptionType, String email,
                                              String fullName, String companyName,
                                              Map<String, Object> subscriptionData);

    /**
     * Send login alert
     */
    EmailResponse sendLoginAlert(String email, String fullName, LocalDateTime loginTime,
                               String ipAddress, String userAgent, String location);

    // === EMAIL TRACKING AND MANAGEMENT ===

    /**
     * Get email by ID
     */
    Optional<EmailResponse> getEmailById(String emailId);

    /**
     * Get emails by user
     */
    Page<EmailResponse> getEmailsByUser(String userId, Pageable pageable);

    /**
     * Get emails by company
     */
    Page<EmailResponse> getEmailsByCompany(String companyId, Pageable pageable);

    /**
     * Get emails by type
     */
    Page<EmailResponse> getEmailsByType(EmailType emailType, Pageable pageable);

    /**
     * Get emails by date range
     */
    Page<EmailResponse> getEmailsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                           Pageable pageable);

    /**
     * Get bulk email by ID
     */
    Optional<BulkEmailResponse> getBulkEmailById(String bulkEmailId);

    /**
     * Get bulk emails by campaign
     */
    Page<BulkEmailResponse> getBulkEmailsByCampaign(String campaignId, Pageable pageable);

    // === EMAIL RETRY AND MANAGEMENT ===

    /**
     * Retry failed email
     */
    EmailResponse retryEmail(String emailId);

    /**
     * Cancel scheduled email
     */
    boolean cancelScheduledEmail(String emailId);

    /**
     * Cancel bulk email operation
     */
    boolean cancelBulkEmail(String bulkEmailId);

    // === SYSTEM MANAGEMENT ===

    /**
     * Check if email service is enabled
     */
    boolean isEmailServiceEnabled();

    /**
     * Get email service health status
     */
    Map<String, Object> getServiceHealthStatus();
}
