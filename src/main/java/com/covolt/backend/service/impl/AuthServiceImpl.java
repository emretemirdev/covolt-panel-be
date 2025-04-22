package com.covolt.backend.service.impl;

import com.covolt.backend.dto.auth.AuthResponse;
import com.covolt.backend.dto.auth.LoginRequest;
import com.covolt.backend.dto.auth.RefreshTokenRequest;
import com.covolt.backend.dto.auth.RegisterRequest;
import com.covolt.backend.exception.DuplicateRegistrationException; // Kendi hata sınıfımızı import et
import com.covolt.backend.exception.TokenRefreshException;
import com.covolt.backend.model.RefreshToken;
import com.covolt.backend.model.Role; // Kendi Role entity'nizi import edin
import com.covolt.backend.model.User; // Kendi User entity'nizi import edin
import com.covolt.backend.repository.RoleRepository; // Kendi RoleRepository'nizi import edin
import com.covolt.backend.repository.UserRepository; // Kendi UserRepository'nizi import edin
import com.covolt.backend.security.jwt.JwtService;
import com.covolt.backend.security.service.CustomUserDetailsService;
import com.covolt.backend.service.AuthService;
import com.covolt.backend.service.RefreshTokenService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException; // Spring Security Hatası
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors; // Gerekirse authResponse için
import org.springframework.security.core.GrantedAuthority; // Gerekirse authResponse için

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // RoleRepository dependency
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService userDetailsService; // UserDetails yüklemek için

    // Tüm bağımlılıklar Constructor Injection ile alınıyor
    public AuthServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    @Transactional // Kayıt işlemi başarılı ya da tamamen başarısız olsun
    public AuthResponse register(RegisterRequest request) {
        // Kullanıcı ve E-posta benzersizliği kontrolü
        if (userRepository.existsByEmailOrUsername(request.getEmail(), request.getUsername())) {
            throw new DuplicateRegistrationException("Bu e-posta (" + request.getEmail() + ") veya kullanıcı adı (" + request.getUsername() + ") zaten kullanılıyor.");
        }

        // Kullanıcı objesi oluştur ve şifreyi hashle
        User newUser = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .enabled(true) // Default: etkin
                .locked(false) // Default: kilitli değil
                .failedLoginAttempts(0) // Default: 0 başarısız deneme
                .build();


        // Default Role'u ata (örn. ROLE_USER)
        Optional<Role> defaultRoleOpt = roleRepository.findByName("ROLE_USER");
        if (!defaultRoleOpt.isPresent()) {
            // **Çok Önemli:** Uygulama başlarken ROLE_USER gibi varsayılan rolleri DB'ye eklemelisiniz!
            throw new RuntimeException("Varsayılan 'ROLE_USER' rolü veritabanında bulunamadı!");
        }
        Set<Role> roles = new HashSet<>();
        roles.add(defaultRoleOpt.get());
        newUser.setRoles(roles); // User Entity'nizde setRoles metodu olmalı

        // Kullanıcıyı veritabanına kaydet
        User savedUser = userRepository.save(newUser);

        // Kayıt sonrası otomatik giriş (Access + Refresh token oluştur)
        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(savedUser.getEmail()); // Refresh token oluştur

        Instant accessTokenExpiration = jwtService.extractExpiration(accessToken).toInstant(); // Access token bitiş zamanı

        // AuthResponse oluştur
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(accessTokenExpiration)
                // İstenirse kullanıcı bilgileri buraya eklenebilir:
                // .userId(savedUser.getId())
                // .email(savedUser.getEmail())
                // .roles(savedUser.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional(readOnly = true) // Sadece okuma işlemi
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            // Spring Security'nin Authentication Manager'ını kullanarak kimlik doğrula
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException ex) {
            // Kimlik doğrulama başarısızsa (e-posta/şifre yanlış, hesap kilitli/disable vb.)
            // Başarısız giriş deneme sayısını artırma gibi logic burada veya CustomUserDetailsService'de yapılabilir.
            // GlobalExceptionHandler bu hatayı yakalayacak ve genel bir 401 yanıtı dönecek.
            throw new BadCredentialsException("E-posta veya şifre yanlış."); // Daha genel bir mesaj fırlat
        }

        // Kimlik doğrulama başarılıysa UserDetails objesini al
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Access ve Refresh tokenları oluştur
        String accessToken = jwtService.generateToken(userDetails);
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());

        Instant accessTokenExpiration = jwtService.extractExpiration(accessToken).toInstant();

        // AuthResponse oluştur
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(accessTokenExpiration)
                // İstenirse kullanıcı bilgileri userDetails'den alınarak buraya eklenebilir:
                // .email(userDetails.getUsername())
                // .roles(userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional // Refresh token silme ve yenisini yaratma tek bir işlem
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Gelen refresh token'ı bul, yoksa veya süresi geçmişse hata fırlat (RefreshTokenService handle ediyor)
        RefreshToken existingRefreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenRefreshException(request.getRefreshToken(), "Refresh token bulunamadı veya geçerli değil."));

        refreshTokenService.verifyExpiration(existingRefreshToken); // Süresi geçmişse hata fırlatıp siler

        // Tokena ait kullanıcıyı yükle
        UserDetails userDetails = userDetailsService.loadUserByUsername(existingRefreshToken.getUsername());

        // Single-Use pattern: Eski refresh tokenı sil
        refreshTokenService.deleteByToken(existingRefreshToken.getToken());

        // Yeni access ve refresh tokenları oluştur
        String newAccessToken = jwtService.generateToken(userDetails);
        RefreshToken newRefreshTokenEntity = refreshTokenService.createRefreshToken(userDetails.getUsername());

        Instant newAccessTokenExpiration = jwtService.extractExpiration(newAccessToken).toInstant();

        // AuthResponse oluştur
        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresAt(newAccessTokenExpiration)
                // İstenirse kullanıcı bilgileri eklenebilir
                .build();
    }

    @Override
    @Transactional // Refresh token silme işlemi
    public void logout(String refreshToken) {
        // Sadece ilgili refresh tokenı veritabanından sil
        refreshTokenService.deleteByToken(refreshToken);
        // Access token hala geçerli olsa da, refresh token olmadığından
        // yeni token setine ulaşılamaz ve oturum etkin bir şekilde sona erer.
    }

    // GEREKLİ MEVCUT KOD GÜNCELLEMELERİ BU SINIF DIŞINDA YAPILMALIDIR. AŞAĞI BAKINIZ.
}