package com.covolt.backend.modules.platform_administration.role_permission_management.controller;

import com.covolt.backend.core.model.Role;
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.RoleRequestDto;
// RoleResponseDto ve RoleMapper'ı da kullanacaksak importları eklenmeli
import com.covolt.backend.modules.platform_administration.role_permission_management.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List; // List için import eklendi
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform-admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_PLATFORM_ADMIN')")
public class PlatformAdminRoleController {

    private final RoleService roleService;
    // private final RoleMapper roleMapper; // DTO dönüşümü için

    @PostMapping
    public ResponseEntity<Role> createRole(@Valid @RequestBody RoleRequestDto roleDto) {
        Role createdRole = roleService.createRole(roleDto);
        // Eğer RoleResponseDto kullanıyorsak:
        // return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toResponseDto(createdRole));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable UUID id) {
        return roleService.getRoleById(id)
                .map(ResponseEntity::ok)
                // Eğer RoleResponseDto kullanıyorsak:
                // .map(role -> ResponseEntity.ok(roleMapper.toResponseDto(role)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.getAllRoles();
        // Eğer RoleResponseDto listesi döneceksek:
        // List<RoleResponseDto> roleDtos = roles.stream().map(roleMapper::toResponseDto).collect(Collectors.toList());
        // return ResponseEntity.ok(roleDtos);
        return ResponseEntity.ok(roles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Role> updateRole(@PathVariable UUID id, @Valid @RequestBody RoleRequestDto roleDto) {
        try {
            Role updatedRole = roleService.updateRole(id, roleDto);
            // return ResponseEntity.ok(roleMapper.toResponseDto(updatedRole));
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) { // Daha spesifik exception'lar da yakalanabilir (örn: ResourceNotFoundException)
            return ResponseEntity.notFound().build(); // Veya uygun bir hata DTO'su ile
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) { // Örn: RoleInUseException veya ResourceNotFoundException
            // return ResponseEntity.status(HttpStatus.CONFLICT).build(); // Eğer rol kullanımdaysa
            return ResponseEntity.notFound().build(); // Eğer rol bulunamadıysa
        }
    }

    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<Role> assignPermissionsToRole(@PathVariable UUID roleId, @RequestBody Set<UUID> permissionIds) {
        // Request body için ayrı bir DTO daha iyi olabilir: AssignPermissionsToRoleRequest { Set<UUID> permissionIds; }
        try {
            Role updatedRole = roleService.assignPermissionsToRole(roleId, permissionIds);
            // return ResponseEntity.ok(roleMapper.toResponseDto(updatedRole));
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) { // ResourceNotFoundException (rol veya izin bulunamadı)
            return ResponseEntity.notFound().build();
        }
    }

    // İzinleri rolden kaldırmak için @DeleteMapping veya @PutMapping kullanılabilir.
    // Tek bir izin kaldırma veya çoklu izin kaldırma için farklı endpointler de düşünülebilir.
    // Şimdilik Set<UUID> ile çoklu kaldıralım.
    @DeleteMapping("/{roleId}/permissions")
    public ResponseEntity<Role> removePermissionsFromRole(@PathVariable UUID roleId, @RequestBody Set<UUID> permissionIds) {
        try {
            Role updatedRole = roleService.removePermissionsFromRole(roleId, permissionIds);
            // return ResponseEntity.ok(roleMapper.toResponseDto(updatedRole));
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) { // ResourceNotFoundException
            return ResponseEntity.notFound().build();
        }
    }
}