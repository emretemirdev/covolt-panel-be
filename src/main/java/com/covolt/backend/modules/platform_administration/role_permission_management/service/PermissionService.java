package com.covolt.backend.modules.platform_administration.role_permission_management.service;

import com.covolt.backend.core.model.Permission;
import com.covolt.backend.modules.platform_administration.dto.PermissionRequestDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PermissionService {
    Permission createPermission(PermissionRequestDto dto);
    Optional<Permission> getPermissionById(UUID id);
    Optional<Permission> getPermissionByName(String name);
    List<Permission> getAllPermissions();
    Permission updatePermission(UUID id, PermissionRequestDto dto);
    void deletePermission(UUID id); // Dikkat: Rollere atanmışsa silinmemeli!
    Set<Permission> findPermissionsByIds(Set<UUID> permissionIds);
}