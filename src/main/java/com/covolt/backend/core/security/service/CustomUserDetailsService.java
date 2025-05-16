package com.covolt.backend.core.security.service;

import com.covolt.backend.core.model.CompanySubscription;
import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.service.CompanySubscriptionService; // BU SERVİSİ ENJEKTE EDECEĞİZ
// GitHub reponda CompanySubscriptionService 'com.covolt.backend.service' paketinde.
// Bu paketi modüler yapıya uygun olarak 'com.covolt.backend.modules.subscription.service' gibi bir yere taşımayı düşünebiliriz ileride.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
// Gerekli importları kontrol et

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;
    private final CompanySubscriptionService companySubscriptionService; // YENİ BAĞIMLILIK

    // Constructor'ı güncelle
    public CustomUserDetailsService(UserRepository userRepository,
                                    CompanySubscriptionService companySubscriptionService) {
        this.userRepository = userRepository;
        this.companySubscriptionService = companySubscriptionService;
    }

    @Override
    @Transactional(readOnly = true) // LAZY fetch edilecek ilişkiler için transaction gerekli
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Kullanıcı yükleme denemesi: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Kullanıcı bulunamadı: {}", email);
                    // Burada ResourceNotFoundException(ErrorCode.USER_003, email) kullanmak daha standart olurdu
                    return new UsernameNotFoundException("Kullanıcı bulunamadı: " + email);
                });

        logger.debug("Kullanıcı bulundu: {}. Roller, izinler ve abonelik bilgileri yükleniyor.", user.getUsername());

        if (user.getCompany() == null) {
            logger.error("KRİTİK HATA: Kullanıcı {} (ID: {}) bir firmaya bağlı değil!", user.getUsername(), user.getId());
            throw new UsernameNotFoundException("Kullanıcı firma bilgisi eksik: " + email);
        }

        // Yetkileri topla (Roller + İzinler)
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            for (Role role : user.getRoles()) { // User.roles LAZY olabilir, @Transactional gerekli
                authorities.add(new SimpleGrantedAuthority(role.getName())); // Örn: ROLE_PLATFORM_ADMIN
                if (role.getPermissions() != null && !role.getPermissions().isEmpty()) { // Role.permissions EAGER fetch edilmiş olmalı
                    for (Permission permission : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(permission.getName())); // Örn: MANAGE_ROLES
                    }
                }
            }
        } else {
            logger.warn("{} kullanıcısı için rol bulunamadı. Yetkisiz kalacak.", user.getUsername());
        }
        logger.debug("{} kullanıcı için {} adet yetki (rol/izin) yüklendi.", user.getUsername(), authorities.size());

        // Aktif abonelik bilgilerini çek
        UserSubscriptionStatus subscriptionStatus = null;
        String planName = null;
        Set<String> planFeatures = Collections.emptySet();

        // CompanySubscriptionService null olabilir eğer opsiyonel bir bağımlılık olarak tasarlanırsa,
        // ama bizim için kritik bir servis, bu yüzden null olmamalı.
        if (companySubscriptionService != null) {
            Optional<CompanySubscription> activeSubOpt = companySubscriptionService.getCurrentActiveSubscription(user.getCompany());
            if (activeSubOpt.isPresent()) {
                CompanySubscription activeSub = activeSubOpt.get();
                subscriptionStatus = activeSub.getStatus();
                if (activeSub.getPlan() != null) {
                    planName = activeSub.getPlan().getDisplayName(); // Veya getPlan().getName()
                    planFeatures = new HashSet<>(activeSub.getPlan().getFeatures());
                }
                logger.debug("Aktif abonelik bulundu: Plan='{}', Statü='{}', Özellik Sayısı={}", planName, subscriptionStatus, planFeatures.size());
            } else {
                logger.info("{} kullanıcısının firması (ID: {}) için aktif abonelik bulunamadı.", user.getUsername(), user.getCompany().getId());
            }
        } else {
            logger.error("CompanySubscriptionService enjekte edilmemiş! Abonelik bilgileri çekilemiyor.");
        }


        // CovoltUserDetails nesnesini oluştur
        return new CovoltUserDetails(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true, // accountNonExpired - Şimdilik true
                true, // credentialsNonExpired - Şimdilik true
                !user.isLocked(), // accountNonLocked
                authorities,
                user.getId(),
                user.getCompany().getId(),
                user.getCompany().getName(),
                subscriptionStatus,
                planName,
                planFeatures
        );
    }
}