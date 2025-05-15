package com.covolt.backend.core.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
@SuperBuilder(toBuilder = true) // toBuilder = true EKLENDİ

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "company_subscriptions")
public class CompanySubscription extends BaseEntity { // BaseEntity id, createdAt, updatedAt, version

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false, foreignKey = @ForeignKey(name = "fk_comp_sub_company_id")) // Hangi firma? Null olamaz.
    private Company company;

    @ManyToOne(fetch = FetchType.EAGER, optional = true) // Plana ne zaman ihtiyaç duyulacak? (Çekilirken) EAGER yapalım. Null olabilir (örn: ömür boyu eski bir plan, silinmiş plan)
    @JoinColumn(name = "plan_id", nullable = true, foreignKey = @ForeignKey(name = "fk_comp_sub_plan_id")) // Hangi Plan?
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserSubscriptionStatus status; // Deneme, Aktif, Süresi Dolmuş vs.

    @Column(name = "start_date")
    private Instant startDate; // Abonelik başlangıcı (Deneme değilse)

    @Column(name = "end_date")
    private Instant endDate; // Abonelik bitişi (Deneme değilse)

    @Column(name = "trial_end_date")
    private Instant trialEndDate; // Sadece Deneme ise bitiş zamanı

    // Helper Metotlar (durum kontrolleri için)
    public boolean isTrialActive() {
        return UserSubscriptionStatus.TRIAL.equals(status) && trialEndDate != null && trialEndDate.isAfter(Instant.now());
    }

    public boolean isActiveSubscription() {
        return UserSubscriptionStatus.ACTIVE.equals(status) && (endDate == null || endDate.isAfter(Instant.now()));
    }

    public boolean hasActiveAccess() { // Sistemin sunduğu özelliklere erişebilir mi?
        return isTrialActive() || isActiveSubscription();
        // Belki CANCELED durumunda da dönem sonuna kadar aktif kalması gerekenler için
        // && !(UserSubscriptionStatus.CANCELLED.equals(status) && periodEndAfterCancel != null && periodEndAfterCancel.isBefore(Instant.now()))
    }

    public boolean isExpired() {
        return !hasActiveAccess(); // Active Access yoksa expired veya pasiftir diyebiliriz basitleştirerek
        // Daha spesifik expired checkleri entity statusu ve tarih kontrolüyle yapılır
       /*
       if (UserSubscriptionStatus.EXPIRED.equals(status)) return true;
       if (UserSubscriptionStatus.TRIAL.equals(status) && trialEndDate != null && trialEndDate.isBefore(Instant.now())) return true;
       if (UserSubscriptionStatus.ACTIVE.equals(status) && endDate != null && endDate.isBefore(Instant.now())) return true;
       return false;
       */
    }

    // Optional: Kullanıcının bu abonelik kaydı üzerinden belirli bir feature'a erişimi var mı?
    // Bu, SubscriptionPlan'ın feature listesine bakar.
    public boolean grantsFeature(String featureCode) {
        return hasActiveAccess() // Önce abonelik genel olarak aktif mi
                && this.plan != null // Sonra bir plana bağlı mı
                && this.plan.hasFeature(featureCode); // Sonra plan o feature'a sahip mi
    }
}