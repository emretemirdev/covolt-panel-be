package com.covolt.backend.repository;

import com.covolt.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // Eğer Company PK tipiniz UUID ise, BaseEntity'nizle aynı olmalı

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> { // PK tipini BaseEntity ile eşleştirin (Long veya UUID)

    // Firma adına göre bulma (unique index'imiz var, bu mantıklı bir metot)
    Optional<Company> findByNameIgnoreCase(String name); // Büyük/küçük harf duyarsız arama

    // Identifier'a göre bulma (opsiyonel)
    Optional<Company> findByIdentifier(String identifier);

    boolean existsByNameIgnoreCase(String name);
}