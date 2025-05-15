package com.covolt.backend.controller.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/verify")
@RequiredArgsConstructor
public class VerificationController {

    @GetMapping("/email")
    public void verifyEmail(@RequestParam String token) {
        // e-posta doğrulaması yap
    }

    @PostMapping("/resend")
    public void resendVerificationEmail(@RequestParam String email) {
        // doğrulama e-postasını tekrar gönder
    }
}
