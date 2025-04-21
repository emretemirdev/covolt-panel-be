package com.covolt.backend.repository;

import com.covolt.backend.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
}
