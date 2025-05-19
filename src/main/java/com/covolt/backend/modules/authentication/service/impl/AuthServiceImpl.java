package com.covolt.backend.modules.authentication.service.impl;

// --- Spring Framework & Security Importları ---
import com.covolt.backend.modules.authentication.dto.AuthResponse;
import com.covolt.backend.modules.authentication.dto.LoginRequest;
import com.covolt.backend.modules.authentication.dto.RefreshTokenRequest;
import com.covolt.backend.modules.authentication.dto.RegisterRequest;
import com.covolt.backend.modules.authentication.service.AuthService;
import com.covolt.backend.modules.authentication.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transaction yönetimi için

// --- Proje İçi Importlar ---

import com.covolt.backend.core.exception.DuplicateRegistrationException;
import com.covolt.backend.core.exception.ResourceCreationException; // Yeni özel hata sınıfı
import com.covolt.backend.core.exception.TokenRefreshException;

import com.covolt.backend.core.model.Company; // Yeni Company Entity
import com.covolt.backend.core.model.RefreshToken;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.enums.CompanyStatus; // CompanyStatus Enum
import com.covolt.backend.core.model.CompanySubscription; // Abonelik Entity (abonelik servisi kullanılınca)
import com.covolt.backend.core.model.enums.UserSubscriptionStatus; // Abonelik Enum (abonelik servisi kullanılınca)


import com.covolt.backend.core.repository.CompanyRepository; // Yeni Company Repository
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.core.security.jwt.JwtService;
import com.covolt.backend.core.security.service.CustomUserDetailsService;
import com.covolt.backend.service.CompanySubscriptionService; // Yeni Abonelik Servisi (şimdi kullanıyoruz)

// --- Java Standart Importlar ---
import java.time.Instant; // Zaman için
import java.util.HashSet;
import java.util.Optional; // Optional importu eklendi
import java.util.Set;

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
    private final CompanySubscriptionService companySubscriptionService; // Abonelik servisi şimdi enjekte ediliyor


    // --- Constructor ---
    // Spring Boot bu constructor'daki bağımlılıkları otomatik enjekte eder
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           CompanyRepository companyRepository, // Yeni bağımlılık eklendi
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           CustomUserDetailsService userDetailsService,
                           CompanySubscriptionService companySubscriptionService // Abonelik servisi şimdi bağımlılık
    ) { // Abonelik servisi için yer tutuldu
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyRepository = companyRepository; // Yeni bağımlılık atandı
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
        this.companySubscriptionService = companySubscriptionService; // Şimdi atanıyor
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

        // 6. Yeni Oluşturulan Firma İçin Deneme Aboneliği Başlat (Şimdi aktive edildi)
        try {
            companySubscriptionService.startTrial(savedCompany); // defaultTrialPlan bilgisi servisin içinde config'den geliyor
            logger.info("Firma (ID: {}) için deneme aboneliği başarıyla başlatıldı.", savedCompany.getId());
        } catch (Exception e) {
            // Abonelik başlatılamasa bile kullanıcı ve firma kaydedilmiş olabilir.
            // Karar: Kayıt işlemini atomik mi istiyoruz yoksa abonelik opsiyonel mi?
            // Şimdilik, abonelik hatasını da loglayıp, kayıt işlemini başarılı sayalım (soft fail).
            // Eğer kritikse, buradan da exception fırlatılabilir ve kayıt geri alınır.
            logger.error("Firma (ID: {}) için deneme aboneliği başlatılırken hata oluştu. Kayıt tamamlandı ama abonelik yok: {}",
                    savedCompany.getId(), e.getMessage(), e);
            // Eğer burası kritikse aşağıdaki satırı aktif edin:
            // throw new ResourceCreationException("Kayıt tamamlandı ancak abonelik başlatılırken hata oluştu: " + e.getMessage(), e);
        }


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
                // CustomUserDetails'e bu bilgiler eklendi. Access token payload'ına eklemeyi düşünebilirsiniz
                // veya LoginResponse DTO'suna direkt ekleyebilirsiniz.
                // Şu anki AuthResponse DTO'sunda bu alanlar yok, gerekirse AuthResponse güncellenir.
                .build();

        logger.info("Kullanıcı kaydı ve ilk tokenlar başarıyla oluşturuldu: Email: {}", savedUser.getEmail());
        return authResponse;
    }

    // --- Login Metodu ---
    @Override
    @Transactional(readOnly = false) // Başarısız deneme sayacını sıfırlamak/Son login zamanını kaydetmek için false yaptık
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
            // Optional: Başarısız deneme sayısını artırma logic'i eklenebilir (buradan kaldırıldı, User Entity'sinde alan var).
            // throw new BadCredentialsException("E-posta veya şifre yanlış."); // GlobalExceptionHandler handled it
            throw ex; // Hata fırlatmayı GlobalExceptionHandler'a bırakıyoruz
        }


        // Kimlik doğrulama başarılıysa Spring Security'nin UserDetails objesini al
        // CustomUserDetailsService'in dönüştürdüğü UserDetails objesi buraya gelir.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // --- Login Sonrası Kontroller ve Abonelik Logic'i ---
        // Bu kısım CustomUserDetailsService içine taşındı. CustomUserDetailsService zaten
        // kullanıcının enabled, locked durumunu ve firmanın aktif aboneliğini kontrol edip,
        // UserDetails objesine ilgili bilgiyi set ediyor (CovoltUserDetails).
        // UserDetails.isEnabled(), isAccountNonLocked() vb. Spring Security tarafından login sırasında otomatik kontrol edilir.
        // Abonelik kontrolü CustomUserDetailsService'de yapılarak isEnabled veya diğer non-locked metotları override edilebilir,
        // Veya SubscriptionInactiveException CustomUserDetailsService içinde fırlatılıp GlobalExceptionHandler tarafından yakalanabilir.
        // Şu anki implementasyonda abonelik durumu CovoltUserDetails içinde taşınıyor.
        // Login sonrasında sadece başarılı kimlik doğrulamadan devam ediyoruz.

        // User entity'sini çekip last login gibi bilgileri güncelleyebiliriz (Opsiyonel, transactional yaparsak)
        // try {
        //     User authenticatedUser = userRepository.findByEmail(userDetails.getUsername())
        //             .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB: " + userDetails.getUsername()));
        //     // Eğer login attempt sayacı AuthServiceImpl'de yönetiliyorsa
        //     if (authenticatedUser.getFailedLoginAttempts() > 0) {
        //          authenticatedUser.setFailedLoginAttempts(0);
        //          userRepository.save(authenticatedUser); // save only if changed
        //     }
        //      authenticatedUser.setLastLoginAt(LocalDateTime.now()); // Eğer LocalDateTime kullandığınız için
        //     // userRepository.save(authenticatedUser); // Kaydet
        // } catch (Exception e) {
        //      logger.error("Login sonrası kullanıcı verilerini güncelleme hatası: {}", userDetails.getUsername(), e);
        //      // Logla ama girişi engelleme, zaten login oldu
        // }
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
                // Eğer AuthResponse'a firma/abonelik bilgisi eklenecekse, CovoltUserDetails'ten alıp buraya ekleyebilirsiniz.
                // (AuthResponse DTO'su şu an bu alanları içermiyor)
                // if (userDetails instanceof CovoltUserDetails covoltUserDetails) {
                //    authResponse.setCompanyId(covoltUserDetails.getCompanyId()); // uuid ise toString
                //    authResponse.setCompanyName(covoltUserDetails.getCompanyName());
                //    // etc.
                // }
                .build();

        logger.info("Kullanıcı girişi başarılı. Email: {}", request.getEmail());
        return authResponse;
    }

    // --- Refresh Token Metodu ---
    @Override
    @Transactional
    // AuthServiceImpl.java - refreshToken metodu
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        logger.info("Refresh token isteği alındı.");

        RefreshToken existingRefreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> {
                    logger.warn("Refresh token bulunamadı veya geçerli değil. İstenen token: {}", request.getRefreshToken());
                    // TokenRefreshException'ın parametresiz constructor'ını kullanıyoruz (AUTH_005).
                    return new TokenRefreshException();
                });

        // RefreshTokenService'in verifyExpiration metodu token süresi dolmuşsa ErrorCode.AUTH_006 ile kendi TokenRefreshException'ını fırlatacak.
        // Başka bir try/catch gerekmez, hata yakalama GlobalExceptionHandler'a bırakılır.
        refreshTokenService.verifyExpiration(existingRefreshToken);

        UserDetails userDetails = userDetailsService.loadUserByUsername(existingRefreshToken.getUsername());

        // --- Refresh Sonrası Abonelik Kontrolü (Planlanan - CustomUserDetailsService'de yapılıyor artık) ---
        // userDetails zaten CovoltUserDetails ise gerekli abonelik bilgileri ve durumu içerir.
        // Güvenlik filtreleri (SecurityConfig) veya @PreAuthorize ekspresyonları
        // CovoltUserDetails içindeki abonelik/feature bilgilerini kullanabilir.
        // Eğer kritik bir özellik aboneliğe bağlı ise, token validasyonu veya refresh sonrası
        // o özelliğe erişimin hala mümkün olup olmadığı CustomUserDetailsService'den gelen
        // CovoltUserDetails'deki isEnabled/isAccountNonLocked metotları veya authority listesi ile kontrol edilmelidir.
        // Şu anki yapıda CovoltUserDetails isEnabled'ı User entity enabled/locked'ından alıyor, abonelik durumu ayrı taşınıyor.
        // Erişim kontrolü Security layer veya metod seviyesinde @PreAuthorize ile yapılmalıdır, login/refresh değil.

        // Eski refresh tokenı sil
        refreshTokenService.deleteByToken(existingRefreshToken.getToken());

        // Yeni tokenları üret
        String newAccessToken = jwtService.generateToken(userDetails);
        RefreshToken newRefreshTokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());
        Instant newAccessTokenExpiration = jwtService.extractExpiration(newAccessToken).toInstant();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(newAccessTokenExpiration)
                // Eğer AuthResponse'a firma/abonelik bilgisi eklenecekse (CustomUserDetails'ten)
                // if (userDetails instanceof CovoltUserDetails covoltUserDetails) { ... }
                .build();

        logger.info("Refresh token başarıyla tamamlandı kullanıcı: {}", userDetails.getUsername());
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
        // SecurityContextHolder.clearContext(); // Logout request Spring Security context temizleyicisi tarafından handle edilebilir.
        // Logout endpointine ulaşıldıysa JWT filtre geçerli access token'ı doğruladı demektir.
        // Logout logic sadece refresh token'ı geçersiz kılarak token çiftini bitirir.
        logger.info("Logout işlemi tamamlandı.");
    }

    // --- Başarısız Login Denemesi Yardımcı Metodu (Opsiyonel - Şu an kullanılmıyor) ---
     /*
     @Transactional // Bu metot transaction içinde olmalı
     private void handleFailedLoginAttempt(String email) {
         // LOGIN_ATTEMPT_THRESHOLD ve LOCKOUT_DURATION_MINUTES gibi sabitlere ihtiyacı var
         // User entity'de lockoutEndTime gibi alanlara da ihtiyacı var.
         // Eğer UserStatus enum kullanılıyorsa.
         // Bu logic UserRepository'nin içinde veya ayrı bir AccountManagementService içinde daha uygun olabilir.
         Optional<User> userOpt = userRepository.findByEmail(email);
         userOpt.ifPresent(user -> {
             int currentAttempts = user.getFailedLoginAttempts() + 1;
             user.setFailedLoginAttempts(currentAttempts);
             if (currentAttempts >= LOGIN_ATTEMPT_THRESHOLD) {
                 user.setLocked(true);
                 // user.setLockoutEndTime(LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
                 // user.setStatus(UserStatus.LOCKED_FAILED_LOGIN); // Eğer UserStatus varsa
                 logger.warn("Kullanici hesabı başarısız giriş denemeleri nedeniyle kilitlendi: {}", email);
             }
             userRepository.save(user); // Değişiklikleri kaydet
         });
     }
     */
}