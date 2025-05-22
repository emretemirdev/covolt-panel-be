package com.covolt.backend.features.email.enums;

/**
 * Enum for email sending status
 */
public enum EmailStatus {
    
    PENDING("Beklemede", "Email gönderilmeyi bekliyor"),
    SENDING("Gönderiliyor", "Email şu anda gönderiliyor"),
    SENT("Gönderildi", "Email başarıyla gönderildi"),
    DELIVERED("Teslim Edildi", "Email alıcıya teslim edildi"),
    FAILED("Başarısız", "Email gönderimi başarısız oldu"),
    BOUNCED("Geri Döndü", "Email geri döndü"),
    REJECTED("Reddedildi", "Email reddedildi"),
    CANCELLED("İptal Edildi", "Email gönderimi iptal edildi");
    
    private final String displayName;
    private final String description;
    
    EmailStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if email is in a final state (completed or failed)
     */
    public boolean isFinalState() {
        return this == SENT || this == DELIVERED || this == FAILED || 
               this == BOUNCED || this == REJECTED || this == CANCELLED;
    }
    
    /**
     * Check if email was successfully sent
     */
    public boolean isSuccessful() {
        return this == SENT || this == DELIVERED;
    }
    
    /**
     * Check if email failed
     */
    public boolean isFailed() {
        return this == FAILED || this == BOUNCED || this == REJECTED;
    }
}
