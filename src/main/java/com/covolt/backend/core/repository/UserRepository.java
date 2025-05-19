package com.covolt.backend.core.repository;

import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.User; // Kendi User entity'niz
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository // Sende zaten bu annotasyon olmalı
public interface UserRepository extends JpaRepository<User, UUID> { // Veya PK tipinize göre UUID veya ne kullanıyorsanız onu yazın

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // **YENİ EKLENECEK METHOD SIGNATURE'I**
    boolean existsByEmailOrUsername(String email, String username); // Kullanıcı adı veya e-posta ile varlık kontrolü

    long countByRolesContains(Role role); // Kullanıcı sayısını rol ile sayma
}