package com.covolt.backend.service.email;

import com.covolt.backend.service.email.dto.PasswordResetEmailDto;

/**
 * Legacy email service interface - will be deprecated
 * Use EmailFeatureService for new implementations
 */
@Deprecated
public interface EmailService {

    /**
     * Send password reset email to user
     *
     * @param emailDto Password reset email data
     * @return true if email sent successfully, false otherwise
     */
    boolean sendPasswordResetEmail(PasswordResetEmailDto emailDto);

    /**
     * Send welcome email to new user
     *
     * @param email User email
     * @param fullName User full name
     * @param companyName Company name
     * @param temporaryPassword Temporary password
     * @return true if email sent successfully, false otherwise
     */
    boolean sendWelcomeEmail(String email, String fullName, String companyName, String temporaryPassword);

    /**
     * Send user transfer notification email
     *
     * @param email User email
     * @param fullName User full name
     * @param fromCompanyName Source company name
     * @param toCompanyName Target company name
     * @param transferReason Transfer reason
     * @return true if email sent successfully, false otherwise
     */
    boolean sendUserTransferNotification(String email, String fullName, String fromCompanyName,
                                       String toCompanyName, String transferReason);

    /**
     * Send role update notification email
     *
     * @param email User email
     * @param fullName User full name
     * @param companyName Company name
     * @param newRoles New roles assigned
     * @param changeReason Change reason
     * @return true if email sent successfully, false otherwise
     */
    boolean sendRoleUpdateNotification(String email, String fullName, String companyName,
                                     String newRoles, String changeReason);

    /**
     * Check if email service is enabled
     *
     * @return true if email service is enabled
     */
    boolean isEmailEnabled();
}
