package com.covolt.backend.core.exception; // VEYA modules.authentication.exception

import org.springframework.http.HttpStatus;

public class TokenRefreshException extends BaseApplicationException {

    // Senaryo 1: Token bulunamadı veya genel geçersizlik durumu için
    // Kullanım: throw new TokenRefreshException();
    public TokenRefreshException() {
        super(ErrorCode.AUTH_005, HttpStatus.UNAUTHORIZED);
    }

    // Senaryo 2: Token süresi dolmuş durumu için
    // Kullanım: throw new TokenRefreshException(token.getToken());
    public TokenRefreshException(String expiredTokenValue) {
        super(ErrorCode.AUTH_006, HttpStatus.UNAUTHORIZED, expiredTokenValue);
    }

    // Senaryo 3: Özel bir mesajla ve belirli bir ErrorCode ile (çok nadir gerekebilir)
    // Kullanım: throw new TokenRefreshException("Özel bir token hatası oluştu.", ErrorCode.AUTH_003);
    public TokenRefreshException(String customMessage, ErrorCode specificAuthErrorCode) {
        super(customMessage, specificAuthErrorCode, HttpStatus.UNAUTHORIZED);
    }
}