package com.covolt.backend.core.model.enums;

public enum UserSubscriptionStatus {
    TRIAL("Ücretsiz Deneme"),          // Firma ücretsiz deneme periyodunda
    ACTIVE("Aktif"),                  // Aktif, ödenmiş abonelik
    CANCELLED("İptal Edilmiş"),      // Kullanıcı/Firma tarafından iptal edildi (genellikle dönem sonuna kadar erişim devam eder)
    EXPIRED("Süresi Dolmuş"),         // Deneme veya ücretli aboneliğin süresi doldu
    PAST_DUE("Ödeme Zamanı Geçmiş"), // Ödeme alınamadı, servis kısıtlanabilir
    INACTIVE("Pasif");                // Yönetici tarafından kapatılmış veya başka bir nedenle pasif

    private final String description;

    UserSubscriptionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}