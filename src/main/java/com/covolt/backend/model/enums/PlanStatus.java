package com.covolt.backend.model.enums;

public enum PlanStatus {
    ACTIVE("Aktif"),     // Plan şu anda aktif ve kullanıcılar/firmalar abone olabilir
    ARCHIVED("Arşivlenmiş"); // Plan artık yeni abonelikler için seçilemez, ancak mevcut aboneler (isterse) kullanmaya devam edebilir (genellikle plan değiştirme/yenileme engellenir)

    private final String description;

    PlanStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}