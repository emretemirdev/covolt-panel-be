package com.covolt.backend.service.impl;

// --- Spring Framework & Security Importları ---
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transaction yönetimi için
import org.springframework.security.core.GrantedAuthority; // Authority listesi için

// --- Proje İçi Importlar ---
import com.covolt.backend.dto.auth.AuthResponse;
import com.covolt.backend.dto.auth.LoginRequest;
import com.covolt.backend.dto.auth.RefreshTokenRequest;
import com.covolt.backend.dto.auth.RegisterRequest;
import com.covolt.backend.exception.DuplicateRegistrationException;
import com.covolt.backend.exception.ResourceCreationException; // Yeni özel hata sınıfı
import com.covolt.backend.exception.TokenRefreshException;
import com.covolt.backend.exception.SubscriptionInactiveException; // Abonelik için (sonra)

import com.covolt.backend.model.Company; // Yeni Company Entity
import com.covolt.backend.model.RefreshToken;
import com.covolt.backend.model.Role;
import com.covolt.backend.model.User;
import com.covolt.backend.model.enums.CompanyStatus; // CompanyStatus Enum
// import com.covolt.backend.model.enums.UserSubscriptionStatus; // Abonelik Enum (sonra)

import com.covolt.backend.repository.CompanyRepository; // Yeni Company Repository
import com.covolt.backend.repository.RoleRepository;
import com.covolt.backend.repository.UserRepository;
import com.covolt.backend.security.jwt.JwtService;
import com.covolt.backend.security.service.CustomUserDetailsService;
import com.covolt.backend.service.AuthService;
import com.covolt.backend.service.RefreshTokenService;
// import com.covolt.backend.service.CompanySubscriptionService; // Yeni Abonelik Servisi (sonra)

// --- Java Standart Importlar ---
import java.time.Instant; // Zaman için
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // Stream işlemleri için


import org.slf4j.Logger; // Loglama için
import org.slf4j.LoggerFactory; // Loglama için


@Service // Spring Service bileşeni
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class); // Logger tanımlama

    // --- Bağımlılıklar (Constructor Injection) ---
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository; // Yeni Company Repository
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService;
    // private final CompanySubscriptionService companySubscriptionService; // Abonelik servisi sonra eklenecek


    // --- Constructor ---
    // Spring Boot bu constructor'daki bağımlılıkları otomatik enjekte eder
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           CompanyRepository companyRepository, // Yeni bağımlılık eklendi
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           CustomUserDetailsService userDetailsService
            /* , CompanySubscriptionService companySubscriptionService */) { // Abonelik servisi için yer tutuldu
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository; // Yeni bağımlılık atandı
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        // this.companySubscriptionService = companySubscriptionService; // Sonra atanacak
    }


    // --- Register Metodu ---
    @Override
    @Transactional // Bu metot içindeki tüm veritabanı işlemleri tek bir transaction içinde yönetilir
    public AuthResponse register(RegisterRequest request) {
        logger.info("Yeni kullanıcı ve firma kaydı başlatıldı. Email: {}, Firma Adı: {}", request.getEmail(), request.getCompanyName());

        // 1. Kullanıcı (Email ve Username) Benzersizliğini Kontrol Et
        if (userRepository.existsByEmailOrUsername(request.getEmail(), request.getUsername())) {
            logger.warn("Kayıt reddedildi: Email ({}) veya Kullanıcı adı ({}) zaten kullanımda.", request.getEmail(), request.getUsername());
            throw new DuplicateRegistrationException("Bu e-posta veya kullanıcı adı zaten kayıtlı.");
        }

        // 2. Yeni Firma/Kuruluş Oluşturma
        // Karar: Firma adı unique olmak zorunda değil, her kayıtla yeni firma oluşturulacak.
        // Eğer firma adı unique olacaksa, burada existsByNameIgnoreCase kontrolü yapılmalıydı.
        Company newCompany = Company.builder()
                .name(request.getCompanyName()) // RegisterRequest DTO'dan firma adını al
                // Opsiyonel: Diğer Company alanları (type, address, contactEmail) da RegisterRequest DTO'suna
                // eklenirse veya varsayılan değerler verilirse burada set edilebilir.
                // Şimdilik sadece name ve default status ile oluşturuyoruz.
                .status(CompanyStatus.ACTIVE) // Varsayılan statü (PENDING_VERIFICATION da olabilir)
                // İlişkiler (@OneToMany users, companySubscriptions) @Builder.Default ile initialize ediliyor.
                .build();

        Company savedCompany;
        try {
            savedCompany = companyRepository.save(newCompany);
            logger.info("Yeni firma/kuruluş başarıyla oluşturuldu: Adı='{}', ID='{}'", savedCompany.getName(), savedCompany.getId());
        } catch (Exception e) {
            // Örneğin DataIntegrityViolationException (eğer DB'de unique kısıtlama varsa) veya başka DB hataları
            logger.error("Firma/kuruluş ('{}') oluşturulurken kritik hata: {}", request.getCompanyName(), e.getMessage(), e);
            throw new ResourceCreationException("Kuruluş oluşturulurken bir hata meydana geldi: " + e.getMessage(), e);
        }

        // 3. Yeni Kullanıcıyı Oluştur ve Bu Yeni Firmaya Bağla
        User newUser = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // Şifreyi şifrele (hashle)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true) // Varsayılan: aktif. E-posta onayı gelirse bu false ile başlayabilir.
                .locked(false)
                .failedLoginAttempts(0)
                .company(savedCompany) // *** YENİ: Kullanıcıyı yeni oluşturulan firmaya ata ***
                // roles User entity'sinde @Builder.Default ile new HashSet<>() olarak initialize ediliyor.
                // permissions User entity'sinden kaldırıldı.
                .build();

        // Opsiyonel: Eğer yeni oluşturulan Company'nin ownerUserIdentifier'ı yeni User ise, Company'yi güncelle.
        // Örneğin, kaydolan kullanıcının username'ini owner olarak set et.
        // if (savedCompany.getOwnerUserIdentifier() == null) { // Veya başka bir koşul
        //     savedCompany.setOwnerUserIdentifier(savedUser.getUsername()); // Veya ID'si
        //     companyRepository.save(savedCompany); // Tekrar kaydetmek gerekebilir
        // }


        // 4. Varsayılan Rol ("ROLE_USER") Atama
        // Kullanıcı ilk kaydolduğunda sadece standart USER rolü atanacak.
        // Eğer COMPANY_ADMIN rolü de atanacaksa, o rolü de bulup roles setine eklememiz gerekir.
        Set<Role> roles = new HashSet<>();
        roleRepository.findByName("ROLE_USER")
                .ifPresentOrElse(roles::add,
                        () -> {
                            logger.error("'ROLE_USER' rolü bulunamadı. Sistem konfigürasyon hatası!");
                            // Bu durum uygulamanın başlamadan önce çözülmeli (InitialDataLoader ile)
                            throw new RuntimeException("Sistem hatası: Varsayılan kullanıcı rolü tanımsız.");
                        });
        // Örneğin COMPANY_ADMIN rolü de atanacaksa (opsiyonel):
        // roleRepository.findByName("ROLE_COMPANY_ADMIN")
        //         .ifPresentOrElse(roles::add,
        //              () -> logger.warn("'ROLE_COMPANY_ADMIN' rolü bulunamadı. InitialDataLoader'da tanımlanmalı."));

        newUser.setRoles(roles); // Kullanıcı entity'sine rolleri set et

        // 5. Kullanıcıyı Kaydetme
        User savedUser;
        try {
            savedUser = userRepository.save(newUser);
            logger.info("Yeni kullanıcı ('{}') başarıyla oluşturuldu ve firmaya (ID: {}) bağlandı.", savedUser.getEmail(), savedCompany.getId());
        } catch (Exception e) {
            // DataIntegrityViolationException (email/username unique constraint) veya diğer DB hataları
            logger.error("Kullanıcı kaydedilirken kritik hata: {}. İstek: {}", e.getMessage(), request.getEmail(), e);
            // Eğer burada bir hata olursa, yukarıda oluşturulan Company kaydını geri almak (rollback) gerekir.
            // @Transactional anotasyonu tüm bu metodu tek bir transaction olarak yönettiği için
            // bu userRepository.save başarısız olursa, companyRepository.save de geri alınır (eğer exception uygunsa).
            throw new ResourceCreationException("Kullanıcı oluşturulurken bir hata meydana geldi: " + e.getMessage(), e);
        }

        // 6. (SONRAKİ ADIM) Yeni Oluşturulan Firma İçin Deneme Aboneliği Başlat
        // Firma ve kullanıcı kaydedildikten sonra, bu firmaya bir deneme aboneliği oluşturulacak.
        // Bunun için CompanySubscriptionService.startTrial metodunu çağıracağız.
        // companySubscriptionService.startTrial(savedCompany, defaultTrialPlan); // defaultTrialPlan da SubscriptionPlanRepository'den çekilmeli

        // 7. Tokenları Üret ve Dön
        // CustomUserDetailsService, UserDetails objesini oluştururken kullanıcının
        // Firma ve (ileride) Abonelik bilgilerini de içerecek şekilde güncellenecek.
        // Şimdilik sadece rolleri ve temel kullanıcı bilgilerini alıyor.
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());

        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(savedUser.getEmail());

        Instant accessTokenExpiration = jwtService.extractExpiration(accessToken).toInstant();

        // 8. Yanıtı Hazırla (AuthResponse DTO)
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(accessTokenExpiration)
                // İsteğe bağlı olarak AuthResponse'a kullanıcı ve firma ID/adı gibi bilgileri eklenebilir.
                // Frontend'in login sonrası kullanıcının hangi firmaya ait olduğunu bilmesi gerekebilir.
                // .userId(savedUser.getId().toString()) // User PK UUID ise toString() gerekli
                // .companyId(savedCompany.getId().toString())
                // .companyName(savedCompany.getName())
                .build();

        logger.info("Kullanıcı kaydı ve ilk tokenlar başarıyla oluşturuldu: Email: {}", savedUser.getEmail());
        return authResponse;
    }

    // --- Login Metodu ---
    @Override
    @Transactional(readOnly = true) // Login sırasında veri güncellemesi (failed attempts, last login) olabilir.
    // Eğer bu güncellemeler olacaksa readOnly = false olmalı!
    // Eğer sadece abonelik kontrolü için abonelik servisi çağrılacaksa
    // ve o servis transactionalsa, burası readOnly kalabilir veya readOnly=false olur.
    public AuthResponse login(LoginRequest request) {
        logger.info("Kullanıcı giriş denemesi: Email: {}", request.getEmail());

        Authentication authentication;
        try {
            // Spring Security'nin Authentication Manager'ını kullanarak kimlik doğrula
            // Bu aşamada CustomUserDetailsService çağırılır ve kullanıcı bulunur, şifre kontrol edilir.
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(), // CustomUserDetailsService email ile yüklüyor
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            // Kimlik doğrulama başarısızsa (e-posta/şifre yanlış, hesap kilitli/disable vb.)
            // Başarısız giriş deneme sayısını artırma gibi logic burada VEYA CustomUserDetailsService'de yapılabilir.
            // GlobalExceptionHandler bu hatayı yakalayacak ve genel bir 401 yanıtı dönecek.
            logger.warn("Kimlik doğrulama başarısız: Email: {}", request.getEmail());
            // Optional: Başarısız deneme sayısını artırma logic'i eklenebilir.
            // handleFailedLoginAttempt(request.getEmail());
            throw new BadCredentialsException("E-posta veya şifre yanlış."); // Güvenli genel hata mesajı
        }

        // Kimlik doğrulama başarılıysa Spring Security'nin UserDetails objesini al
        // CustomUserDetailsService'in dönüştürdüğü UserDetails objesi buraya gelir.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // --- Login Sonrası Kontroller ve Abonelik Logic'i (Planlanan) ---
        // Bu kısım, abonelik ve firma aktiflik kontrollerini içerecek şekilde güncellenecek.
        // UserDetails'den kullanıcının tam entity objesini çekmek gerekebilir
        // (UserDetails JPA Entity değildir ve üzerinde Company/Subscription gibi ilişkili objelere erişim garanti olmaz).
        /*
        User authenticatedUser = userRepository.findByEmail(userDetails.getUsername())
                                    .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB!")); // Olmaması gereken durum

        // 1. Kullanıcı Durumu Kontrolü (enabled, locked, status)
        if (!authenticatedUser.isEnabled() || authenticatedUser.isLocked() || authenticatedUser.getStatus() != UserStatus.ACTIVE) { // Eğer UserStatus enum varsa
            logger.warn("Giriş engellendi: Kullanıcı durumu uygun değil. Email: {}", authenticatedUser.getEmail());
             // Abonelik aktif değilse fırlattığımız özel exception SubscriptionInactiveException olabilir.
             // throw new SubscriptionInactiveException("Hesabınız şu anda aktif değil."); // GlobalExceptionHandler yakalar
             // Veya Spring Security DisabledException, LockedException fırlatılabilir (CustomUserDetailsService'de)
            throw new BadCredentialsException("Hesabınız şu anda aktif değil."); // Generic message
        }

        // 2. Firmanın Abonelik Durumunu Kontrol Et (CompanySubscriptionService ile)
        Company userCompany = authenticatedUser.getCompany();
        if (userCompany == null) { // Kullanıcı firmaya bağlı değilse (olmaması gereken durum @ManyToOne nullable=false yaptık)
             logger.error("Kullanıcı firmaya bağlı değil: {}", authenticatedUser.getEmail());
             throw new RuntimeException("Kullanıcı şirket bilgisi eksik."); // Kritik hata
        }

        // CompanySubscriptionService'den firmanın aktif aboneliğini çek
        // Bu metot abonelik yoksa veya süresi dolmuşsa durumu kontrol edecek.
        Optional<CompanySubscription> activeSubscriptionOpt = companySubscriptionService.getCurrentActiveSubscription(userCompany);

        if (activeSubscriptionOpt.isEmpty() || !activeSubscriptionOpt.get().hasAccess()) { // hasAccess() Subscription entity'de yardımcı metot olabilir.
             logger.warn("Giriş engellendi: Firmanın aktif aboneliği yok veya süresi dolmuş/pasif. Firma: {}, Kullanıcı: {}", userCompany.getName(), authenticatedUser.getEmail());
            throw new SubscriptionInactiveException("Firmanızın aboneliği şu anda aktif değil."); // GlobalExceptionHandler yakalar
        }

        // 3. Login Sırasında Başarısız Deneme Sayacını Sıfırla ve Son Giriş Zamanını Güncelle
        // Eğer failed login attempts yönetimi burada yapılıyorsa:
        // if (authenticatedUser.getFailedLoginAttempts() > 0) {
        //     authenticatedUser.setFailedLoginAttempts(0);
        //     userRepository.save(authenticatedUser); // Transactional olması gerekli
        // }
        // authenticatedUser.setLastLoginAt(LocalDateTime.now()); // LocalDateTime kullandığınız için
        // userRepository.save(authenticatedUser); // Transactional olması gerekli
        */
        // --- Login Sonrası Kontroller Sonu ---


        // Tokenları Üret (Access ve Refresh) - Eğer kontrollere takılmadıysa
        // CustomUserDetailsService UserDetails'e abonelik/firma bilgisi ekleyecekse,
        // burada generateToken metoduna verdiğimiz userDetails bu bilgiyi taşıyacak.
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());

        Instant accessTokenExpiration = jwtService.extractExpiration(accessToken).toInstant();

        // AuthResponse Hazırla
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(accessTokenExpiration)
                // Eğer AuthResponse'a firma/abonelik bilgisi eklenecekse:
                // .companyId(authenticatedUser.getCompany().getId().toString())
                // .companyName(authenticatedUser.getCompany().getName())
                // .subscriptionStatus(activeSubscriptionOpt.map(s -> s.getStatus().name()).orElse("NO_SUBSCRIPTION")) // Enum name
                // .planName(activeSubscriptionOpt.map(s -> s.getPlan().getName()).orElse(null)) // Plan name
                .build();

        logger.info("Kullanıcı girişi başarılı. Email: {}", request.getEmail());
        return authResponse;
    }

    // --- Refresh Token Metodu ---
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Refresh token isteği alındı.");
        // Gelen refresh token'ı bul, yoksa veya süresi geçmişse hata fırlat (RefreshTokenService handle ediyor)
        RefreshToken existingRefreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    logger.warn("Refresh token bulunamadı veya geçerli değil.");
                    return new TokenRefreshException(request.getRefreshToken(), "Yenileme tokeni geçersiz.");
                });

        refreshTokenService.verifyExpiration(existingRefreshToken); // Süresi geçmişse exception fırlatır ve siler

        // Tokena ait kullanıcıyı yükle
        // CustomUserDetailsService bu noktada kullanıcının firma/abonelik bilgilerini de yükleyecek şekilde güncellenecek.
        UserDetails userDetails = userDetailsService.loadUserByUsername(existingRefreshToken.getUsername());

        // --- Refresh Sonrası Abonelik Kontrolü (Planlanan) ---
        // Refresh işlemi sonrası da kullanıcının veya firmanın aboneliğinin aktif olup olmadığını kontrol etmek isteyebiliriz.
        // Eğer aktif değilse, yeni token üretmeyip hata fırlatabiliriz.
         /*
         User refreshTokenUser = userRepository.findByEmail(userDetails.getUsername())
                                     .orElseThrow(() -> new RuntimeException("Authenticated user not found in DB!")); // Olmaması gereken durum

         Company refreshTokenCompany = refreshTokenUser.getCompany();
         if (refreshTokenCompany == null) { // Kullanıcı firmaya bağlı değilse
              logger.error("Kullanıcı firmaya bağlı değil (Refresh): {}", refreshTokenUser.getEmail());
              throw new RuntimeException("Kullanıcı şirket bilgisi eksik."); // Kritik hata
         }

         Optional<CompanySubscription> activeSubscriptionOpt = companySubscriptionService.getCurrentActiveSubscription(refreshTokenCompany);

         if (activeSubscriptionOpt.isEmpty() || !activeSubscriptionOpt.get().hasAccess()) {
              logger.warn("Refresh engellendi: Firmanın aktif aboneliği yok veya süresi dolmuş/pasif. Firma: {}, Kullanıcı: {}", refreshTokenCompany.getName(), refreshTokenUser.getEmail());
             throw new SubscriptionInactiveException("Firmanızın aboneliği şu anda aktif değil. Token yenileme başarısız."); // GlobalExceptionHandler yakalar
         }
         */
        // --- Refresh Sonrası Kontrol Sonu ---


        // Single-Use pattern: Eski refresh tokenı sil
        // Eğer abonelik kontrolü geçtiyse
        refreshTokenService.deleteByToken(existingRefreshToken.getToken());

        // Yeni access ve refresh tokenları oluştur
        String newAccessToken = jwtService.generateToken(userDetails);
        RefreshToken newRefreshTokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());

        Instant newAccessTokenExpiration = jwtService.extractExpiration(newAccessToken).toInstant();

        // AuthResponse oluştur
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(newAccessTokenExpiration)
                // Eğer AuthResponse'a firma/abonelik bilgisi eklenecekse (Refresh'de de):
                // .companyId(refreshTokenUser.getCompany().getId().toString())
                // .companyName(refreshTokenUser.getCompany().getName())
                // .subscriptionStatus(activeSubscriptionOpt.map(s -> s.getStatus().name()).orElse("NO_SUBSCRIPTION_REFRESH"))
                // .planName(activeSubscriptionOpt.map(s -> s.getPlan().getName()).orElse(null))
                .build();

        logger.info("Refresh token başarıyla tamamlandı.");
        return authResponse;
    }

    // --- Logout Metodu ---
    @Override
    @Transactional // Refresh token silme işlemi
    public void logout(String refreshToken) {
        logger.info("Logout isteği alındı.");
        // Sadece ilgili refresh tokenı veritabanından sil
        // RefreshTokenService içinde loglama var. Bulunamazsa hata fırlatmıyor, sadece logluyor.
        refreshTokenService.deleteByToken(refreshToken);
        // Access token hala geçerli olsa da, refresh token olmadığından
        // yeni token setine ulaşılamaz ve oturum etkin bir şekilde sona erer.
        logger.info("Logout işlemi tamamlandı.");
    }

    // --- Başarısız Login Denemesi Yardımcı Metodu (Opsiyonel) ---
     /*
     @Transactional // Bu metot transaction içinde olmalı
     private void handleFailedLoginAttempt(String email) {
         Optional<User> userOpt = userRepository.findByEmail(email);
         userOpt.ifPresent(user -> {
             user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
             if (user.getFailedLoginAttempts() >= LOGIN_ATTEMPT_THRESHOLD) { // LOGIN_ATTEMPT_THRESHOLD bir sabittir
                 user.setLocked(true);
                 user.setLockoutEndTime(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES)); // LOCKOUT_DURATION_MINUTES bir sabittir
                 user.setStatus(UserStatus.LOCKED_FAILED_LOGIN); // Eğer UserStatus enum varsa
                 logger.warn("Kullanici hesabı başarısız giriş denemeleri nedeniyle kilitlendi: {}", email);
             }
             userRepository.save(user);
         });
     }
     */

}