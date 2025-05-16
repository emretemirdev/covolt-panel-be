package com.covolt.backend.core.security.service; // CustomUserDetailsService ile aynı pakette olabilir

import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User; // Spring'in User sınıfını genişletmek işleri kolaylaştırır

import java.util.*;

@Getter // Lombok ile tüm alanlar için getter metodları otomatik oluşur
public class CovoltUserDetails extends User { // Spring Security'nin User sınıfını genişletiyoruz

    private final UUID userId;
    private final UUID companyId;
    private final String companyName; // Opsiyonel, ihtiyaç duyulursa

    // Abonelikle ilgili alanlar
    private final UserSubscriptionStatus activeSubscriptionStatus;
    private final String activeSubscriptionPlanName;
    private final Set<String> activeSubscriptionFeatures; // Bu çok önemli: Planın sunduğu özellik kodları

    /**
     * Tam teşekküllü constructor.
     * CustomUserDetailsService bu constructor'ı kullanarak nesneyi dolduracak.
     */
    public CovoltUserDetails(
            String username, // Genellikle kullanıcının e-posta adresi
            String password, // Veritabanından gelen hashlenmiş şifre
            boolean enabled, // Kullanıcı aktif mi? (User entity'sinden)
            boolean accountNonExpired, // Hesap süresi doldu mu? (Şimdilik true varsayabiliriz)
            boolean credentialsNonExpired, // Şifre süresi doldu mu? (Şimdilik true varsayabiliriz)
            boolean accountNonLocked, // Hesap kilitli mi? (User entity'sinden)
            Collection<? extends GrantedAuthority> authorities, // Hem ROLLER hem de İZİNLER burada olacak
            UUID userId, // Bizim User entity'mizin ID'si
            UUID companyId, // Kullanıcının bağlı olduğu Company'nin ID'si
            String companyName, // Kullanıcının bağlı olduğu Company'nin adı
            UserSubscriptionStatus activeSubscriptionStatus, // Aktif aboneliğin durumu
            String activeSubscriptionPlanName, // Aktif aboneliğin plan adı
            Set<String> activeSubscriptionFeatures // Aktif abonelik planının özellik kodları
    ) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
        this.companyId = companyId;
        this.companyName = companyName;
        this.activeSubscriptionStatus = activeSubscriptionStatus;
        this.activeSubscriptionPlanName = activeSubscriptionPlanName;
        // Gelen set null ise boş bir set ata, ayrıca değiştirilemez yap (unmodifiableSet)
        this.activeSubscriptionFeatures = (activeSubscriptionFeatures != null) ? Collections.unmodifiableSet(new HashSet<>(activeSubscriptionFeatures)) : Collections.emptySet();
    }

    // İhtiyaç olursa, sadece temel bilgilerle veya daha az parametreyle başka constructor'lar eklenebilir.
    // Örneğin, abonelik bilgileri olmadan bir constructor:
    public CovoltUserDetails(
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            UUID userId,
            UUID companyId,
            String companyName,
            boolean enabled,
            boolean accountNonLocked
    ) {
        this(username, password, enabled, true, true, !accountNonLocked, authorities,
                userId, companyId, companyName,
                null, null, Collections.emptySet()); // Abonelik bilgileri null/boş
    }

    // Lombok @Getter sayesinde bu alanlar için getter metodları otomatik olarak oluşacak.
    // Örnek: getUserId(), getCompanyId(), getActiveSubscriptionFeatures()
}