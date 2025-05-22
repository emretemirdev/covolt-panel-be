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

    /**
     * Sends a single email synchronously using the provided request details.
     *
     * If the email service is disabled, returns a failed response without attempting to send. Generates a unique email ID, builds the subject and HTML content from templates, sends the email, updates the response status based on the outcome, and stores the result in memory.
     *
     * @param request the email request containing recipient, type, template variables, and other metadata
     * @return an {@link EmailResponse} with the status and details of the email operation
     */
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

    /**
     * Initiates asynchronous sending of an email and returns a generated email ID immediately.
     *
     * The email is processed in a background task. The resulting status and response are stored in memory and can be retrieved later using the returned email ID.
     *
     * @param request the email request containing recipient, content, and metadata
     * @return the unique ID assigned to the asynchronous email operation
     */
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

    /**
     * Schedules an email for future delivery by creating a pending email response and storing it for later processing.
     *
     * @param request the email request containing recipient, type, and scheduled time
     * @return the scheduled email response with status set to PENDING
     */
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

    /**
     * Sends a bulk email to multiple recipients, processing them in batches and tracking individual results.
     *
     * Processes the provided recipients in batches of configurable size, sending an email to each recipient using the specified template and variables. Tracks the number of successful and failed sends, applies a delay between batches if configured, and stores the results in memory. Returns a response containing detailed results for each recipient and overall bulk operation status.
     *
     * @param request the bulk email request containing recipients, template data, batch size, and delay settings
     * @return a response summarizing the outcome of the bulk email operation, including individual email results and counts
     */
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

    /**
     * Initiates asynchronous sending of bulk emails and returns the generated bulk email ID immediately.
     *
     * The bulk email operation is processed in a separate thread. The resulting response is stored in memory and can be retrieved later using the returned bulk email ID.
     *
     * @param request the bulk email request containing recipients and email details
     * @return the unique ID assigned to the bulk email operation
     */
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

    /**
     * Sends a welcome email to a new user with their temporary password and company information.
     *
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param companyName name of the company the user is joining
     * @param temporaryPassword temporary password assigned to the user
     * @param additionalData optional additional template variables for the email content
     * @return the response containing the status and details of the sent email
     */

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

    /**
     * Sends an email verification message to the specified recipient with a verification token and URL.
     *
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param verificationToken unique token for email verification
     * @param verificationUrl URL for completing the email verification process
     * @return the response containing the status and details of the sent email
     */
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

    /**
     * Sends a password reset email to the specified recipient with a reset token and expiration details.
     *
     * @param email the recipient's email address
     * @param fullName the recipient's full name
     * @param resetToken the password reset token to include in the email
     * @param resetUrl the URL for resetting the password
     * @param expirationMinutes the number of minutes before the reset token expires
     * @return the response containing the status and details of the sent email
     */
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

    /**
     * Sends a password changed notification email to the specified recipient.
     *
     * @param email the recipient's email address
     * @param fullName the recipient's full name
     * @param changedAt the date and time when the password was changed
     * @param ipAddress the IP address from which the password change occurred, or "Bilinmiyor" if unknown
     * @return the response containing the status and details of the sent email
     */
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

    /**
     * Sends an account locked notification email to the specified recipient.
     *
     * @param email the recipient's email address
     * @param fullName the recipient's full name
     * @param reason the reason for the account lock; defaults to a generic message if null
     * @param lockedAt the timestamp when the account was locked
     * @return the response containing the status and details of the sent email
     */
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

    /**
     * Constructs an {@link EmailResponse} object with initial status and metadata based on the provided email request and email ID.
     *
     * @param emailId the unique identifier for the email
     * @param request the email request containing details for the email to be sent
     * @return a new {@link EmailResponse} initialized with request data and default values
     */

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

    /**
     * Creates an {@link EmailResponse} representing a failed email operation with the specified error message.
     *
     * @param request the original email request
     * @param errorMessage the error message describing the failure
     * @return an {@link EmailResponse} with status FAILED and error details
     */
    private EmailResponse createFailedResponse(EmailRequest request, String errorMessage) {
        EmailResponse response = createEmailResponse(UUID.randomUUID().toString(), request);
        response.setStatus(EmailStatus.FAILED);
        response.setErrorMessage(errorMessage);
        response.setErrorCode("SERVICE_DISABLED");
        return response;
    }

    /**
     * Constructs a new {@link BulkEmailResponse} with metadata and initial values based on the provided bulk email request.
     *
     * @param bulkEmailId the unique identifier for the bulk email operation
     * @param request the bulk email request containing configuration and recipient details
     * @return a {@link BulkEmailResponse} initialized with request data and empty results and errors lists
     */
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

    /**
     * Determines the subject line for an email request.
     *
     * Returns the subject specified in the request if provided; otherwise, returns the default subject for the email type.
     *
     * @param request the email request containing subject and type information
     * @return the resolved email subject line
     */
    private String buildSubject(EmailRequest request) {
        if (request.getSubject() != null && !request.getSubject().trim().isEmpty()) {
            return request.getSubject();
        }
        return request.getEmailType().getDefaultSubject();
    }

    /**
     * Generates the HTML content for an email using the specified template and variables.
     *
     * If template processing fails, returns fallback HTML content.
     *
     * @param request the email request containing template variables and email type
     * @return the generated HTML content for the email
     */
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

    /**
     * Generates a simple HTML email content as a fallback when the template cannot be loaded.
     *
     * @return basic HTML content with the email type's default subject and code.
     */
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

    /**
     * Sends an email message using the provided recipient, subject, and HTML content.
     *
     * Prepares a MIME email with optional sender name, CC, and BCC recipients, and sends it via the configured mail sender.
     *
     * @param to the recipient's email address
     * @param subject the subject of the email
     * @param htmlContent the HTML content of the email body
     * @param request the email request containing sender and recipient details
     * @throws MessagingException if an error occurs while constructing or sending the email
     */
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

    /**
     * Constructs an individual {@link EmailRequest} for a bulk email recipient by merging common and recipient-specific template variables and applying recipient overrides.
     *
     * @param bulkRequest the bulk email request containing shared configuration and template variables
     * @param recipient the recipient with individual template variables and optional custom subject
     * @return an {@link EmailRequest} tailored for the specified recipient
     */
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

    /**
     * Checks whether the email service is currently enabled.
     *
     * @return true if the email service is enabled; false otherwise
     */

    @Override
    public boolean isEmailServiceEnabled() {
        return emailConfig.isEnabled();
    }

    /**
     * Returns a map containing the current health status and statistics of the email service.
     *
     * The returned map includes whether the service is enabled, the number of stored emails and bulk emails,
     * the timestamp of the health check, and a status string.
     *
     * @return a map with service health details and metrics
     */
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

    /**
     * Stub for sending a company transfer notification email.
     *
     * @return always returns null as this method is not yet implemented
     */

    @Override
    public EmailResponse sendCompanyTransferNotification(String email, String fullName,
                                                        String fromCompany, String toCompany, String reason) {
        // Implementation will be added in next part
        return null;
    }

    /**
     * Stub for sending a role updated notification email to a user.
     *
     * @return always returns null as this method is not yet implemented
     */
    @Override
    public EmailResponse sendRoleUpdatedNotification(String email, String fullName, String companyName,
                                                   List<String> newRoles, String reason) {
        // Implementation will be added in next part
        return null;
    }

    /****
     * Stub for sending a subscription-related notification email.
     *
     * @param subscriptionType the type of subscription event triggering the notification
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param companyName name of the recipient's company
     * @param subscriptionData additional data relevant to the subscription event
     * @return always returns null as this method is not yet implemented
     */
    @Override
    public EmailResponse sendSubscriptionNotification(EmailType subscriptionType, String email,
                                                     String fullName, String companyName,
                                                     Map<String, Object> subscriptionData) {
        // Implementation will be added in next part
        return null;
    }

    /**
     * Placeholder for sending a login alert email notification to a user.
     *
     * @return always returns null as this method is not yet implemented
     */
    @Override
    public EmailResponse sendLoginAlert(String email, String fullName, LocalDateTime loginTime,
                                       String ipAddress, String userAgent, String location) {
        // Implementation will be added in next part
        return null;
    }

    /**
     * Retrieves an email response by its unique ID from in-memory storage.
     *
     * @param emailId the unique identifier of the email
     * @return an Optional containing the EmailResponse if found, or empty if not present
     */
    @Override
    public Optional<EmailResponse> getEmailById(String emailId) {
        return Optional.ofNullable(emailStorage.get(emailId));
    }

    /**
     * Retrieves a paginated list of emails associated with the specified user ID.
     *
     * @param userId the ID of the user whose emails are to be retrieved
     * @param pageable pagination information for the result set
     * @return a page of email responses for the given user
     */
    @Override
    public Page<EmailResponse> getEmailsByUser(String userId, Pageable pageable) {
        // Simple implementation - in production use database
        List<EmailResponse> userEmails = emailStorage.values().stream()
                .filter(email -> userId.equals(email.getUserId()))
                .toList();
        return new PageImpl<>(userEmails, pageable, userEmails.size());
    }

    /**
     * Retrieves a paginated list of email responses associated with the specified company ID.
     *
     * @param companyId the unique identifier of the company
     * @param pageable pagination information
     * @return a page of email responses for the given company
     */
    @Override
    public Page<EmailResponse> getEmailsByCompany(String companyId, Pageable pageable) {
        List<EmailResponse> companyEmails = emailStorage.values().stream()
                .filter(email -> companyId.equals(email.getCompanyId()))
                .toList();
        return new PageImpl<>(companyEmails, pageable, companyEmails.size());
    }

    /**
     * Retrieves a paginated list of emails filtered by the specified email type.
     *
     * @param emailType the type of emails to retrieve
     * @param pageable pagination information
     * @return a page of email responses matching the given email type
     */
    @Override
    public Page<EmailResponse> getEmailsByType(EmailType emailType, Pageable pageable) {
        List<EmailResponse> typeEmails = emailStorage.values().stream()
                .filter(email -> emailType.equals(email.getEmailType()))
                .toList();
        return new PageImpl<>(typeEmails, pageable, typeEmails.size());
    }

    /**
     * Retrieves a paginated list of emails created within the specified date range.
     *
     * @param startDate the start of the date range (exclusive)
     * @param endDate the end of the date range (exclusive)
     * @param pageable pagination information
     * @return a page of emails whose creation timestamps fall within the given range
     */
    @Override
    public Page<EmailResponse> getEmailsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                                   Pageable pageable) {
        List<EmailResponse> dateEmails = emailStorage.values().stream()
                .filter(email -> email.getCreatedAt().isAfter(startDate) && email.getCreatedAt().isBefore(endDate))
                .toList();
        return new PageImpl<>(dateEmails, pageable, dateEmails.size());
    }

    /**
     * Retrieves a bulk email response by its unique ID from in-memory storage.
     *
     * @param bulkEmailId the unique identifier of the bulk email
     * @return an {@code Optional} containing the {@code BulkEmailResponse} if found, or empty if not present
     */
    @Override
    public Optional<BulkEmailResponse> getBulkEmailById(String bulkEmailId) {
        return Optional.ofNullable(bulkEmailStorage.get(bulkEmailId));
    }

    /**
     * Retrieves a paginated list of bulk email responses filtered by campaign ID.
     *
     * @param campaignId the ID of the campaign to filter bulk emails
     * @param pageable pagination information
     * @return a page of bulk email responses associated with the specified campaign
     */
    @Override
    public Page<BulkEmailResponse> getBulkEmailsByCampaign(String campaignId, Pageable pageable) {
        List<BulkEmailResponse> campaignEmails = bulkEmailStorage.values().stream()
                .filter(bulk -> campaignId.equals(bulk.getCampaignId()))
                .toList();
        return new PageImpl<>(campaignEmails, pageable, campaignEmails.size());
    }

    /**
     * Attempts to resend an email by its ID if it is eligible for retry.
     *
     * @param emailId the unique identifier of the email to retry
     * @return the response from the resend attempt, or null if the email does not exist or cannot be retried
     */
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

    /**
     * Cancels a scheduled email if it is in PENDING status.
     *
     * @param emailId the unique identifier of the scheduled email
     * @return true if the email was found and cancelled; false otherwise
     */
    @Override
    public boolean cancelScheduledEmail(String emailId) {
        EmailResponse email = emailStorage.get(emailId);
        if (email != null && email.getStatus() == EmailStatus.PENDING) {
            email.setStatus(EmailStatus.CANCELLED);
            return true;
        }
        return false;
    }

    /**
     * Marks a bulk email operation as completed, effectively canceling further processing if it is not already completed.
     *
     * @param bulkEmailId the unique identifier of the bulk email operation
     * @return true if the bulk email was found and successfully marked as completed; false if it was not found or already completed
     */
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
