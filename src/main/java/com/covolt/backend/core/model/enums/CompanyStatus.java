package com.covolt.backend.core.model.enums;

public enum CompanyStatus {
    PENDING_VERIFICATION("Onay Bekliyor"), // Yeni kayıtlı firma, admin onayı bekliyor olabilir
    ACTIVE("Aktif"),                   // Firma aktif ve sistemi kullanabilir
    SUSPENDED("Askıya Alındı"),       // Ödeme sorunu veya kural ihlali nedeniyle askıya alındı
    DELETED("Silinmiş");               // Kalıcı olarak silindi


    private final String description;

    CompanyStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}