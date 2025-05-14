package com.covolt.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
// Spring Query Importları varsa eklenebilir
// import org.springframework.data.jpa.repository.Query;

import com.covolt.backend.model.CompanySubscription;
import com.covolt.backend.model.Company; // İlişkili Entity
import com.covolt.backend.model.enums.UserSubscriptionStatus; // Enum

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID; // PK tipi UUID ise

@Repository
public interface CompanySubscriptionRepository extends JpaRepository<CompanySubscription, UUID> { // PK tipini BaseEntity ile eşleştir

    // Belirli bir firmanın, belirtilen statülerdeki abonelik kayıtlarını (genellikle aktif/deneme)
    // oluşturulma tarihine göre tersten sırala ve sadece en üsttekini al.
    // Bu, bir firmanın *son* veya *geçerli* aktif/deneme aboneliğini bulmak için yaygın bir yöntemdir.
    // Genellikle firmanın tek bir aktif/deneme kaydı olur, ancak DB seviyesinde unique constraint yoksa veya tarih yönetimi hassas değilse birden fazla gelebilir.
    Optional<CompanySubscription> findTopByCompanyAndStatusInOrderByCreatedAtDesc(Company company, List<UserSubscriptionStatus> statuses);

    // Süresi dolmuş deneme aboneliklerini bul (Zamanlanmış görev için)
    List<CompanySubscription> findByStatusAndTrialEndDateBefore(UserSubscriptionStatus status, Instant trialEndDate);

    // Süresi dolmuş aktif abonelikleri bul (Zamanlanmış görev için)
    List<CompanySubscription> findByStatusAndEndDateBefore(UserSubscriptionStatus status, Instant endDate);


    // İstenirse firmaya ait tüm abonelik geçmişini getiren metot eklenebilir
    // List<CompanySubscription> findByCompanyOrderByCreatedAtDesc(Company company);

}