package com.covolt.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {

    @NotBlank(message = "Çıkış yapmak için refresh token gerekli.")
    private String refreshToken;
}