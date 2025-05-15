package com.covolt.backend.service;

import com.covolt.backend.dto.auth.AuthResponse;
import com.covolt.backend.dto.auth.LoginRequest;
import com.covolt.backend.dto.auth.RefreshTokenRequest;
import com.covolt.backend.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken); // Logout DTO kullanmayıp direkt string alıyoruz
}