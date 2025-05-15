package com.covolt.backend.core.model.enums;

public enum BillingInterval {
    MONTHLY("Aylık"),
    ANNUALLY("Yıllık"),
    CUSTOM("Özel"); // Özel anlaşmalar için

    private final String description;

    BillingInterval(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}