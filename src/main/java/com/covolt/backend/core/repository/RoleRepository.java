package com.covolt.backend.core.repository; // Paket yolunu kontrol et

import com.covolt.backend.core.model.Permission;
import com.covolt.backend.core.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID; // UUID importu

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> { // <--- BURASI UUID OLMALI

    Optional<Role> findByName(String name);
    long countByPermissionsContains(Permission permission);

    // Diğer özel sorgu metodların varsa burada kalabilir
}