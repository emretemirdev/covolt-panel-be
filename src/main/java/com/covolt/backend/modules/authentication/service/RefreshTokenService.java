package com.covolt.backend.modules.authentication.service;

import com.covolt.backend.core.exception.TokenRefreshException;
import com.covolt.backend.core.model.RefreshToken;
import com.covolt.backend.core.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${app.security.jwt.refreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        logger.info("RefreshTokenService initialized with expiration time: {} ms", refreshTokenDurationMs);
    }

    public Optional<RefreshToken> findByToken(String token) {
        logger.debug("Searching for refresh token: {}", token);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        if (refreshToken.isPresent()) {
            logger.debug("Refresh token found: {}", token);
        } else {
            logger.debug("Refresh token not found: {}", token);
        }
        return refreshToken;
    }

    @Transactional
    public RefreshToken createRefreshToken(String username) {
        logger.info("Creating new refresh token for user: {}", username);
        
        logger.debug("Deleting existing refresh tokens for user: {}", username);
        refreshTokenRepository.deleteByUsername(username);

        RefreshToken refreshToken = RefreshToken.builder()
                .username(username)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        logger.info("New refresh token created for user: {}", username);
        logger.debug("Token details - Expiry: {}, Token: {}", savedToken.getExpiryDate(), savedToken.getToken());
        
        return savedToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        logger.debug("Verifying expiration for token: {}", token.getToken());

        if (token.getExpiryDate().isBefore(Instant.now())) {
            logger.warn("Refresh token expired for user: {} with token {}", token.getUsername(), token.getToken());
            refreshTokenRepository.delete(token);
            // YENİ ve DOĞRU KULLANIM:
            // Token süresi dolduğu için ErrorCode.AUTH_006'yı kullanıyoruz.
            // Token değerini de (expiredTokenValue) constructor'a iletiyoruz.
            throw new TokenRefreshException(token.getToken());
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        logger.info("Attempting to delete refresh token: {}", token);
        try {
            refreshTokenRepository.deleteByToken(token);
            logger.info("Refresh token successfully deleted");
        } catch (Exception e) {
            logger.error("Error while deleting refresh token: {}", token, e);
            throw e;
        }
    }
}