package com.covolt.backend.core.security;

import com.covolt.backend.core.security.common.ApiEndpoint;

import com.covolt.backend.core.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor; // Lombok kullanıyorsak
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService; // ÖNEMLİ
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // ÖNEMLİ
import org.springframework.security.crypto.password.PasswordEncoder;   // ÖNEMLİ
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor // Bu, final alanlar için constructor'ı otomatik oluşturur
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService; // CustomUserDetailsService buraya enjekte edilecek
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // AuthenticationProvider bean'i, UserDetailsService ve PasswordEncoder'ı kullanır.
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        // PasswordEncoder'ı doğrudan yukarıdaki @Bean metodunu çağırarak alıyoruz.
        // VEYA Spring context'inden PasswordEncoder bean'ini enjekte almasını sağlayabiliriz:
        // authProvider.setPasswordEncoder(this.passwordEncoder()); // this.passwordEncoder() yerine direkt metot çağrısı
        authProvider.setPasswordEncoder(passwordEncoder()); // Yukarıdaki @Bean metodunu çağırır
        return authProvider;
    }
    /**
     * Defines a CORS configuration source that allows all origins, methods, and headers, permits credentials, and sets a preflight cache duration of one hour.
     *
     * @return a CorsConfigurationSource configured for permissive cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Tüm origins'e izin ver
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);        // veya daha güvenli bir yaklaşım için:
        // configuration.addAllowedOriginPattern("*");

        // Eğer credentials (cookies, auth headers) kullanıyorsanız,
        // wildcard origin (*) ile setAllowCredentials(true) birlikte kullanılamaz.
        // Bu durumda spesifik origin'ler belirtmeli veya pattern kullanmalısınız:
        configuration.setAllowedOriginPatterns(Collections.singletonList("*"));

        // İzin verilen tüm HTTP metodları
        configuration.addAllowedMethod("*");

        // İzin verilen tüm headers
        configuration.addAllowedHeader("*");

        // Credentials izni (cookies, authorization headers, etc.)
        configuration.setAllowCredentials(true);

        // OPTIONS isteklerinin önbelleğe alınma süresi (1 saat)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    for (ApiEndpoint endpoint : ApiEndpoint.values()) {
                        String httpMethodName = (endpoint.getHttpMethod() != null) ? endpoint.getHttpMethod().name() : null;
                        AntPathRequestMatcher requestMatcher = new AntPathRequestMatcher(endpoint.getPathPattern(), httpMethodName);

                        switch (endpoint.getAccessType()) {
                            case PERMIT_ALL:
                                auth.requestMatchers(requestMatcher).permitAll();
                                break;
                            case AUTHENTICATED:
                                auth.requestMatchers(requestMatcher).authenticated();
                                break;
                            case HAS_ROLE:
                                // ApiEndpoint.authority alanı "PLATFORM_ADMIN" gibi ROLE_ prefix'siz olmalı
                                auth.requestMatchers(requestMatcher).hasRole(endpoint.getAuthority());
                                break;
                            case HAS_AUTHORITY:
                                // ApiEndpoint.authority alanı "MANAGE_ROLES" gibi izin adı olmalı
                                auth.requestMatchers(requestMatcher).hasAuthority(endpoint.getAuthority());
                                break;
                            case DENY_ALL:
                                auth.requestMatchers(requestMatcher).denyAll();
                                break;
                        }
                    }
                    // Enum'da tanımlı olmayan ve yukarıdaki kurallara uymayan diğer tüm istekleri reddet.
                    auth.anyRequest().denyAll();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
