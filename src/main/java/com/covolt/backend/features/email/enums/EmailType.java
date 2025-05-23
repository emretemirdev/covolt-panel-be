package com.covolt.backend.features.email.enums;

/**
 * Enum for different types of emails that can be sent
 */
public enum EmailType {
    
    // Authentication related emails
    WELCOME("welcome", "Covolt'a Hoş Geldiniz", "welcome/welcome-email"),
    EMAIL_VERIFICATION("verification", "Email Doğrulama", "verification/email-verification"),
    PASSWORD_RESET("password-reset", "Şifre Sıfırlama", "password-reset/password-reset"),
    PASSWORD_CHANGED("password-changed", "Şifre Değiştirildi", "notification/password-changed"),
    
    // Account related emails
    ACCOUNT_LOCKED("account-locked", "Hesap Kilitlendi", "notification/account-locked"),
    ACCOUNT_UNLOCKED("account-unlocked", "Hesap Kilidi Açıldı", "notification/account-unlocked"),
    ACCOUNT_SUSPENDED("account-suspended", "Hesap Askıya Alındı", "notification/account-suspended"),
    ACCOUNT_REACTIVATED("account-reactivated", "Hesap Yeniden Aktifleştirildi", "notification/account-reactivated"),
    
    // Company related emails
    COMPANY_TRANSFER("company-transfer", "Şirket Transferi", "notification/company-transfer"),
    ROLE_UPDATED("role-updated", "Rol Güncellendi", "notification/role-updated"),
    COMPANY_INVITATION("company-invitation", "Şirket Davetiyesi", "notification/company-invitation"),
    
    // Subscription related emails
    SUBSCRIPTION_ACTIVATED("subscription-activated", "Abonelik Aktifleştirildi", "notification/subscription-activated"),
    SUBSCRIPTION_EXPIRED("subscription-expired", "Abonelik Süresi Doldu", "notification/subscription-expired"),
    SUBSCRIPTION_RENEWED("subscription-renewed", "Abonelik Yenilendi", "notification/subscription-renewed"),
    TRIAL_STARTED("trial-started", "Deneme Süresi Başladı", "notification/trial-started"),
    TRIAL_ENDING("trial-ending", "Deneme Süresi Bitiyor", "notification/trial-ending"),
    
    // Security related emails
    LOGIN_ALERT("login-alert", "Giriş Uyarısı", "notification/login-alert"),
    SUSPICIOUS_ACTIVITY("suspicious-activity", "Şüpheli Aktivite", "notification/suspicious-activity"),
    TWO_FACTOR_CODE("two-factor-code", "İki Faktörlü Doğrulama", "verification/two-factor-code"),
    
    // System notifications
    MAINTENANCE_NOTICE("maintenance-notice", "Bakım Bildirimi", "notification/maintenance-notice"),
    SYSTEM_UPDATE("system-update", "Sistem Güncellemesi", "notification/system-update"),
    FEATURE_ANNOUNCEMENT("feature-announcement", "Yeni Özellik Duyurusu", "notification/feature-announcement"),
    
    // Admin notifications
    ADMIN_ALERT("admin-alert", "Admin Uyarısı", "notification/admin-alert"),
    USER_REPORT("user-report", "Kullanıcı Raporu", "notification/user-report"),
    SYSTEM_HEALTH("system-health", "Sistem Sağlığı", "notification/system-health");
    
    private final String code;
    private final String defaultSubject;
    private final String templatePath;
    
    EmailType(String code, String defaultSubject, String templatePath) {
        this.code = code;
        this.defaultSubject = defaultSubject;
        this.templatePath = templatePath;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDefaultSubject() {
        return defaultSubject;
    }
    
    public String getTemplatePath() {
        return templatePath;
    }
    
    /**
     * Get EmailType by code
     */
    public static EmailType fromCode(String code) {
        for (EmailType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown email type code: " + code);
    }
}
