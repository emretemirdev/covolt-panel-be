package com.covolt.backend.exception;

import org.springframework.http.HttpStatus;
// Opsiyonel: İsterseniz `@ResponseStatus` ile varsayılan HTTP durumunu belirtebilirsiniz.
// @ResponseStatus(HttpStatus.FORBIDDEN) // 403 Yetkilendirme hatası
// @ResponseStatus(HttpStatus.UNAUTHORIZED) // 401 Kimlik Doğrulama hatası (Login sırasında daha uygun)
public class SubscriptionInactiveException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    // Sadece mesaj alan constructor
    public SubscriptionInactiveException(String message) {
        super(message);
    }

    // Mesaj ve neden (Throwable cause) alan constructor
    public SubscriptionInactiveException(String message, Throwable cause) {
        super(message, cause);
    }

    // Firma ID'si, Plan Adı gibi ek bilgiler de tutulabilir loglama/hata analizi için
    // private UUID companyId;
    // private String planName;
    // // Constructorları buna göre güncelleyebilirsiniz
}