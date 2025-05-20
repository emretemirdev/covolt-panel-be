package com.covolt.backend.core.repository;

import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByEmailOrUsername(String email, String username);

    long countByRolesContains(Role role);

    // Company management methods
    Page<User> findByCompany(Company company, Pageable pageable);

    long countByCompany(Company company);
}