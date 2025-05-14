package com.covolt.backend.exception;

import org.springframework.http.HttpStatus;
// Opsiyonel: İsterseniz `@ResponseStatus` ile varsayılan HTTP durumunu belirtebilirsiniz.
// Ancak, bizim GlobalExceptionHandler'ımız Status kodunu manuel olarak belirlediği için bu zorunlu değil.
// @ResponseStatus(HttpStatus.NOT_FOUND) // 404
public class SubscriptionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Sadece mesaj alan constructor
    public SubscriptionNotFoundException(String message) {
        super(message);
    }

    // Mesaj ve neden (Throwable cause) alan constructor
    public SubscriptionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Abonelik ID'si gibi ek bilgiler de tutulabilir ihtiyaca göre
    // private UUID subscriptionId;
    // public UUID getSubscriptionId() { return subscriptionId; }
    // public SubscriptionNotFoundException(UUID subscriptionId, String message) { super(message); this.subscriptionId = subscriptionId; }
}