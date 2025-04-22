package com.covolt.backend.config;

import com.covolt.backend.model.Role;
import com.covolt.backend.model.User; // User entity'ni import et
import com.covolt.backend.repository.RoleRepository;
import com.covolt.backend.repository.UserRepository; // UserRepository'ni import et
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder; // PasswordEncoder'ı import et
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet; // HashSet import et
import java.util.Optional;
import java.util.Set;

@Configuration
@RequiredArgsConstructor // Final alanlar için constructor injeksiyonu
public class InitialDataLoader {

    private static final Logger logger = LoggerFactory.getLogger(InitialDataLoader.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; // UserRepository'yi enjekte et
    private final PasswordEncoder passwordEncoder; // PasswordEncoder'ı enjekte et

    @Bean
    public CommandLineRunner loadInitialData() {
        return args -> {
            logger.info("Başlangıç verileri yükleme işlemi başlatılıyor (Roller & Admin Kullanıcısı)...");
            try {
                initializeRoles(); // Roller oluşturulsun
                initializeDefaultAdminUser(); // Varsayılan admin kullanıcısı oluşturulsun

                logger.info("Başlangıç verileri başarıyla yüklendi.");
            } catch (Exception e) {
                logger.error("Başlangıç verileri yüklenirken kritik hata oluştu: {}", e.getMessage(), e);
                // throw new RuntimeException("Başlangıç veri yükleme işlemi başarısız oldu: " + e.getMessage()); // Hata fırlatıp uygulamanın başlamasını engelleyebilirsin
            }
        };
    }

    // --- Roller Oluşturma Metodları (Önceki Halinden) ---

    @Transactional
    protected void initializeRoles() {
        logger.debug("Rol yükleme işlemi başlatıldı");
        try {
            // createRoleWithRetry metodunuzun @Transactional ve Protected olduğunu varsayıyorum
            // createRoleWithRetry("ROLE_USER", "Standart kullanıcı rolü", 3); // Kendi metodunuzu kullanın
            // createRoleWithRetry("ROLE_ADMIN", "Yönetici rolü", 3);         // Kendi metodunuzu kullanın
            // ... Kendi createRoleWithRetry metodunuzu buraya veya yukarıya kopyalayın veya kullanın

            // VEYA basitleştirilmiş kontrol:
            createRoleIfNotFound("ROLE_USER", "Standart kullanıcı rolü");
            createRoleIfNotFound("ROLE_ADMIN", "Yönetici rolü");


        } catch (Exception e) {
            logger.error("Rol oluşturma işlemi başarısız oldu", e);
            throw e; // Hata devam etsin
        }
    }

    // Basit Rol Kontrol ve Oluşturma Yardımcı Metodu (Eğer retry mekanizmasını ayrı kullanıyorsanız)
    // Eğer createRoleWithRetry metodunuz transactional ise, bu yardımcı metota ihtiyacınız olmayabilir.
    // Ya initializeRoles içinde createRoleWithRetry çağrılarını kullanın YA DA aşağıdaki metodu
    // eğer daha basit kontrol istiyorsanız kullanın ve initializeRoles içinde çağırın.
    @Transactional // Rol yaratma işlemi kendi transactionında olmalı
    protected Role createRoleIfNotFound(String name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("'{}' rolü veritabanında bulunamadı, oluşturuluyor...", name);
                    Role newRole = Role.builder()
                            .name(name)
                            .description(description)
                            // BaseEntity alanları @Builder/Lombok tarafından veya JPA tarafından halledilir genelde
                            .build();
                    Role savedRole = roleRepository.save(newRole);
                    logger.info("'{}' rolü başarıyla oluşturuldu. ID: {}", name, savedRole.getId());
                    return savedRole;
                });
    }


    // --- Varsayılan Admin Kullanıcısı Oluşturma Metodu ---

    @Transactional // Admin kullanıcı oluşturma işlemi transaction içinde olsun
    protected void initializeDefaultAdminUser() {
        logger.debug("Varsayılan admin kullanıcısı kontrolü başlatıldı");

        // admin@covolt.com email adresiyle bir kullanıcı var mı kontrol et
        Optional<User> existingAdmin = userRepository.findByEmail("admin@covolt.com");

        if (existingAdmin.isPresent()) {
            logger.info("Varsayılan admin kullanıcısı 'admin@covolt.com' zaten mevcut. Username: {}", existingAdmin.get().getUsername());
        } else {
            logger.info("Varsayılan admin kullanıcısı 'admin@covolt.com' bulunamadı, oluşturuluyor...");

            // Gerekli rolleri (Admin ve User) veritabanından çek
            Optional<Role> adminRoleOpt = roleRepository.findByName("ROLE_ADMIN");
            Optional<Role> userRoleOpt = roleRepository.findByName("ROLE_USER");

            if (!adminRoleOpt.isPresent() || !userRoleOpt.isPresent()) {
                logger.error("Admin veya User varsayılan rolleri veritabanında bulunamadı. Varsayılan admin oluşturulamıyor.");
                // Rollere eklerken hata fırlatabilirsiniz
                // throw new RuntimeException("Default admin role not found!");
                return; // Rollere ihtiyaç olduğu için admin oluşturmayı atla
            }

            Set<Role> adminRoles = new HashSet<>();
            adminRoles.add(adminRoleOpt.get());
            adminRoles.add(userRoleOpt.get()); // Admin'ler genellikle normal kullanıcı yeteneklerine de sahiptir

            // Yeni admin kullanıcısı objesini oluştur
            // Şifre, güvenli bir varsayılan şifre olmalı ve hashlenmeli!
            String defaultAdminPassword = "AdminPassword123!"; // **UYARI: Bu gerçek bir uygulama için varsayılan şifre olmamalı, Üretim ortamında kullanılacaksa DEĞİŞTİRİLMELİ veya Random üretilmeli.**
            String hashedAdminPassword = passwordEncoder.encode(defaultAdminPassword);


            User adminUser = User.builder()
                    .email("admin@covolt.com")
                    .username("admin") // Veya başka bir varsayılan admin username'i
                    .password(hashedAdminPassword) // Hashlenmiş şifre
                    .fullName("Default Admin User")
                    .enabled(true) // Etkin olsun
                    .locked(false) // Kilitli olmasın
                    .failedLoginAttempts(0)
                    .roles(adminRoles) // Roller Setini ata
                    // Permissions field'ını boş olarak bıraktığınızda
                    // User entity'nizdeki default initialization (= new HashSet<>()) veya JPA'nın yükleme mekanizması bunu halletmeli.
                    // .permissions(new HashSet<>()) // Default init'e güvenmiyorsanız burada boş set atayabilirsiniz.
                    .build();

            // Admin kullanıcısını kaydet
            userRepository.save(adminUser);
            logger.info("Varsayılan admin kullanıcısı 'admin@covolt.com' başarıyla oluşturuldu.");
            // **ÇOK ÖNEMLİ GÜVENLİK UYARISI:** Üretim ortamında, varsayılan admin şifresi oluşturulduğunda,
            // BU ŞİFRE KONSOL LOGLARINDA GÖRÜNMEMELİ veya başka güvenli olmayan bir yerde saklanmamalıdır.
            // Sadece geliștirme/test ortamında kolaylık için loglayabilirsiniz VEYA uygulama ilk
            // başlarken environment variable/güvenli konfigürasyondan alınmalı ve LOGLANMAMALIDIR.
            // Veya uygulama başlarken rastgele bir şifre generate edilip secure bir şekilde (örn: e-posta ile)
            // admin kullanıcıya ulaştırılmalı ve ilk girişte değiştirmesi istenmelidir.
            // Şimdilik geliştirme kolaylığı için loglayabiliriz:
            logger.info("--- Varsayılan Admin Kullanıcısı Oluşturuldu ---");
            logger.info("E-posta: admin@covolt.com");
            logger.info("Kullanıcı Adı: admin");
            logger.info("Geçici Şifre (ÜRETİMDE LOGLAMAYIN!): {}", defaultAdminPassword); // ŞİFRE BURADA LOGLANIYOR! Dikkatli Olun.
            logger.info("---------------------------------------------");

        }
    }

    // Kendi createRoleWithRetry metodunuzu kullanıyorsanız yukarıdaki createRoleIfNotFound'u silin
    protected void createRoleWithRetry(String name, String description, int maxRetries) {
        // ... senin createRoleWithRetry metodunun kodu ...
        // loggerları ve logic'i barındıran retry metot
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                attempts++;
                logger.debug("'{}' rolü için {}. deneme", name, attempts);

                Optional<Role> existingRole = roleRepository.findByName(name);
                if (existingRole.isPresent()) {
                    logger.info("'{}' rolü zaten mevcut. ID: {}", name, existingRole.get().getId());
                    return;
                }

                Role newRole = Role.builder()
                        .name(name)
                        .description(description)
                        .build();

                Role savedRole = roleRepository.save(newRole);
                logger.info("'{}' rolü başarıyla oluşturuldu. ID: {}", name, savedRole.getId());
                return;

            } catch (DataIntegrityViolationException e) {
                logger.warn("'{}' rolü oluşturulurken veri bütünlüğü hatası: {}. Deneme: {}", name, e.getMessage(), attempts);
                if (attempts == maxRetries) {
                    logger.error("'{}' rolü oluşturulamadı, max retry denemesine ulaşıldı.", name);
                    throw e;
                }
                try {
                    Thread.sleep(1000); // Yeniden denemeden önce 1 saniye bekle
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("Rol oluşturma işlemi kesintiye uğradı.", ie);
                    throw new RuntimeException("Rol oluşturma işlemi kesintiye uğradı", ie);
                }
            } catch (Exception e) {
                logger.error("'{}' rolü oluşturulurken beklenmeyen hata: {}. Deneme: {}", name, e.getMessage(), attempts);
                throw e; // Beklenmeyen hata durumunda yeniden deneme yapmadan hatayı fırlat
            }
        }
    }


}