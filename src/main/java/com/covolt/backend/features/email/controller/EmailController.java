package com.covolt.backend.features.email.controller;

import com.covolt.backend.features.email.dto.*;
import com.covolt.backend.features.email.enums.EmailType;
import com.covolt.backend.features.email.service.EmailFeatureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for Email Feature
 * Handles all email-related operations
 */
@RestController
@RequestMapping("/api/v1/email")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Email Feature", description = "Centralized email management system")
public class EmailController {

    private final EmailFeatureService emailFeatureService;

    // === SINGLE EMAIL OPERATIONS ===

    @PostMapping("/send")
    @PreAuthorize("hasAuthority('SEND_EMAIL')")
    @Operation(summary = "Send single email", description = "Send a single email with specified type and content")
    public ResponseEntity<EmailResponse> sendEmail(@Valid @RequestBody EmailRequest request) {
        log.debug("REST request to send email: type={}, to={}", request.getEmailType(), request.getTo());
        EmailResponse response = emailFeatureService.sendEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/send-async")
    @PreAuthorize("hasAuthority('SEND_EMAIL')")
    @Operation(summary = "Send email asynchronously", description = "Send email asynchronously and return tracking ID")
    public ResponseEntity<Map<String, String>> sendEmailAsync(@Valid @RequestBody EmailRequest request) {
        log.debug("REST request to send email async: type={}, to={}", request.getEmailType(), request.getTo());
        String emailId = emailFeatureService.sendEmailAsync(request);
        return ResponseEntity.ok(Map.of("emailId", emailId, "status", "QUEUED"));
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAuthority('SCHEDULE_EMAIL')")
    @Operation(summary = "Schedule email", description = "Schedule an email to be sent at a specific time")
    public ResponseEntity<EmailResponse> scheduleEmail(@Valid @RequestBody EmailRequest request) {
        log.debug("REST request to schedule email: type={}, to={}, scheduledAt={}", 
                request.getEmailType(), request.getTo(), request.getScheduledAt());
        EmailResponse response = emailFeatureService.scheduleEmail(request);
        return ResponseEntity.ok(response);
    }

    // === BULK EMAIL OPERATIONS ===

    @PostMapping("/bulk/send")
    @PreAuthorize("hasAuthority('SEND_BULK_EMAIL')")
    @Operation(summary = "Send bulk emails", description = "Send emails to multiple recipients")
    public ResponseEntity<BulkEmailResponse> sendBulkEmail(@Valid @RequestBody BulkEmailRequest request) {
        log.debug("REST request to send bulk email: type={}, recipients={}", 
                request.getEmailType(), request.getRecipients().size());
        BulkEmailResponse response = emailFeatureService.sendBulkEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/bulk/send-async")
    @PreAuthorize("hasAuthority('SEND_BULK_EMAIL')")
    @Operation(summary = "Send bulk emails asynchronously", description = "Send bulk emails asynchronously")
    public ResponseEntity<Map<String, String>> sendBulkEmailAsync(@Valid @RequestBody BulkEmailRequest request) {
        log.debug("REST request to send bulk email async: type={}, recipients={}", 
                request.getEmailType(), request.getRecipients().size());
        String bulkEmailId = emailFeatureService.sendBulkEmailAsync(request);
        return ResponseEntity.ok(Map.of("bulkEmailId", bulkEmailId, "status", "QUEUED"));
    }

    // === QUICK SEND ENDPOINTS ===

    @PostMapping("/quick/welcome")
    @PreAuthorize("hasAuthority('SEND_EMAIL')")
    @Operation(summary = "Send welcome email", description = "Quick method to send welcome email")
    public ResponseEntity<EmailResponse> sendWelcomeEmail(@RequestBody Map<String, Object> request) {
        log.debug("REST request to send welcome email: {}", request.get("email"));
        
        EmailResponse response = emailFeatureService.sendWelcomeEmail(
                (String) request.get("email"),
                (String) request.get("fullName"),
                (String) request.get("companyName"),
                (String) request.get("temporaryPassword"),
                (Map<String, Object>) request.get("additionalData")
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quick/verification")
    @PreAuthorize("hasAuthority('SEND_EMAIL')")
    @Operation(summary = "Send email verification", description = "Quick method to send email verification")
    public ResponseEntity<EmailResponse> sendEmailVerification(@RequestBody Map<String, Object> request) {
        log.debug("REST request to send email verification: {}", request.get("email"));
        
        EmailResponse response = emailFeatureService.sendEmailVerification(
                (String) request.get("email"),
                (String) request.get("fullName"),
                (String) request.get("verificationToken"),
                (String) request.get("verificationUrl")
        );
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quick/password-reset")
    @PreAuthorize("hasAuthority('SEND_EMAIL')")
    @Operation(summary = "Send password reset email", description = "Quick method to send password reset email")
    public ResponseEntity<EmailResponse> sendPasswordResetEmail(@RequestBody Map<String, Object> request) {
        log.debug("REST request to send password reset email: {}", request.get("email"));
        
        EmailResponse response = emailFeatureService.sendPasswordResetEmail(
                (String) request.get("email"),
                (String) request.get("fullName"),
                (String) request.get("resetToken"),
                (String) request.get("resetUrl"),
                (Integer) request.getOrDefault("expirationMinutes", 30)
        );
        
        return ResponseEntity.ok(response);
    }

    // === EMAIL TRACKING AND MANAGEMENT ===

    @GetMapping("/{emailId}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get email by ID", description = "Retrieve email details by ID")
    public ResponseEntity<EmailResponse> getEmailById(@PathVariable String emailId) {
        log.debug("REST request to get email: {}", emailId);
        Optional<EmailResponse> email = emailFeatureService.getEmailById(emailId);
        return email.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get emails by user", description = "Get all emails for a specific user")
    public ResponseEntity<Page<EmailResponse>> getEmailsByUser(
            @PathVariable String userId, 
            Pageable pageable) {
        log.debug("REST request to get emails by user: {}", userId);
        Page<EmailResponse> emails = emailFeatureService.getEmailsByUser(userId, pageable);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get emails by company", description = "Get all emails for a specific company")
    public ResponseEntity<Page<EmailResponse>> getEmailsByCompany(
            @PathVariable String companyId, 
            Pageable pageable) {
        log.debug("REST request to get emails by company: {}", companyId);
        Page<EmailResponse> emails = emailFeatureService.getEmailsByCompany(companyId, pageable);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/type/{emailType}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get emails by type", description = "Get all emails of a specific type")
    public ResponseEntity<Page<EmailResponse>> getEmailsByType(
            @PathVariable EmailType emailType, 
            Pageable pageable) {
        log.debug("REST request to get emails by type: {}", emailType);
        Page<EmailResponse> emails = emailFeatureService.getEmailsByType(emailType, pageable);
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/date-range")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get emails by date range", description = "Get emails within a date range")
    public ResponseEntity<Page<EmailResponse>> getEmailsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            Pageable pageable) {
        log.debug("REST request to get emails by date range: {} to {}", startDate, endDate);
        Page<EmailResponse> emails = emailFeatureService.getEmailsByDateRange(startDate, endDate, pageable);
        return ResponseEntity.ok(emails);
    }

    // === BULK EMAIL TRACKING ===

    @GetMapping("/bulk/{bulkEmailId}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get bulk email by ID", description = "Retrieve bulk email details by ID")
    public ResponseEntity<BulkEmailResponse> getBulkEmailById(@PathVariable String bulkEmailId) {
        log.debug("REST request to get bulk email: {}", bulkEmailId);
        Optional<BulkEmailResponse> bulkEmail = emailFeatureService.getBulkEmailById(bulkEmailId);
        return bulkEmail.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bulk/campaign/{campaignId}")
    @PreAuthorize("hasAuthority('VIEW_EMAIL')")
    @Operation(summary = "Get bulk emails by campaign", description = "Get all bulk emails for a campaign")
    public ResponseEntity<Page<BulkEmailResponse>> getBulkEmailsByCampaign(
            @PathVariable String campaignId, 
            Pageable pageable) {
        log.debug("REST request to get bulk emails by campaign: {}", campaignId);
        Page<BulkEmailResponse> bulkEmails = emailFeatureService.getBulkEmailsByCampaign(campaignId, pageable);
        return ResponseEntity.ok(bulkEmails);
    }

    // === EMAIL MANAGEMENT ===

    @PostMapping("/{emailId}/retry")
    @PreAuthorize("hasAuthority('MANAGE_EMAIL')")
    @Operation(summary = "Retry failed email", description = "Retry sending a failed email")
    public ResponseEntity<EmailResponse> retryEmail(@PathVariable String emailId) {
        log.debug("REST request to retry email: {}", emailId);
        EmailResponse response = emailFeatureService.retryEmail(emailId);
        if (response != null) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{emailId}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_EMAIL')")
    @Operation(summary = "Cancel scheduled email", description = "Cancel a scheduled email")
    public ResponseEntity<Map<String, String>> cancelScheduledEmail(@PathVariable String emailId) {
        log.debug("REST request to cancel scheduled email: {}", emailId);
        boolean cancelled = emailFeatureService.cancelScheduledEmail(emailId);
        if (cancelled) {
            return ResponseEntity.ok(Map.of("status", "CANCELLED", "emailId", emailId));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/bulk/{bulkEmailId}/cancel")
    @PreAuthorize("hasAuthority('MANAGE_EMAIL')")
    @Operation(summary = "Cancel bulk email", description = "Cancel a bulk email operation")
    public ResponseEntity<Map<String, String>> cancelBulkEmail(@PathVariable String bulkEmailId) {
        log.debug("REST request to cancel bulk email: {}", bulkEmailId);
        boolean cancelled = emailFeatureService.cancelBulkEmail(bulkEmailId);
        if (cancelled) {
            return ResponseEntity.ok(Map.of("status", "CANCELLED", "bulkEmailId", bulkEmailId));
        }
        return ResponseEntity.notFound().build();
    }

    // === SYSTEM STATUS ===

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('VIEW_SYSTEM_STATUS')")
    @Operation(summary = "Get email service status", description = "Get current status of email service")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        log.debug("REST request to get email service status");
        Map<String, Object> status = emailFeatureService.getServiceHealthStatus();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "EmailFeature",
                "timestamp", LocalDateTime.now(),
                "enabled", emailFeatureService.isEmailServiceEnabled()
        );
        return ResponseEntity.ok(health);
    }
}
