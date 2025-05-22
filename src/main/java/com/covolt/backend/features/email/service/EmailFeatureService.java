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
 * Sends a single email synchronously and returns the result.
 *
 * @param request the email request containing recipient, content, and metadata
 * @return the response with delivery status and tracking information
 */
    EmailResponse sendEmail(EmailRequest request);

    /**
 * Sends a single email asynchronously and returns a unique identifier for tracking its status.
 *
 * @param request the email request containing recipient, content, and metadata
 * @return a unique email ID for tracking the asynchronous send operation
 */
    String sendEmailAsync(EmailRequest request);

    /**
 * Schedules an email to be sent at a specified future time.
 *
 * @param request the email request containing recipient, content, and scheduled send time
 * @return an EmailResponse with details about the scheduled email, including status and scheduling information
 */
    EmailResponse scheduleEmail(EmailRequest request);

    // === BULK EMAIL OPERATIONS ===

    /**
 * Sends multiple emails in a single synchronous operation.
 *
 * @param request the bulk email request containing recipients and message details
 * @return a response with delivery statistics and tracking information
 */
    BulkEmailResponse sendBulkEmail(BulkEmailRequest request);

    /**
 * Initiates the sending of bulk emails asynchronously and returns a tracking ID.
 *
 * @param request the bulk email request containing recipients and message details
 * @return a unique ID for tracking the bulk email operation
 */
    String sendBulkEmailAsync(BulkEmailRequest request);

    // === QUICK SEND METHODS (for common use cases) ===

    /**
                                  * Sends a welcome email to a new user with their temporary password and onboarding details.
                                  *
                                  * @param email recipient's email address
                                  * @param fullName recipient's full name
                                  * @param companyName name of the company the user is joining
                                  * @param temporaryPassword temporary password assigned to the user
                                  * @param additionalData optional additional data to include in the email
                                  * @return response containing the status and tracking information of the sent email
                                  */
    EmailResponse sendWelcomeEmail(String email, String fullName, String companyName,
                                 String temporaryPassword, Map<String, Object> additionalData);

    /**
                                       * Sends an email verification message to the specified recipient.
                                       *
                                       * @param email recipient's email address
                                       * @param fullName recipient's full name
                                       * @param verificationToken unique token for email verification
                                       * @param verificationUrl URL the recipient should visit to verify their email
                                       * @return response containing the status and details of the sent email
                                       */
    EmailResponse sendEmailVerification(String email, String fullName, String verificationToken,
                                      String verificationUrl);

    /**
                                        * Sends a password reset email to the specified recipient.
                                        *
                                        * @param email the recipient's email address
                                        * @param fullName the recipient's full name
                                        * @param resetToken the token used to authorize the password reset
                                        * @param resetUrl the URL for resetting the password
                                        * @param expirationMinutes the number of minutes before the reset link expires
                                        * @return the response containing the status and details of the sent email
                                        */
    EmailResponse sendPasswordResetEmail(String email, String fullName, String resetToken,
                                       String resetUrl, int expirationMinutes);

    /**
                                                  * Sends a notification email informing the user that their password has been changed.
                                                  *
                                                  * @param email the recipient's email address
                                                  * @param fullName the full name of the user
                                                  * @param changedAt the date and time when the password was changed
                                                  * @param ipAddress the IP address from which the password change occurred
                                                  * @return the response containing the status and details of the sent email
                                                  */
    EmailResponse sendPasswordChangedNotification(String email, String fullName,
                                                 LocalDateTime changedAt, String ipAddress);

    /**
                                               * Sends an account locked notification email to the specified user.
                                               *
                                               * @param email the recipient's email address
                                               * @param fullName the recipient's full name
                                               * @param reason the reason the account was locked
                                               * @param lockedAt the timestamp when the account was locked
                                               * @return the response containing the status and details of the sent email
                                               */
    EmailResponse sendAccountLockedNotification(String email, String fullName, String reason,
                                              LocalDateTime lockedAt);

    /**
                                                  * Sends a notification email to inform a user about a company transfer.
                                                  *
                                                  * @param email recipient's email address
                                                  * @param fullName recipient's full name
                                                  * @param fromCompany name of the company the user is transferring from
                                                  * @param toCompany name of the company the user is transferring to
                                                  * @param reason explanation for the transfer
                                                  * @return response containing the status and details of the sent email
                                                  */
    EmailResponse sendCompanyTransferNotification(String email, String fullName,
                                                 String fromCompany, String toCompany, String reason);

    /**
                                             * Sends a notification email to inform a user about updates to their roles within a company.
                                             *
                                             * @param email the recipient's email address
                                             * @param fullName the recipient's full name
                                             * @param companyName the name of the company where the roles were updated
                                             * @param newRoles the list of new roles assigned to the user
                                             * @param reason the reason for the role update
                                             * @return the response containing the status and details of the sent email
                                             */
    EmailResponse sendRoleUpdatedNotification(String email, String fullName, String companyName,
                                            List<String> newRoles, String reason);

    /**
                                               * Sends a subscription-related notification email to a user.
                                               *
                                               * @param subscriptionType the type of subscription notification to send
                                               * @param email the recipient's email address
                                               * @param fullName the recipient's full name
                                               * @param companyName the name of the recipient's company
                                               * @param subscriptionData additional data relevant to the subscription notification
                                               * @return the response containing the status and details of the sent email
                                               */
    EmailResponse sendSubscriptionNotification(EmailType subscriptionType, String email,
                                              String fullName, String companyName,
                                              Map<String, Object> subscriptionData);

    /**
                                * Sends a login alert email to the specified user with details about the login event.
                                *
                                * @param email the recipient's email address
                                * @param fullName the recipient's full name
                                * @param loginTime the date and time of the login event
                                * @param ipAddress the IP address from which the login occurred
                                * @param userAgent the user agent string of the device used for login
                                * @param location the geographical location of the login attempt
                                * @return the response containing the status and details of the sent email
                                */
    EmailResponse sendLoginAlert(String email, String fullName, LocalDateTime loginTime,
                               String ipAddress, String userAgent, String location);

    // === EMAIL TRACKING AND MANAGEMENT ===

    /**
 * Retrieves an email by its unique identifier.
 *
 * @param emailId the unique ID of the email to retrieve
 * @return an Optional containing the EmailResponse if found, or empty if not found
 */
    Optional<EmailResponse> getEmailById(String emailId);

    /**
 * Retrieves a paginated list of emails associated with the specified user.
 *
 * @param userId the unique identifier of the user whose emails are to be retrieved
 * @param pageable pagination and sorting information
 * @return a page of email responses for the given user
 */
    Page<EmailResponse> getEmailsByUser(String userId, Pageable pageable);

    /**
 * Retrieves a paginated list of emails associated with the specified company.
 *
 * @param companyId the unique identifier of the company
 * @param pageable pagination and sorting information
 * @return a page of email responses for the given company
 */
    Page<EmailResponse> getEmailsByCompany(String companyId, Pageable pageable);

    /**
 * Retrieves a paginated list of emails filtered by the specified email type.
 *
 * @param emailType the type of emails to retrieve
 * @param pageable pagination and sorting information
 * @return a page of email responses matching the given type
 */
    Page<EmailResponse> getEmailsByType(EmailType emailType, Pageable pageable);

    /**
                                            * Retrieves a paginated list of emails sent within the specified date and time range.
                                            *
                                            * @param startDate the start of the date range (inclusive)
                                            * @param endDate the end of the date range (inclusive)
                                            * @param pageable pagination and sorting information
                                            * @return a page of email responses matching the date range criteria
                                            */
    Page<EmailResponse> getEmailsByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                           Pageable pageable);

    /**
 * Retrieves the details of a bulk email operation by its unique ID.
 *
 * @param bulkEmailId the unique identifier of the bulk email operation
 * @return an Optional containing the BulkEmailResponse if found, or empty if not found
 */
    Optional<BulkEmailResponse> getBulkEmailById(String bulkEmailId);

    /**
 * Retrieves a paginated list of bulk email operations associated with a specific campaign.
 *
 * @param campaignId the unique identifier of the campaign
 * @param pageable pagination and sorting information
 * @return a page of bulk email responses for the specified campaign
 */
    Page<BulkEmailResponse> getBulkEmailsByCampaign(String campaignId, Pageable pageable);

    // === EMAIL RETRY AND MANAGEMENT ===

    /**
 * Attempts to resend a previously failed email by its ID.
 *
 * @param emailId the unique identifier of the failed email to retry
 * @return the response containing the status and details of the retry attempt
 */
    EmailResponse retryEmail(String emailId);

    /**
 * Cancels a scheduled email by its ID.
 *
 * @param emailId the unique identifier of the scheduled email to cancel
 * @return true if the email was successfully canceled; false otherwise
 */
    boolean cancelScheduledEmail(String emailId);

    /****
 * Cancels a pending or ongoing bulk email operation identified by the given bulk email ID.
 *
 * @param bulkEmailId the unique identifier of the bulk email operation to cancel
 * @return true if the bulk email operation was successfully canceled; false otherwise
 */
    boolean cancelBulkEmail(String bulkEmailId);

    // === SYSTEM MANAGEMENT ===

    /**
 * Determines whether the email service is currently enabled.
 *
 * @return true if the email service is enabled; false otherwise
 */
    boolean isEmailServiceEnabled();

    /****
 * Retrieves the current health status details of the email service.
 *
 * @return a map containing health status information and related metrics
 */
    Map<String, Object> getServiceHealthStatus();
}
