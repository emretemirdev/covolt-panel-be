package com.covolt.backend.features.email.service;

import com.covolt.backend.features.email.config.EmailConfig;
import com.covolt.backend.features.email.dto.*;
import com.covolt.backend.features.email.enums.EmailPriority;
import com.covolt.backend.features.email.enums.EmailStatus;
import com.covolt.backend.features.email.enums.EmailType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of EmailFeatureService
 * This is the main email service that handles all email operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailFeatureServiceImpl implements EmailFeatureService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailConfig emailConfig;

    // In-memory storage for demo purposes (in production, use database)
    private final Map<String, EmailResponse> emailStorage = new ConcurrentHashMap<>();
    private final Map<String, BulkEmailResponse> bulkEmailStorage = new ConcurrentHashMap<>();

    @Override
    public EmailResponse sendEmail(EmailRequest request) {
        log.debug("Sending email: type={}, to={}", request.getEmailType(), request.getTo());

        if (!emailConfig.isEnabled()) {
            log.info("Email service is disabled. Skipping email: {}", request.getTo());
            return createFailedResponse(request, "Email service is disabled");
        }

        String emailId = UUID.randomUUID().toString();
        EmailResponse response = createEmailResponse(emailId, request);

        try {
            // Build email content
            String subject = buildSubject(request);
            String htmlContent = buildHtmlContent(request);

            // Send email
            sendEmailMessage(request.getTo(), subject, htmlContent, request);

            // Update response with success
            response.setStatus(EmailStatus.SENT);
            response.setSentAt(LocalDateTime.now());
            response.setProcessingTimeMs(System.currentTimeMillis() - response.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());

            log.info("Email sent successfully: id={}, type={}, to={}", emailId, request.getEmailType(), request.getTo());

        } catch (Exception e) {
            log.error("Failed to send email: id={}, type={}, to={}", emailId, request.getEmailType(), request.getTo(), e);
            response.setStatus(EmailStatus.FAILED);
            response.setErrorMessage(e.getMessage());
            response.setErrorCode("SEND_FAILED");
        }

        // Store response
        emailStorage.put(emailId, response);
        return response;
    }

    @Override
    @Async
    public String sendEmailAsync(EmailRequest request) {
        String emailId = UUID.randomUUID().toString();
        log.debug("Sending email asynchronously: id={}, type={}, to={}", emailId, request.getEmailType(), request.getTo());

        CompletableFuture.runAsync(() -> {
            try {
                EmailResponse response = sendEmail(request);
                response.setEmailId(emailId);
                emailStorage.put(emailId, response);
            } catch (Exception e) {
                log.error("Failed to send async email: id={}", emailId, e);
                EmailResponse failedResponse = createFailedResponse(request, e.getMessage());
                failedResponse.setEmailId(emailId);
                emailStorage.put(emailId, failedResponse);
            }
        });

        return emailId;
    }

    @Override
    public EmailResponse scheduleEmail(EmailRequest request) {
        log.debug("Scheduling email: type={}, to={}, scheduledAt={}",
                request.getEmailType(), request.getTo(), request.getScheduledAt());

        String emailId = UUID.randomUUID().toString();
        EmailResponse response = createEmailResponse(emailId, request);
        response.setStatus(EmailStatus.PENDING);
        response.setScheduledAt(request.getScheduledAt());

        // Store for later processing
        emailStorage.put(emailId, response);

        log.info("Email scheduled: id={}, scheduledAt={}", emailId, request.getScheduledAt());
        return response;
    }

    @Override
    public BulkEmailResponse sendBulkEmail(BulkEmailRequest request) {
        log.debug("Sending bulk email: type={}, recipients={}", request.getEmailType(), request.getRecipients().size());

        String bulkEmailId = UUID.randomUUID().toString();
        BulkEmailResponse bulkResponse = createBulkEmailResponse(bulkEmailId, request);

        try {
            List<EmailResponse> emailResults = new ArrayList<>();
            int successCount = 0;
            int failedCount = 0;

            // Process recipients in batches
            List<BulkEmailRequest.BulkEmailRecipient> recipients = request.getRecipients();
            int batchSize = request.getBatchSize();

            for (int i = 0; i < recipients.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, recipients.size());
                List<BulkEmailRequest.BulkEmailRecipient> batch = recipients.subList(i, endIndex);

                for (BulkEmailRequest.BulkEmailRecipient recipient : batch) {
                    EmailRequest emailRequest = buildEmailRequestFromBulk(request, recipient);
                    EmailResponse emailResponse = sendEmail(emailRequest);
                    emailResults.add(emailResponse);

                    if (emailResponse.getStatus().isSuccessful()) {
                        successCount++;
                    } else {
                        failedCount++;
                    }
                }

                // Add delay between batches
                if (i + batchSize < recipients.size()) {
                    try {
                        Thread.sleep(request.getBatchDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            // Update bulk response
            bulkResponse.setEmailResults(emailResults);
            bulkResponse.setSuccessCount(successCount);
            bulkResponse.setFailedCount(failedCount);
            bulkResponse.setProcessedCount(successCount + failedCount);
            bulkResponse.setCompletedAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Failed to send bulk email: id={}", bulkEmailId, e);
            bulkResponse.setFailedCount(request.getRecipients().size());
        }

        // Store bulk response
        bulkEmailStorage.put(bulkEmailId, bulkResponse);
        return bulkResponse;
    }

    @Override
    @Async
    public String sendBulkEmailAsync(BulkEmailRequest request) {
        String bulkEmailId = UUID.randomUUID().toString();
        log.debug("Sending bulk email asynchronously: id={}, recipients={}", bulkEmailId, request.getRecipients().size());

        CompletableFuture.runAsync(() -> {
            try {
                BulkEmailResponse response = sendBulkEmail(request);
                response.setBulkEmailId(bulkEmailId);
                bulkEmailStorage.put(bulkEmailId, response);
            } catch (Exception e) {
                log.error("Failed to send async bulk email: id={}", bulkEmailId, e);
            }
        });

        return bulkEmailId;
    }

    // === QUICK SEND METHODS ===

    @Override
    public EmailResponse sendWelcomeEmail(String email, String fullName, String companyName,
                                        String temporaryPassword, Map<String, Object> additionalData) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", fullName);
        variables.put("companyName", companyName);
        variables.put("temporaryPassword", temporaryPassword);
        variables.put("email", email);
        if (additionalData != null) {
            variables.putAll(additionalData);
        }

        EmailRequest request = EmailRequest.builder()
                .emailType(EmailType.WELCOME)
                .to(email)
                .templateVariables(variables)
                .priority(EmailPriority.HIGH)
                .trackOpens(true)
                .trackClicks(true)
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendEmailVerification(String email, String fullName, String verificationToken,
                                             String verificationUrl) {
        Map<String, Object> variables = Map.of(
                "fullName", fullName,
                "email", email,
                "verificationToken", verificationToken,
                "verificationUrl", verificationUrl,
                "expirationMinutes", 60
        );

        EmailRequest request = EmailRequest.builder()
                .emailType(EmailType.EMAIL_VERIFICATION)
                .to(email)
                .templateVariables(variables)
                .priority(EmailPriority.HIGH)
                .trackOpens(true)
                .trackClicks(true)
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendPasswordResetEmail(String email, String fullName, String resetToken,
                                              String resetUrl, int expirationMinutes) {
        Map<String, Object> variables = Map.of(
                "fullName", fullName,
                "email", email,
                "resetToken", resetToken,
                "resetUrl", resetUrl,
                "expirationMinutes", expirationMinutes,
                "requestTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        EmailRequest request = EmailRequest.builder()
                .emailType(EmailType.PASSWORD_RESET)
                .to(email)
                .templateVariables(variables)
                .priority(EmailPriority.HIGH)
                .trackOpens(true)
                .trackClicks(true)
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendPasswordChangedNotification(String email, String fullName,
                                                        LocalDateTime changedAt, String ipAddress) {
        Map<String, Object> variables = Map.of(
                "fullName", fullName,
                "email", email,
                "changedAt", changedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                "ipAddress", ipAddress != null ? ipAddress : "Bilinmiyor"
        );

        EmailRequest request = EmailRequest.builder()
                .emailType(EmailType.PASSWORD_CHANGED)
                .to(email)
                .templateVariables(variables)
                .priority(EmailPriority.NORMAL)
                .trackOpens(true)
                .build();

        return sendEmail(request);
    }

    @Override
    public EmailResponse sendAccountLockedNotification(String email, String fullName, String reason,
                                                     LocalDateTime lockedAt) {
        Map<String, Object> variables = Map.of(
                "fullName", fullName,
                "email", email,
                "reason", reason != null ? reason : "Güvenlik nedeniyle",
                "lockedAt", lockedAt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        EmailRequest request = EmailRequest.builder()
                .emailType(EmailType.ACCOUNT_LOCKED)
                .to(email)
                .templateVariables(variables)
                .priority(EmailPriority.URGENT)
                .trackOpens(true)
                .build();

        return sendEmail(request);
    }

    // === Helper Methods ===

    private EmailResponse createEmailResponse(String emailId, EmailRequest request) {
        return EmailResponse.builder()
                .emailId(emailId)
                .emailType(request.getEmailType())
                .status(EmailStatus.PENDING)
                .priority(request.getPriority())
                .to(request.getTo())
                .cc(request.getCc())
                .bcc(request.getBcc())
                .subject(buildSubject(request))
                .templatePath(request.getEmailType().getTemplatePath())
                .createdAt(LocalDateTime.now())
                .scheduledAt(request.getScheduledAt())
                .trackOpens(request.isTrackOpens())
                .trackClicks(request.isTrackClicks())
                .maxRetries(request.getMaxRetries())
                .userId(request.getUserId())
                .companyId(request.getCompanyId())
                .referenceId(request.getReferenceId())
                .referenceType(request.getReferenceType())
                .fromName(request.getFromName() != null ? request.getFromName() : emailConfig.getDefaultFromName())
                .fromEmail(request.getFromEmail() != null ? request.getFromEmail() : emailConfig.getDefaultFromEmail())
                .notes(request.getNotes())
                .build();
    }

    private EmailResponse createFailedResponse(EmailRequest request, String errorMessage) {
        EmailResponse response = createEmailResponse(UUID.randomUUID().toString(), request);
        response.setStatus(EmailStatus.FAILED);
        response.setErrorMessage(errorMessage);
        response.setErrorCode("SERVICE_DISABLED");
        return response;
    }

    private BulkEmailResponse createBulkEmailResponse(String bulkEmailId, BulkEmailRequest request) {
        return BulkEmailResponse.builder()
                .bulkEmailId(bulkEmailId)
                .emailType(request.getEmailType())
                .priority(request.getPriority())
                .campaignId(request.getCampaignId())
                .campaignName(request.getCampaignName())
                .createdAt(LocalDateTime.now())
                .scheduledAt(request.getScheduledAt())
                .startedAt(LocalDateTime.now())
                .totalRecipients(request.getRecipients().size())
                .batchSize(request.getBatchSize())
                .totalBatches((int) Math.ceil((double) request.getRecipients().size() / request.getBatchSize()))
                .batchDelayMs(request.getBatchDelayMs())
                .userId(request.getUserId())
                .companyId(request.getCompanyId())
                .notes(request.getNotes())
                .emailResults(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();
    }

    private String buildSubject(EmailRequest request) {
        if (request.getSubject() != null && !request.getSubject().trim().isEmpty()) {
            return request.getSubject();
        }
        return request.getEmailType().getDefaultSubject();
    }

    private String buildHtmlContent(EmailRequest request) {
        try {
            Context context = new Context();

            // Add all template variables
            request.getTemplateVariables().forEach(context::setVariable);

            // Add common variables
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("companyName", "Covolt");
            context.setVariable("supportEmail", "support@covolt.com");

            return templateEngine.process(request.getEmailType().getTemplatePath(), context);
        } catch (Exception e) {
            log.error("Failed to build HTML content for email type: {}", request.getEmailType(), e);
            return buildFallbackContent(request);
        }
    }

    private String buildFallbackContent(EmailRequest request) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>%s</h2>
                <p>Bu email %s türünde bir bildirimdir.</p>
                <p>Template yüklenemediği için basit format kullanılmıştır.</p>
                <hr>
                <p style="font-size: 12px; color: #666;">Bu email Covolt tarafından gönderilmiştir.</p>
            </body>
            </html>
            """, request.getEmailType().getDefaultSubject(), request.getEmailType().getCode());
    }

    private void sendEmailMessage(String to, String subject, String htmlContent, EmailRequest request)
            throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(request.getFromEmail() != null ? request.getFromEmail() : emailConfig.getDefaultFromEmail(),
                          request.getFromName() != null ? request.getFromName() : emailConfig.getDefaultFromName());
        } catch (java.io.UnsupportedEncodingException e) {
            // Fallback to simple email without name
            helper.setFrom(request.getFromEmail() != null ? request.getFromEmail() : emailConfig.getDefaultFromEmail());
        }
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        // Add CC recipients
        if (request.getCc() != null && !request.getCc().isEmpty()) {
            helper.setCc(request.getCc().toArray(new String[0]));
        }

        // Add BCC recipients
        if (request.getBcc() != null && !request.getBcc().isEmpty()) {
            helper.setBcc(request.getBcc().toArray(new String[0]));
        }

        mailSender.send(message);
    }

    private EmailRequest buildEmailRequestFromBulk(BulkEmailRequest bulkRequest,
                                                  BulkEmailRequest.BulkEmailRecipient recipient) {
        Map<String, Object> variables = new HashMap<>(bulkRequest.getCommonTemplateVariables());
        variables.putAll(recipient.getTemplateVariables());

        // Add recipient-specific variables
        variables.put("recipientName", recipient.getName());
        variables.put("recipientEmail", recipient.getEmail());

        return EmailRequest.builder()
                .emailType(bulkRequest.getEmailType())
                .to(recipient.getEmail())
                .subject(recipient.getCustomSubject() != null ? recipient.getCustomSubject() : bulkRequest.getSubject())
                .templateVariables(variables)
                .priority(bulkRequest.getPriority())
                .scheduledAt(bulkRequest.getScheduledAt())
                .maxRetries(bulkRequest.getMaxRetries())
                .userId(bulkRequest.getUserId())
                .companyId(bulkRequest.getCompanyId())
                .referenceId(recipient.getReferenceId())
                .referenceType(recipient.getReferenceType())
                .trackOpens(bulkRequest.isTrackOpens())
                .trackClicks(bulkRequest.isTrackClicks())
                .fromName(bulkRequest.getFromName())
                .fromEmail(bulkRequest.getFromEmail())
                .notes(bulkRequest.getNotes())
                .build();
    }

    // === System Management Methods ===

    @Override
    public boolean isEmailServiceEnabled() {
        return emailConfig.isEnabled();
    }

    @Override
    public Map<String, Object> getServiceHealthStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", emailConfig.isEnabled());
        status.put("emailsInStorage", emailStorage.size());
        status.put("bulkEmailsInStorage", bulkEmailStorage.size());
        status.put("lastHealthCheck", LocalDateTime.now());
        status.put("status", "HEALTHY");
        return status;
    }

    // === Stub implementations for remaining methods ===

    @Override
    public EmailResponse sendCompanyTransferNotification(String email, String fullName,
                                                        String fromCompany, String toCompany, String reason) {
        // Implementation will be added in next part
        return null;
    }

    @Override
    public EmailResponse sendRoleUpdatedNotification(String email, String fullName, String companyName,
                                                   List<String> newRoles, String reason) {
        // Implementation will be added in next part
        return null;
    }

    @Override
    public EmailResponse sendSubscriptionNotification(EmailType subscriptionType, String email,
                                                     String fullName, String companyName,
                                                     Map<String, Object> subscriptionData) {
        // Implementation will be added in next part
        return null;
    }

    @Override
    public EmailResponse sendLoginAlert(String email, String fullName, LocalDateTime loginTime,
                                       String ipAddress, String userAgent, String location) {
        // Implementation will be added in next part
        return null;
    }

    @Override
    public Optional<EmailResponse> getEmailById(String emailId) {
        return Optional.ofNullable(emailStorage.get(emailId));
    }

    @Override
    public Page<EmailResponse> getEmailsByUser(String userId, Pageable pageable) {
        // Simple implementation - in production use database
        List<EmailResponse> userEmails = emailStorage.values().stream()
                .filter(email -> userId.equals(email.getUserId()))
                .toList();
        return new PageImpl<>(userEmails, pageable, userEmails.size());
    }

    @Override
    public Page<EmailResponse> getEmailsByCompany(String companyId, Pageable pageable) {
        List<EmailResponse> companyEmails = emailStorage.values().stream()
                .filter(email -> companyId.equals(email.getCompanyId()))
                .toList();
        return new PageImpl<>(companyEmails, pageable, companyEmails.size());
    }

    @Override
    public Page<EmailResponse> getEmailsByType(EmailType emailType, Pageable pageable) {
        List<EmailResponse> typeEmails = emailStorage.values().stream()
                .filter(email -> emailType.equals(email.getEmailType()))
                .toList();
        return new PageImpl<>(typeEmails, pageable, typeEmails.size());
    }

    @Override
    public Page<EmailResponse> getEmailsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                                   Pageable pageable) {
        List<EmailResponse> dateEmails = emailStorage.values().stream()
                .filter(email -> email.getCreatedAt().isAfter(startDate) && email.getCreatedAt().isBefore(endDate))
                .toList();
        return new PageImpl<>(dateEmails, pageable, dateEmails.size());
    }

    @Override
    public Optional<BulkEmailResponse> getBulkEmailById(String bulkEmailId) {
        return Optional.ofNullable(bulkEmailStorage.get(bulkEmailId));
    }

    @Override
    public Page<BulkEmailResponse> getBulkEmailsByCampaign(String campaignId, Pageable pageable) {
        List<BulkEmailResponse> campaignEmails = bulkEmailStorage.values().stream()
                .filter(bulk -> campaignId.equals(bulk.getCampaignId()))
                .toList();
        return new PageImpl<>(campaignEmails, pageable, campaignEmails.size());
    }

    @Override
    public EmailResponse retryEmail(String emailId) {
        EmailResponse email = emailStorage.get(emailId);
        if (email != null && email.canRetry()) {
            // Create new request from existing email
            EmailRequest retryRequest = EmailRequest.builder()
                    .emailType(email.getEmailType())
                    .to(email.getTo())
                    .subject(email.getSubject())
                    .build();

            return sendEmail(retryRequest);
        }
        return null;
    }

    @Override
    public boolean cancelScheduledEmail(String emailId) {
        EmailResponse email = emailStorage.get(emailId);
        if (email != null && email.getStatus() == EmailStatus.PENDING) {
            email.setStatus(EmailStatus.CANCELLED);
            return true;
        }
        return false;
    }

    @Override
    public boolean cancelBulkEmail(String bulkEmailId) {
        BulkEmailResponse bulkEmail = bulkEmailStorage.get(bulkEmailId);
        if (bulkEmail != null && !bulkEmail.isCompleted()) {
            bulkEmail.setCompletedAt(LocalDateTime.now());
            return true;
        }
        return false;
    }
}
