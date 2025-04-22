package com.covolt.backend.repository;

import com.covolt.backend.model.User; // Kendi User entity'niz
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // Eğer UUID kullanıyorsanız PK tipi olarak

@Repository // Sende zaten bu annotasyon olmalı
public interface UserRepository extends JpaRepository<User, Long> { // Veya PK tipinize göre UUID veya ne kullanıyorsanız onu yazın

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    // **YENİ EKLENECEK METHOD SIGNATURE'I**
    boolean existsByEmailOrUsername(String email, String username);
}