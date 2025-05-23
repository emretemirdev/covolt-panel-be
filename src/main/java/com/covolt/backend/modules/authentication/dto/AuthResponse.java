package com.covolt.backend.modules.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

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

    // Kullanıcı bilgileri
    private UUID userId;
    private String email;
    private String fullName;
}
