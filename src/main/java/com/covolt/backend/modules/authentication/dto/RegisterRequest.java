package com.covolt.backend.modules.authentication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "E-posta alanı boş olamaz.")
    @Email(message = "Geçerli bir e-posta adresi girin.")
    private String email;

    @NotBlank(message = "Kullanıcı adı alanı boş olamaz.")
    @Size(min = 3, max = 50, message = "Kullanıcı adı 3 ile 50 karakter arasında olmalıdır.")
    private String username;

    @NotBlank(message = "Şifre alanı boş olamaz.")
    @Size(min = 8, message = "Şifre en az 8 karakter olmalıdır.")
    private String password;

    // Opsiyonel Alanlar
    private String fullName;
    private String phoneNumber;

    @NotBlank(message = "Kuruluş adı alanı boş olamaz.") // Kuruluş adı zorunlu
    @Size(min = 2, max = 100, message = "Kuruluş adı 2 ile 100 karakter arasında olmalıdır.")
    private String companyName; // Kullanıcının kayıt olurken gireceği kuruluş adı
}