package com.covolt.backend.repository;

import java.util.UUID;
import com.covolt.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {
}
