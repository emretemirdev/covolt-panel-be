package com.covolt.backend.modules.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType = "Bearer"; // Default değer

    // Access token'ın geçerlilik bitiş zamanı (timestamp formatında)
    private Instant expiresAt;

    // Opsiyonel Kullanıcı Bilgileri (auth serviste dahil edilebilir)
    // private UUID userId;
    // private String email;
    // private Set<String> roles;
}