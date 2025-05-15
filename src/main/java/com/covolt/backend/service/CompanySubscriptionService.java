package com.covolt.backend.service;

// --- Spring & Diğer Importlar ---
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// --- Proje İçi Importlar ---
import com.covolt.backend.core.model.Company; // Company Entity
import com.covolt.backend.core.model.CompanySubscription; // Entity
import com.covolt.backend.core.model.SubscriptionPlan; // Entity
import com.covolt.backend.core.model.enums.UserSubscriptionStatus; // Status Enum
import com.covolt.backend.core.exception.ResourceCreationException; // Kaynak yaratma hatası

import com.covolt.backend.core.repository.CompanySubscriptionRepository; // Repository
import com.covolt.backend.core.repository.SubscriptionPlanRepository; // Repository


// --- Java Standart Importlar ---
import java.time.Instant; // Zaman
import java.time.temporal.ChronoUnit; // Zaman birimleri
import java.util.List; // Liste
import java.util.Optional; // Optional

@Service // Spring Service
@RequiredArgsConstructor // Final alanlar için constructor injection
public class CompanySubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(CompanySubscriptionService.class);

    // --- Bağımlılıklar ---
    private final CompanySubscriptionRepository companySubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;

    // Config'den alınacak deneme süresi (gün) ve varsayılan deneme planı adı
    @Value("${app.subscription.trialDays}")
    private int trialDays;
    @Value("${app.subscription.plan.freeTrialPlanName:FREE_TRIAL_PLAN}") // Varsayılan plan adı kodu
    private String freeTrialPlanName;


    /**
     * Yeni kayıt olan firma için ücretsiz deneme aboneliği başlatır.
     * Kayıt işlemi sırasında AuthServiceImpl tarafından çağrılır.
     *
     * @param company Deneme başlatılacak Firma Entity'si
     * @return Oluşturulan CompanySubscription Entity'si
     * @throws ResourceCreationException Deneme planı bulunamazsa veya aboneik yaratılırken hata olursa
     */
    @Transactional
    public CompanySubscription startTrial(Company company) {
        logger.info("Firma '{}' (ID: {}) için ücretsiz deneme aboneliği başlatılıyor.", company.getName(), company.getId());

        // Deneme bitiş tarihini hesapla
        Instant trialEndDate = Instant.now().plus(trialDays, ChronoUnit.DAYS);

        // 'FREE_TRIAL_PLAN' SubscriptionPlan entity'sini bul
        // Eğer sistemde FREE_TRIAL_PLAN yoksa bu bir konfigürasyon hatasıdır.
        SubscriptionPlan trialPlan = subscriptionPlanRepository.findByName(freeTrialPlanName)
                .orElseThrow(() -> {
                    logger.error("CRITICAL SİSTEM HATASI: '{}' adında Deneme Planı bulunamadı! InitialDataLoader'da tanımlanmalı.", freeTrialPlanName);
                    // Uygulamanın sağlıklı başlamaması gerekebilir eğer bu kadar kritik bir dependency ise.
                    return new ResourceCreationException("Deneme aboneliği başlatılamadı: Sistemde tanımlı deneme planı yok.");
                });

        // Firmanın zaten aktif/deneme/geçici bir aboneliği var mı kontrol et (Tek aktif abonelik kuralı için)
        // findTopByCompanyAndStatusInOrderByCreatedAtDesc kullanabiliriz
        Optional<CompanySubscription> existingActiveSubscription = companySubscriptionRepository.findTopByCompanyAndStatusInOrderByCreatedAtDesc(
                company,
                List.of(UserSubscriptionStatus.TRIAL, UserSubscriptionStatus.ACTIVE, UserSubscriptionStatus.PAST_DUE)
        );

        if (existingActiveSubscription.isPresent()) {
            logger.warn("Firma '{}' (ID: {}) zaten aktif/deneme bir aboneliğe sahip (Status: {}). Yeni deneme başlatılamaz.",
                    company.getName(), company.getId(), existingActiveSubscription.get().getStatus());
            // İsterseniz burada bir hata fırlatabilirsiniz veya mevcut aktif olanı döndürebilirsiniz.
            // Kullanıcının register olması engel değil, ama yeni bir deneme kaydı açamayız aynı firma için.
            // Şimdilik, zaten varsa mevcut aktif olanı döndürelim, register logic devam edebilir.
            return existingActiveSubscription.get();
        }


        // Yeni CompanySubscription kaydı oluştur (Durum: TRIAL)
        CompanySubscription companySubscription = CompanySubscription.builder()
                .company(company) // Firmayı ata
                .plan(trialPlan) // Deneme planını ata (null=true yapmıştık ama plana bağlamak daha iyi)
                .status(UserSubscriptionStatus.TRIAL) // Statüyü TRIAL yap
                .trialEndDate(trialEndDate) // Deneme bitiş tarihini set et
                // startDate, endDate gibi alanlar TRIAL için null kalacak.
                .build();

        // Kaydet
        CompanySubscription savedSubscription;
        try {
            savedSubscription = companySubscriptionRepository.save(companySubscription);
            logger.info("Firma '{}' (ID: {}) için ücretsiz deneme aboneliği başarıyla başlatıldı. Bitiş: {}",
                    company.getName(), company.getId(), trialEndDate);
        } catch (Exception e) {
            logger.error("Firma '{}' (ID: {}) için deneme aboneliği kaydedilirken hata oluştu: {}",
                    company.getName(), company.getId(), e.getMessage(), e);
            throw new ResourceCreationException("Deneme aboneliği oluşturulurken bir sorun oluştu: " + e.getMessage(), e);
        }


        return savedSubscription;
    }

    /**
     * Belirtilen firmanın şu anki aktif (TRIAL veya ACTIVE) aboneliğini getirir.
     * Abonelik tarihi geçmişse veya status EXPIRED/CANCELLED/INACTIVE ise dönmez.
     * CustomUserDetailsService veya Login/Refresh logic'i tarafından kullanılır.
     *
     * @param company Aboneliği çekilecek Firma Entity'si
     * @return Optional<CompanySubscription> Firmanın aktif abonelik kaydı varsa
     */
    @Transactional(readOnly = true) // Sadece okuma işlemi
    public Optional<CompanySubscription> getCurrentActiveSubscription(Company company) {
        logger.debug("Firma '{}' (ID: {}) için aktif abonelik kontrolü.", company.getName(), company.getId());

        // Firmanın TRIAL, ACTIVE veya PAST_DUE durumundaki aboneliklerini çek
        // createAt'e göre tersten sıralayıp en üsttekini al (en yeni aktif/deneme kaydı).
        Optional<CompanySubscription> latestRelevantSubscriptionOpt = companySubscriptionRepository.findTopByCompanyAndStatusInOrderByCreatedAtDesc(
                company,
                List.of(UserSubscriptionStatus.TRIAL, UserSubscriptionStatus.ACTIVE, UserSubscriptionStatus.PAST_DUE) // Bu statülerdeki abonelikler "aktif" olabilir.
        );

        if (latestRelevantSubscriptionOpt.isEmpty()) {
            logger.debug("Firma '{}' (ID: {}) için aktif/deneme abonelik kaydı bulunamadı.", company.getName(), company.getId());
            return Optional.empty(); // Hiç böyle bir kaydı yok
        }

        CompanySubscription subscription = latestRelevantSubscriptionOpt.get();

        // Kayıt bulundu, şimdi süresinin geçerli olup olmadığını veya statusun erişime izin verip vermediğini kontrol et.
        // CompanySubscription entity'sindeki hasActiveAccess() helper metodunu kullanacağız.
        if (subscription.hasActiveAccess()) {
            logger.debug("Firma '{}' (ID: {}) aktif bir aboneliğe sahip (Status: {}, Plan: {}, Bitiş: {})",
                    company.getName(), company.getId(), subscription.getStatus(),
                    subscription.getPlan() != null ? subscription.getPlan().getName() : "N/A",
                    subscription.isTrialActive() ? subscription.getTrialEndDate() : subscription.getEndDate());
            return Optional.of(subscription); // Aktif ve geçerli bir abonelik bulundu
        } else {
            // Kayıt bulundu ancak süresi dolmuş veya statüsü pasif.
            logger.debug("Firma '{}' (ID: {}) abonelik kaydı bulundu ancak aktif değil/süresi dolmuş (Status: {}, Bitiş: {}).",
                    company.getName(), company.getId(), subscription.getStatus(),
                    subscription.isTrialActive() ? subscription.getTrialEndDate() : subscription.getEndDate());

            // OTOMATİK STATUS GÜNCELLEMESİ: Eğer kaydın statüsü hala TRIAL/ACTIVE ama süresi dolmuşsa
            // (hasActiveAccess false döneceği halde status hala ACTIVE/TRIAL ise)
            // bu kaydın statusunu burada markExpired metoduyla otomatik EXPIRED'e çekebiliriz.
            // Bu, Zamanlanmış görevdeki iş yükünü bir miktar azaltır ve login/erişim sırasında da güncellemeyi tetikler.
            // Bu metodu başka bir yerden (Scheduled Task) de çağıracağımız için,
            // Logic'in tekrarını önlemek amacıyla sadece kontrol yapıp, güncelleme işlemini
            // ayrı bir metotta veya Scheduled Task'in sorumluluğunda bırakmak daha temiz olabilir.
            // Şimdilik sadece varlığını ve aktif olmadığını loglayıp Optional.empty() dönelim.

            return Optional.empty(); // Süresi dolmuş veya aktif olmayan bir abonelik bulundu, aktif olarak kabul etmiyoruz
        }
    }

    /**
     * Aboneliğin durumunu EXPIRED olarak günceller.
     * Genellikle Zamanlanmış görev veya aboneliğin süresi dolduğunda login/refresh sırasında çağrılır.
     * @param subscription Durumu güncellenecek abonelik kaydı
     * @return Güncellenmiş CompanySubscription entity'si
     */
    @Transactional
    public CompanySubscription markExpired(CompanySubscription subscription) {
        // Status zaten EXPIRED, CANCELLED (süresi dolmuşsa), veya INACTIVE ise güncellemeye gerek olmayabilir.
        if (subscription.getStatus() == UserSubscriptionStatus.EXPIRED ||
                subscription.getStatus() == UserSubscriptionStatus.CANCELLED || // Eğer iptal edildi ve süresi dolduysa EXPIRED'a çekmiyoruz
                subscription.getStatus() == UserSubscriptionStatus.INACTIVE) { // veya INACTIVE
            logger.debug("Abonelik {} (Firma: {}) zaten bitiş durumunda. Güncellemeye gerek yok. Statü: {}",
                    subscription.getId(), subscription.getCompany().getName(), subscription.getStatus());
            return subscription;
        }

        // Eğer status TRIAL veya ACTIVE ise VE süresi gerçekten dolmuşsa
        if ((UserSubscriptionStatus.TRIAL.equals(subscription.getStatus()) && subscription.getTrialEndDate() != null && subscription.getTrialEndDate().isBefore(Instant.now())) ||
                (UserSubscriptionStatus.ACTIVE.equals(subscription.getStatus()) && subscription.getEndDate() != null && subscription.getEndDate().isBefore(Instant.now()))) {

            subscription.setStatus(UserSubscriptionStatus.EXPIRED); // Statüyü EXPIRED yap
            // subscription.setEndDate(Instant.now()); // EXPIRED olma zamanını set etmek isteyebilirsiniz

            CompanySubscription updatedSubscription = companySubscriptionRepository.save(subscription);
            logger.info("Abonelik {} (Firma: {}) süresi dolmuş olarak işaretlendi. Yeni Statü: {}",
                    updatedSubscription.getId(), updatedSubscription.getCompany().getName(), updatedSubscription.getStatus());

            // Optional: Firmanın varsayılan ücretsiz planın yetkilerine dönmesi gerekiyorsa,
            // bu firmadaki kullanıcıların rol/yetkilerini güncellemek veya Security context yenilemek gerekebilir.
            // Genellikle yetkilendirme(@PreAuthorize) anlık CompanySubscription durumuna baktığı için
            // explicit bir yetki güncellemesi gerekmez, sadece getCurrentActiveSubscription'dan boş gelmesi yeterli olur.

            return updatedSubscription;

        } else {
            logger.debug("Abonelik {} (Firma: {}) süresi henüz dolmamış veya uygun durumda değil (Statü: {}). EXPIRED'a çekilemez.",
                    subscription.getId(), subscription.getCompany().getName(), subscription.getStatus());
            return subscription; // Süresi henüz dolmadı, güncelleme yapma
        }
    }

    // Optional: activateSubscription, cancelSubscription, changePlan gibi metodlar buraya eklenecek.
    // public CompanySubscription activateSubscription(...) {...}
    // public CompanySubscription cancelSubscription(...) {...}

    // Optional: Firma SubscriptionStatus'ını doğrudan güncellemek için
    // @Transactional
    // public CompanySubscription updateSubscriptionStatus(CompanySubscription subscription, UserSubscriptionStatus newStatus) {
    //     subscription.setStatus(newStatus);
    //     return companySubscriptionRepository.save(subscription);
    // }

}