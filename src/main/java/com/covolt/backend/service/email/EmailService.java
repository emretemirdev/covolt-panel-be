package com.covolt.backend.service.email;

import com.covolt.backend.service.email.dto.PasswordResetEmailDto;

/**
 * Legacy email service interface - will be deprecated
 * Use EmailFeatureService for new implementations
 */
@Deprecated
public interface EmailService {

    /**
 * Sends a password reset email to a user using the provided email data.
 *
 * @param emailDto the data required to compose the password reset email
 * @return true if the email was sent successfully; false otherwise
 */
    boolean sendPasswordResetEmail(PasswordResetEmailDto emailDto);

    /**
 * Sends a welcome email to a new user with their temporary password and company information.
 *
 * @param email the recipient's email address
 * @param fullName the full name of the new user
 * @param companyName the name of the company the user is joining
 * @param temporaryPassword the temporary password assigned to the user
 * @return true if the welcome email was sent successfully; false otherwise
 */
    boolean sendWelcomeEmail(String email, String fullName, String companyName, String temporaryPassword);

    /**
                                        * Sends a notification email to a user about their transfer from one company to another, including the reason for the transfer.
                                        *
                                        * @param email the recipient's email address
                                        * @param fullName the full name of the user being transferred
                                        * @param fromCompanyName the name of the company the user is leaving
                                        * @param toCompanyName the name of the company the user is joining
                                        * @param transferReason the reason for the user's transfer
                                        * @return true if the notification email was sent successfully; false otherwise
                                        */
    boolean sendUserTransferNotification(String email, String fullName, String fromCompanyName,
                                       String toCompanyName, String transferReason);

    /**
                                      * Sends a notification email to a user about updates to their roles within a company.
                                      *
                                      * @param email the recipient's email address
                                      * @param fullName the full name of the user
                                      * @param companyName the name of the company where the role update occurred
                                      * @param newRoles a description of the new roles assigned to the user
                                      * @param changeReason the reason for the role change
                                      * @return true if the notification email was sent successfully; false otherwise
                                      */
    boolean sendRoleUpdateNotification(String email, String fullName, String companyName,
                                     String newRoles, String changeReason);

    /**
 * Returns whether the email service is currently enabled.
 *
 * @return true if the email service is enabled; false otherwise
 */
    boolean isEmailEnabled();
}
