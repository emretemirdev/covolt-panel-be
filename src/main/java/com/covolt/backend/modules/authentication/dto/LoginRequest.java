package com.covolt.backend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "E-posta alanı boş olamaz.")
    @Email(message = "Geçerli bir e-posta adresi girin.")
    private String email;

    @NotBlank(message = "Şifre alanı boş olamaz.")
    private String password;
}