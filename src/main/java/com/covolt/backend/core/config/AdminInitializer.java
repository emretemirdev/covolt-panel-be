package com.covolt.backend.core.config; // Veya seçtiğin başka bir paket

import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import com.covolt.backend.core.repository.CompanyRepository;
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.core.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    private static final String PLATFORM_ADMIN_ROLE_NAME = "ROLE_PLATFORM_ADMIN";
    private static final String SYSTEM_COMPANY_NAME = "COVOLT SYSTEM TEKNOLOJİ";

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializePlatformAdminSetup() {
        logger.info("Platform admin kurulumu kontrol ediliyor...");

        Company systemCompany = companyRepository.findByNameIgnoreCase(SYSTEM_COMPANY_NAME)
                .orElseGet(() -> {
                    logger.info("'{}' adlı sistem firması oluşturuluyor...", SYSTEM_COMPANY_NAME);
                    return companyRepository.save(Company.builder()
                            .name(SYSTEM_COMPANY_NAME)
                            .status(CompanyStatus.ACTIVE)
                            .type(CompanyType.CORPORATION)
                            .contactEmail(environment.getProperty("app.default.admin.email", "admin@covolt.com"))
                            .build());
                });

        Role platformAdminRole = roleRepository.findByName(PLATFORM_ADMIN_ROLE_NAME)
                .orElseGet(() -> {
                    logger.info("'{}' rolü oluşturuluyor...", PLATFORM_ADMIN_ROLE_NAME);
                    return roleRepository.save(Role.builder()
                            .name(PLATFORM_ADMIN_ROLE_NAME)
                            .description("Tüm platform yetkilerine sahip yönetici rolü.")
                            .permissions(new HashSet<>())
                            .build());
                });

        Set<Permission> platformPermissions = new HashSet<>();
        platformPermissions.add(createPermissionIfNotExists("MANAGE_ROLES", "Rolleri yönetme"));
        platformPermissions.add(createPermissionIfNotExists("MANAGE_PERMISSIONS", "İzinleri yönetme"));
        platformPermissions.add(createPermissionIfNotExists("MANAGE_SUBSCRIPTION_PLANS", "Abonelik planlarını yönetme"));
        // ... Diğer temel platform admin izinleri eklenebilir

        boolean roleNeedsUpdate = false;
        for (Permission perm : platformPermissions) {
            if (platformAdminRole.getPermissions().add(perm)) {
                roleNeedsUpdate = true;
            }
        }
        if (roleNeedsUpdate) {
            roleRepository.save(platformAdminRole);
            logger.info("'{}' rolüne temel platform izinleri atandı.", PLATFORM_ADMIN_ROLE_NAME);
        }

        String adminEmail = environment.getProperty("app.default.admin.email", "platformadmin@covolt.com");
        String adminPassword = environment.getProperty("app.default.admin.password", "CovoltAdmin123!");

        if (adminEmail == null || adminPassword == null) {
            logger.error("UYARI: Platform admin email/şifre ayarları (app.default.admin.email, app.default.admin.password) bulunamadı!");
            return;
        }

        if (!userRepository.findByEmail(adminEmail).isPresent()) {
            logger.info("'{}' email adresine sahip platform admin kullanıcısı oluşturuluyor...", adminEmail);
            if ("CovoltAdmin123!".equals(adminPassword) || adminPassword.length() < 12) {
                logger.warn("UYARI: Varsayılan veya güvensiz bir platform admin şifresi kullanılıyor. Lütfen ilk login sonrası değiştirin veya daha güvenli bir şifre ayarlayın!");
            }
            User platformAdminUser = User.builder()
                    .email(adminEmail)
                    .username(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fullName("Platform Yöneticisi")
                    .enabled(true)
                    .locked(false)
                    .company(systemCompany)
                    .roles(Set.of(platformAdminRole))
                    .build();
            userRepository.save(platformAdminUser);
            logger.info("'{}' platform admin kullanıcısı başarıyla oluşturuldu ve '{}' firmasına bağlandı.", adminEmail, systemCompany.getName());
        } else {
            logger.info("'{}' email adresine sahip platform admin kullanıcısı zaten mevcut.", adminEmail);
        }
        logger.info("Platform admin kurulumu tamamlandı.");
    }

    private Permission createPermissionIfNotExists(String name, String description) {
        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("'{}' izni oluşturuluyor.", name);
                    return permissionRepository.save(Permission.builder().name(name).description(description).build());
                });
    }
}