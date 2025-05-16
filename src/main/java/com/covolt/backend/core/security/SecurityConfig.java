package com.covolt.backend.core.security;

import com.covolt.backend.core.security.common.ApiEndpoint;
import com.covolt.backend.core.security.common.SecurityAccess;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor // Bu, final alanlar için constructor'ı otomatik oluşturur
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService; // CustomUserDetailsService buraya enjekte edilecek
    // PasswordEncoder'ı buradan kaldırıyoruz, çünkü bu sınıf onu @Bean ile üretiyor.
    // private final PasswordEncoder passwordEncoder; // <<<--- BU SATIRI SİL VEYA YORUMA AL

    // @RequiredArgsConstructor bu constructor'ı oluşturur (PasswordEncoder olmadan):
    // public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
    //                       UserDetailsService userDetailsService) {
    //     this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    //     this.userDetailsService = userDetailsService;
    // }

    // PasswordEncoder bean'ini burada tanımlıyoruz.
    // AuthServiceImpl veya diğer servisler bu bean'i doğrudan enjekte alacak.
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