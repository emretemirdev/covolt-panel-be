package com.covolt.backend.core.repository;

import com.covolt.backend.core.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> { // BaseEntity PK tipinize göre Long'u düzeltin

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    @Modifying
    void deleteByToken(String token); // Belirli token'ı sil

    @Transactional
    @Modifying
    void deleteByUsername(String username); // Kullanıcının tüm refresh tokenlarını sil (Single-use için)
}