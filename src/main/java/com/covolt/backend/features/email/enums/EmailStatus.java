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
    
    /**
     * Constructs an EmailStatus enum constant with the specified display name and description.
     *
     * @param displayName the short label for the email status
     * @param description a detailed explanation of the email status
     */
    EmailStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Returns the display name associated with this email status.
     *
     * @return the display name in Turkish for the current status
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Returns the detailed description of the email status in Turkish.
     *
     * @return the description associated with this status
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Determines whether the email status represents a terminal state.
     *
     * @return {@code true} if the status is SENT, DELIVERED, FAILED, BOUNCED, REJECTED, or CANCELLED; {@code false} otherwise
     */
    public boolean isFinalState() {
        return this == SENT || this == DELIVERED || this == FAILED || 
               this == BOUNCED || this == REJECTED || this == CANCELLED;
    }
    
    /**
     * Returns whether the email status indicates a successful send.
     *
     * @return {@code true} if the status is SENT or DELIVERED; {@code false} otherwise
     */
    public boolean isSuccessful() {
        return this == SENT || this == DELIVERED;
    }
    
    /**
     * Determines whether the email status represents a failure state.
     *
     * @return {@code true} if the status is FAILED, BOUNCED, or REJECTED; otherwise {@code false}
     */
    public boolean isFailed() {
        return this == FAILED || this == BOUNCED || this == REJECTED;
    }
}
