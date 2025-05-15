package com.covolt.backend.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import com.covolt.backend.core.model.enums.BillingInterval;
import com.covolt.backend.core.model.enums.PlanStatus;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
@SuperBuilder(toBuilder = true) // toBuilder = true EKLENDİ

@Data
@EqualsAndHashCode(callSuper = true, exclude = {"companySubscriptions"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity { // BaseEntity id, createdAt, updatedAt, version

    @Column(nullable = false, unique = true)
    private String name; // Örn: "FREE_TRIAL", "BASIC_MONTHLY" - kodda kullanılacak anahtar

    @Column(name = "display_name", nullable = false)
    private String displayName; // Kullanıcıya gösterilecek - "Ücretsiz Deneme", "Temel Aylık Paket"

    @Lob
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private BillingInterval billingInterval; // Aylık, Yıllık, Tek Seferlik

    @Column(name = "trial_days")
    private Integer trialDays; // Bu plana özel deneme süresi (gündü?)

    // *** ÖZELLİKLER (FEATURES) KISMI ***
    // Plana dahil olan özellikleri kod olarak tutarız.
    @ElementCollection(fetch = FetchType.EAGER) // Plan çekildiğinde featureları da getir
    @CollectionTable(name = "plan_features", joinColumns = @JoinColumn(name = "plan_id", foreignKey = @ForeignKey(name = "fk_plan_features_plan_id")))
    @Column(name = "feature_code", nullable = false)
    @Builder.Default
    private Set<String> features = new HashSet<>(); // Örn: {"MAX_USERS_10", "API_ACCESS", "REALTIME_DASHBOARD"}

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = true; // Kullanıcı arayüzünden seçilebilir mi?

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlanStatus status = PlanStatus.ACTIVE; // Aktif, Arşivlenmiş

    // Relationship back to CompanySubscription (Inverse Side)
    @OneToMany(mappedBy = "plan", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<CompanySubscription> companySubscriptions = new HashSet<>();

    // Helper metot
    public boolean hasFeature(String featureCode) {
        return this.features != null && this.features.contains(featureCode);
    }
}