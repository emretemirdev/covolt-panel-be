package com.covolt.backend.core.security;

import com.covolt.backend.core.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// ... diğer importlar (ApiEndpoint, SecurityAccess vb.)
import org.springframework.security.core.userdetails.UserDetailsService; // ÖNEMLİ
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // ÖNEMLİ
import org.springframework.security.crypto.password.PasswordEncoder; // ÖNEMLİ
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor // Bu, final alanlar için constructor'ı otomatik oluşturur
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService; // CustomUserDetailsService buraya enjekte edilecek
    // PasswordEncoder'ı SecurityConfig'e enjekte ETMİYORUZ, çünkü PasswordEncoder bean'ini
    // BU SINIFTA @Bean metoduyla tanımlıyoruz.

    // PasswordEncoder bean'ini burada tanımlıyoruz.
    // AuthServiceImpl veya diğer servisler bu bean'i doğrudan enjekte alacak.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider bean'i, UserDetailsService ve PasswordEncoder'ı kullanır.
    // PasswordEncoder'ı doğrudan yukarıdaki passwordEncoder() metodunu çağırarak alır.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService); // Enjekte edilen UserDetailsService
        authProvider.setPasswordEncoder(passwordEncoder());     // Yukarıda tanımlanan PasswordEncoder bean'i
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ... (csrf, cors, sessionManagement) ...
                .authorizeHttpRequests(auth -> {
                    // ... (ApiEndpoint enum'u ile kurallar) ...
                    // Örnek:
                    // for (ApiEndpoint endpoint : ApiEndpoint.values()) {
                    //     AntPathRequestMatcher requestMatcher = new AntPathRequestMatcher(endpoint.getPathPattern(), endpoint.getHttpMethod() != null ? endpoint.getHttpMethod().name() : null);
                    //     switch (endpoint.getAccessType()) {
                    //         case PERMIT_ALL: auth.requestMatchers(requestMatcher).permitAll(); break;
                    //         case AUTHENTICATED: auth.requestMatchers(requestMatcher).authenticated(); break;
                    //         case HAS_ROLE: auth.requestMatchers(requestMatcher).hasRole(endpoint.getAuthority()); break;
                    //         case HAS_AUTHORITY: auth.requestMatchers(requestMatcher).hasAuthority(endpoint.getAuthority()); break;
                    //         case DENY_ALL: auth.requestMatchers(requestMatcher).denyAll(); break;
                    //     }
                    // }
                    // auth.anyRequest().denyAll();
                    // ŞİMDİLİK BASİT TUTALIM, ENUM ENTEGRASYONUNU SONRA YAPARIZ:
                    auth.requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider()) // Burada oluşturduğumuz provider'ı kullan
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}