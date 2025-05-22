package com.covolt.backend.service.email;

import com.covolt.backend.service.email.dto.PasswordResetEmailDto;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

/**
 * Implementation of EmailService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Sends a password reset email to the specified user using a Thymeleaf template.
     *
     * @param emailDto data transfer object containing recipient and reset details
     * @return true if the email was sent successfully; false if sending failed or email functionality is disabled
     */
    @Override
    public boolean sendPasswordResetEmail(PasswordResetEmailDto emailDto) {
        if (!emailEnabled) {
            log.info("Email service is disabled. Skipping password reset email for: {}", emailDto.getEmail());
            return false;
        }

        try {
            log.debug("Sending password reset email to: {}", emailDto.getEmail());

            Context context = new Context();
            context.setVariable("fullName", emailDto.getFullName());
            context.setVariable("email", emailDto.getEmail());
            context.setVariable("resetUrl", emailDto.getResetUrl());
            context.setVariable("requestTime", emailDto.getRequestTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
            context.setVariable("expirationMinutes", emailDto.getExpirationMinutes());
            context.setVariable("companyName", emailDto.getCompanyName());
            context.setVariable("requestedBy", emailDto.getRequestedBy());

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(emailDto.getEmail());
            helper.setSubject("Şifre Sıfırlama Talebi - Covolt");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent successfully to: {}", emailDto.getEmail());
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", emailDto.getEmail(), e);
            return false;
        }
    }

    /**
     * Sends a welcome email with account details to a new user.
     *
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param companyName name of the company the user is joining
     * @param temporaryPassword temporary password assigned to the user
     * @return true if the email was sent successfully; false otherwise
     */
    @Override
    public boolean sendWelcomeEmail(String email, String fullName, String companyName, String temporaryPassword) {
        if (!emailEnabled) {
            log.info("Email service is disabled. Skipping welcome email for: {}", email);
            return false;
        }

        try {
            log.debug("Sending welcome email to: {}", email);

            String subject = "Covolt'a Hoş Geldiniz - Hesap Bilgileriniz";
            String htmlContent = buildWelcomeEmailContent(fullName, companyName, email, temporaryPassword);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent successfully to: {}", email);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to: {}", email, e);
            return false;
        }
    }

    /**
     * Sends an account transfer notification email to a user, informing them of their transfer from one company to another.
     *
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param fromCompanyName name of the previous company
     * @param toCompanyName name of the new company
     * @param transferReason reason for the transfer (may be null)
     * @return true if the email was sent successfully; false otherwise
     */
    @Override
    public boolean sendUserTransferNotification(String email, String fullName, String fromCompanyName, 
                                              String toCompanyName, String transferReason) {
        if (!emailEnabled) {
            log.info("Email service is disabled. Skipping transfer notification for: {}", email);
            return false;
        }

        try {
            log.debug("Sending transfer notification email to: {}", email);

            String subject = "Hesap Transfer Bildirimi - Covolt";
            String htmlContent = buildTransferNotificationContent(fullName, fromCompanyName, toCompanyName, transferReason);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Transfer notification email sent successfully to: {}", email);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send transfer notification email to: {}", email, e);
            return false;
        }
    }

    /**
     * Sends a role update notification email to a user with details about their new roles and the reason for the change.
     *
     * @param email recipient's email address
     * @param fullName recipient's full name
     * @param companyName name of the company associated with the user
     * @param newRoles description of the user's new roles
     * @param changeReason reason for the role update
     * @return true if the email was sent successfully; false otherwise
     */
    @Override
    public boolean sendRoleUpdateNotification(String email, String fullName, String companyName, 
                                            String newRoles, String changeReason) {
        if (!emailEnabled) {
            log.info("Email service is disabled. Skipping role update notification for: {}", email);
            return false;
        }

        try {
            log.debug("Sending role update notification email to: {}", email);

            String subject = "Rol Güncelleme Bildirimi - Covolt";
            String htmlContent = buildRoleUpdateNotificationContent(fullName, companyName, newRoles, changeReason);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(email);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Role update notification email sent successfully to: {}", email);
            return true;

        } catch (MessagingException e) {
            log.error("Failed to send role update notification email to: {}", email, e);
            return false;
        }
    }

    /**
     * Indicates whether email sending functionality is currently enabled.
     *
     * @return true if email sending is enabled; false otherwise
     */
    @Override
    public boolean isEmailEnabled() {
        return emailEnabled;
    }

    /**
     * Builds the HTML content for a welcome email, including personalized greeting, company name, user email, and temporary password.
     *
     * @param fullName the recipient's full name
     * @param companyName the name of the company where the account was created
     * @param email the recipient's email address
     * @param temporaryPassword the temporary password assigned to the user
     * @return the formatted HTML content for the welcome email
     */
    private String buildWelcomeEmailContent(String fullName, String companyName, String email, String temporaryPassword) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #007bff;">Covolt'a Hoş Geldiniz!</h2>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p><strong>%s</strong> şirketinde Covolt hesabınız oluşturulmuştur.</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3>Giriş Bilgileriniz:</h3>
                        <p><strong>Email:</strong> %s</p>
                        <p><strong>Geçici Şifre:</strong> %s</p>
                    </div>
                    <p><strong>Önemli:</strong> İlk girişinizde şifrenizi değiştirmeniz önerilir.</p>
                    <p>Herhangi bir sorunuz varsa, destek ekibimizle iletişime geçebilirsiniz.</p>
                    <hr>
                    <p style="font-size: 12px; color: #666;">Bu e-posta Covolt Platform Admin tarafından gönderilmiştir.</p>
                </div>
            </body>
            </html>
            """, fullName, companyName, email, temporaryPassword);
    }

    /**
     * Builds the HTML content for a user account transfer notification email.
     *
     * @param fullName the recipient's full name
     * @param fromCompanyName the name of the previous company
     * @param toCompanyName the name of the new company
     * @param transferReason the reason for the transfer, or "Belirtilmemiş" if null
     * @return the formatted HTML string for the transfer notification email
     */
    private String buildTransferNotificationContent(String fullName, String fromCompanyName, String toCompanyName, String transferReason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #007bff;">Hesap Transfer Bildirimi</h2>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p>Hesabınız başka bir şirkete transfer edilmiştir.</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3>Transfer Detayları:</h3>
                        <p><strong>Önceki Şirket:</strong> %s</p>
                        <p><strong>Yeni Şirket:</strong> %s</p>
                        <p><strong>Transfer Nedeni:</strong> %s</p>
                    </div>
                    <p>Yeni şirketinizde çalışmaya devam edebilirsiniz. Herhangi bir sorunuz varsa, destek ekibimizle iletişime geçebilirsiniz.</p>
                    <hr>
                    <p style="font-size: 12px; color: #666;">Bu e-posta Covolt Platform Admin tarafından gönderilmiştir.</p>
                </div>
            </body>
            </html>
            """, fullName, fromCompanyName, toCompanyName, transferReason != null ? transferReason : "Belirtilmemiş");
    }

    /**
     * Builds the HTML content for a role update notification email.
     *
     * @param fullName the recipient's full name
     * @param companyName the name of the company where the role update occurred
     * @param newRoles a description of the new roles assigned to the user
     * @param changeReason the reason for the role change, or "Belirtilmemiş" if not provided
     * @return a formatted HTML string for the role update notification email
     */
    private String buildRoleUpdateNotificationContent(String fullName, String companyName, String newRoles, String changeReason) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #007bff;">Rol Güncelleme Bildirimi</h2>
                    <p>Merhaba <strong>%s</strong>,</p>
                    <p><strong>%s</strong> şirketindeki rolleriniz güncellenmiştir.</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;">
                        <h3>Güncelleme Detayları:</h3>
                        <p><strong>Yeni Roller:</strong> %s</p>
                        <p><strong>Değişiklik Nedeni:</strong> %s</p>
                    </div>
                    <p>Yeni rollerinizle sistemi kullanmaya devam edebilirsiniz. Herhangi bir sorunuz varsa, destek ekibimizle iletişime geçebilirsiniz.</p>
                    <hr>
                    <p style="font-size: 12px; color: #666;">Bu e-posta Covolt Platform Admin tarafından gönderilmiştir.</p>
                </div>
            </body>
            </html>
            """, fullName, companyName, newRoles, changeReason != null ? changeReason : "Belirtilmemiş");
    }
}
