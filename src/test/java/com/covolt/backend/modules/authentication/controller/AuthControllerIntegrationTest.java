package com.covolt.backend.modules.authentication.controller;

import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.modules.authentication.dto.LoginRequest;
import com.covolt.backend.modules.authentication.dto.RefreshTokenRequest;
import com.covolt.backend.modules.authentication.dto.RegisterRequest;
import com.covolt.backend.modules.authentication.dto.LogoutRequest; // LogoutRequest import edildi
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.RefreshToken; // RefreshToken import edildi
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.core.repository.CompanyRepository;
import com.covolt.backend.core.repository.RefreshTokenRepository; // RefreshTokenRepository import edildi
import com.covolt.backend.core.security.jwt.JwtService; // JwtService import edildi
import com.covolt.backend.modules.authentication.service.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("AuthController Entegrasyon Testleri")
@Transactional // Her test metodundan sonra işlemleri geri alır
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository; // RefreshToken testleri için eklendi

    @Autowired
    private RefreshTokenService refreshTokenService; // RefreshToken testleri için eklendi

    @Autowired
    private JwtService jwtService; // Logout testi için access token üretmek amacıyla eklendi

    @Autowired
    private RoleRepository roleRepository; // RoleRepository'yi enjekte et

    private Company testCompany;
    private User testUser;
    private final String userEmail = "loginuser@example.com";
    private final String userPassword = "Password123!";
    private final String userUsername = "loginuser";

    @BeforeEach
    void setUp() {
        testCompany = companyRepository.save(Company.builder().name("Login Test Company").build());

        // "ROLE_USER" rolünü bul veya oluştur
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save( // <-- DÜZELTİLMİŞ KISIM
                        Role.builder()
                                .name("ROLE_USER")
                                .description("Standart kullanıcı rolü test için")
                                .permissions(new java.util.HashSet<>()) // Gerekirse izinler eklenebilir
                                .build()
                ));
        testUser = User.builder()
                .email(userEmail)
                .username(userUsername)
                .password(passwordEncoder.encode(userPassword))
                .company(testCompany)
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>(Set.of(userRole))) // Immutable Set.of()'ı mutable HashSet'e çevir // Değiştirilebilir seti ata
                .build();
        userRepository.save(testUser);
    }
    @Test
    @DisplayName("POST /api/auth/register - Başarılı Kayıt")
    void register_whenValidRequest_shouldCreateUserAndCompanyAndReturnTokens() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("testuser@example.com")
                .username("testuser")
                .password("Password123!")
                .fullName("Test User")
                .companyName("Test Company")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated()) // HTTP 201 Created
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("POST /api/auth/register - E-posta Zaten Kullanımda")
    void register_whenEmailAlreadyExists_shouldReturnConflict() throws Exception {
        // testUser @BeforeEach'te userEmail ile oluşturuldu.
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(userEmail) // Mevcut e-posta
                .username("newreguser")
                .password("Password123!")
                .fullName("New Reg User")
                .companyName("New Reg Company")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Bu e-posta veya kullanıcı adı zaten kayıtlı.")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Geçersiz İstek (Validation Hatası)")
    void register_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("notanemail")
                .username("tu")
                .password("short")
                .companyName("") // Boş bırakıldı (NotBlank ve Size ihlali)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("Validasyon hatası oluştu.")))
                .andExpect(jsonPath("$.fieldErrors.email", is("Geçerli bir e-posta adresi girin.")))
                .andExpect(jsonPath("$.fieldErrors.username", is("Kullanıcı adı 3 ile 50 karakter arasında olmalıdır.")))
                .andExpect(jsonPath("$.fieldErrors.password", is("Şifre en az 8 karakter olmalıdır.")))
                // BURAYI DÜZELT: Boş string için genellikle NotBlank mesajı ilk/tek gelir.
                .andExpect(jsonPath("$.fieldErrors.companyName", is("Kuruluş adı alanı boş olamaz."))); // Metindeki hataya göre düzeltildi
        // Eğer Size hatası mesajını da kontrol etmek isterseniz
        // (bazı validatorlarda birden fazla mesaj gelebilir):
        // .andExpect(jsonPath("$.fieldErrors.companyName", anyOf(
        //      is("Kuruluş adı alanı boş olamaz."),
        //      is("Kuruluş adı 2 ile 100 karakter arasında olmalıdır.")
        // )))
    }

    // --- LOGIN TESTLERİ ---
    @Test
    @DisplayName("POST /api/auth/login - Başarılı Giriş")
    void login_whenValidCredentials_shouldReturnTokens() throws Exception {
        LoginRequest loginRequest = new LoginRequest(userEmail, userPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Yanlış Şifre")
    void login_whenInvalidPassword_shouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest(userEmail, "WrongPassword123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized (BadCredentialsException)
                .andExpect(jsonPath("$.message", is("E-posta veya şifre yanlış.")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Var Olmayan E-posta")
    void login_whenEmailDoesNotExist_shouldReturnUnauthorized() throws Exception {
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", userPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // HTTP 401 Unauthorized (BadCredentialsException)
                .andExpect(jsonPath("$.message", is("E-posta veya şifre yanlış.")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Geçersiz İstek (Validation Hatası)")
    void login_whenInvalidRequest_shouldReturnBadRequest() throws Exception {
        LoginRequest loginRequest = new LoginRequest("", ""); // Boş e-posta ve şifre

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validasyon hatası oluştu.")))
                .andExpect(jsonPath("$.fieldErrors.email", is("E-posta alanı boş olamaz.")))
                .andExpect(jsonPath("$.fieldErrors.password", is("Şifre alanı boş olamaz.")));
    }


    @Test
    @DisplayName("POST /api/auth/login - Kilitli Kullanıcı (Simülasyon)")
    void login_whenUserIsLocked_shouldReturnUnauthorized() throws Exception {
        testUser.setLocked(true); // Kullanıcıyı kilitle
        userRepository.save(testUser); // Değişikliği kalıcı yap

        LoginRequest loginRequest = new LoginRequest(userEmail, userPassword);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized()) // Statü 401 beklenmeye devam ediyor
                // BURAYI DÜZELT: GlobalExceptionHandler'daki LockedException handler'ının döndürdüğü mesajı bekle
                .andExpect(jsonPath("$.message", is("Hesabınız kilitlenmiştir."))); // GlobalExceptionHandler'dan gelen mesajı bekliyoruz
    }

    // --- REFRESH TOKEN TESTLERİ ---
    @Test
    @DisplayName("POST /api/auth/refresh - Geçerli Refresh Token")
    void refreshToken_whenValidToken_shouldReturnNewTokens() throws Exception {
        // Önce kullanıcı için bir refresh token oluşturalım
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser.getEmail());

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(refreshToken.getToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken").value(is(org.hamcrest.Matchers.not(refreshToken.getToken())))); // Yeni refresh token farklı olmalı

        // Eski refresh token'ın silindiğini ve yenisinin oluştuğunu DB'den kontrol et
        assertTrue(refreshTokenRepository.findByToken(refreshToken.getToken()).isEmpty(), "Eski refresh token silinmeliydi.");
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Geçersiz (Var Olmayan) Refresh Token")
    void refreshToken_whenTokenIsInvalid_shouldReturnUnauthorized() throws Exception {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(UUID.randomUUID().toString()); // Rastgele, var olmayan token

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized()) // TokenRefreshException -> AUTH_005
                .andExpect(jsonPath("$.message", is(com.covolt.backend.core.exception.ErrorCode.AUTH_005.formatMessage())));
    }

    @Test
    @DisplayName("POST /api/auth/refresh - Süresi Dolmuş Refresh Token")
    void refreshToken_whenTokenIsExpired_shouldReturnUnauthorized() throws Exception {
        // Süresi dolmuş bir refresh token oluşturalım
        RefreshToken expiredToken = RefreshToken.builder()
                .username(testUser.getEmail())
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().minusSeconds(3600)) // 1 saat önce süresi dolmuş
                .build();
        refreshTokenRepository.save(expiredToken);

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(expiredToken.getToken());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized()) // TokenRefreshException -> AUTH_006
                .andExpect(jsonPath("$.message", is(com.covolt.backend.core.exception.ErrorCode.AUTH_006.formatMessage(expiredToken.getToken()))));
        // AUTH_006 mesajı token değerini içeriyor
    }

    // --- LOGOUT TESTLERİ ---
    @Test
    @DisplayName("POST /api/auth/logout - Başarılı Çıkış")
    void logout_whenAuthenticatedWithValidRefreshToken_shouldDeleteTokenAndReturnNoContent() throws Exception {
        // 1. Kullanıcı için bir refresh token oluştur
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(testUser.getEmail());

        // 2. Kullanıcı için bir access token üret (normalde login ile alınırdı, burada simüle ediyoruz)
        // CustomUserDetailsService UserDetails'i User'dan oluşturur.
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getEmail(), testUser.getPassword(), new java.util.ArrayList<>());
        String accessToken = jwtService.generateToken(userDetails);


        LogoutRequest logoutRequest = new LogoutRequest(refreshToken.getToken());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken) // Geçerli access token ekle
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent()); // HTTP 204 No Content

        // Refresh token'ın DB'den silindiğini doğrula
        assertFalse(refreshTokenRepository.findByToken(refreshToken.getToken()).isPresent(),
                "Refresh token çıkış sonrası silinmeliydi.");
    }

    @Test
    @DisplayName("POST /api/auth/logout - Kimlik Doğrulanmamış İstek")
    void logout_whenNotAuthenticated_shouldReturnUnauthorizedBySecurityFilter() throws Exception {
        LogoutRequest logoutRequest = new LogoutRequest("any-refresh-token");

        mockMvc.perform(post("/api/auth/logout") // Authorization header YOK
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isForbidden()); // Spring Security filter tarafından engellenmeli
    }

    @Test
    @DisplayName("POST /api/auth/logout - Geçersiz Refresh Token ile (Kimlik Doğrulanmış)")
    void logout_whenAuthenticatedWithInvalidRefreshToken_shouldStillReturnNoContent() throws Exception {
        // Geçerli bir access token üret
        org.springframework.security.core.userdetails.UserDetails userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getEmail(), testUser.getPassword(), new java.util.ArrayList<>());
        String accessToken = jwtService.generateToken(userDetails);

        String nonExistentRefreshToken = UUID.randomUUID().toString();
        LogoutRequest logoutRequest = new LogoutRequest(nonExistentRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isNoContent());

        // Var olmayan token zaten silinemez, DB'de bir değişiklik olmamalı.
        // Bu senaryoda hata fırlatılmıyor, servis sadece logluyor.
    }
}