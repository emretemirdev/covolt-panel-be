package com.covolt.backend.core.repository;

import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID>, JpaSpecificationExecutor<Company> {

    // Firma adına göre bulma (unique index'imiz var, bu mantıklı bir metot)
    Optional<Company> findByNameIgnoreCase(String name); // Büyük/küçük harf duyarsız arama

    // Identifier'a göre bulma (opsiyonel)
    Optional<Company> findByIdentifier(String identifier);

    boolean existsByNameIgnoreCase(String name);

    // Company statistics methods
    long countByStatus(CompanyStatus status);

    @Query("SELECT COUNT(DISTINCT c) FROM Company c JOIN c.companySubscriptions cs WHERE cs.status = :status")
    long countCompaniesWithSubscriptionStatus(@Param("status") UserSubscriptionStatus status);
}