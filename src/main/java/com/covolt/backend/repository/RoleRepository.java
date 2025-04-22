package com.covolt.backend.repository;

import com.covolt.backend.model.Role; // Kendi Role entity'niz
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
// import java.util.UUID; // Eğer UUID kullanıyorsanız PK tipi olarak

@Repository // Sende zaten bu annotasyon olmalı
public interface RoleRepository extends JpaRepository<Role, Long> { // Veya PK tipinize göre UUID veya ne kullanıyorsanız onu yazın

    // **YENİ EKLENECEK METHOD SIGNATURE'I**
    Optional<Role> findByName(String name);

    // ... varsa diğer metotlarınız
}