package com.covolt.backend.exception;

// Daha önce vardı. Sadece `@ResponseStatus` ANOTASYONUNU KALDIRIN.
// Status global handler'da belirlenecek.
public class TokenRefreshException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String token;

    public TokenRefreshException(String token, String message) {
        super(message);
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}