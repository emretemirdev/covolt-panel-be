package com.covolt.backend.repository;

import com.covolt.backend.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> { // BaseEntity PK tipinize göre Long'u düzeltin

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    @Modifying
    void deleteByToken(String token); // Belirli token'ı sil

    @Transactional
    @Modifying
    void deleteByUsername(String username); // Kullanıcının tüm refresh tokenlarını sil (Single-use için)
}