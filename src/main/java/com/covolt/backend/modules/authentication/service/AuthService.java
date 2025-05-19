package com.covolt.backend.modules.authentication.service;

import com.covolt.backend.modules.authentication.dto.AuthResponse;
import com.covolt.backend.modules.authentication.dto.LoginRequest;
import com.covolt.backend.modules.authentication.dto.RefreshTokenRequest;
import com.covolt.backend.modules.authentication.dto.RegisterRequest;


public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken); // Logout DTO kullanmayıp direkt string alıyoruz
}