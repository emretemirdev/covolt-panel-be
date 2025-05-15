package com.covolt.backend.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/password")
@RequiredArgsConstructor
public class PasswordController {

    // Örnek endpoint'ler (implementasyon sonra eklenebilir)
    @PostMapping("/forgot")
    public void forgotPassword(@RequestParam String email) {
        // e-posta adresine şifre sıfırlama bağlantısı gönder
    }

    @PostMapping("/reset")
    public void resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        // token ile yeni şifreyi doğrula ve değiştir
    }

    @PostMapping("/change")
    public void changePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        // oturum açıkken şifre değiştir
    }
}
