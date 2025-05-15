package com.covolt.backend.modules.platform_administration.role_permission_management.service;

import com.covolt.backend.core.model.Role; // core.model.Role'u kullanıyoruz
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.RoleRequestDto;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RoleService {

    Role createRole(RoleRequestDto roleDto);

    Optional<Role> getRoleById(UUID id);

    Optional<Role> getRoleByName(String name); // Rol adına göre bulmak da faydalı

    List<Role> getAllRoles();

    Role updateRole(UUID roleId, RoleRequestDto roleDto);

    void deleteRole(UUID roleId); // Silme işleminin kuralları olmalı (kullanımda mı vs.)

    Role assignPermissionsToRole(UUID roleId, Set<UUID> permissionIds);

    Role removePermissionsFromRole(UUID roleId, Set<UUID> permissionIds);

    Set<Role> findRolesByIds(Set<UUID> roleIds); // User'a rol atarken kullanışlı olabilir
}