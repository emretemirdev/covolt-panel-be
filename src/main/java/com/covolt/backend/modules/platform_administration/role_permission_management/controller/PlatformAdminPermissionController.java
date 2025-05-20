package com.covolt.backend.modules.platform_administration.role_permission_management.controller;

import com.covolt.backend.core.model.Permission;
import com.covolt.backend.modules.platform_administration.dto.PermissionRequestDto;
import com.covolt.backend.modules.platform_administration.role_permission_management.dto.PermissionResponseDto;
import com.covolt.backend.modules.platform_administration.role_permission_management.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/platform-admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_PLATFORM_ADMIN') and hasAuthority('MANAGE_PERMISSIONS')")
public class PlatformAdminPermissionController {

    private final PermissionService permissionService;
    // private final PermissionMapper permissionMapper; // DTO dönüşümü için (opsiyonel)

    @PostMapping
    public ResponseEntity<PermissionResponseDto> createPermission(@Valid @RequestBody PermissionRequestDto permissionDto) {
        Permission createdPermission = permissionService.createPermission(permissionDto);
        // DTO'ya dönüştürme (elle veya mapper ile)
        PermissionResponseDto responseDto = PermissionResponseDto.builder()
                .id(createdPermission.getId())
                .name(createdPermission.getName())
                .description(createdPermission.getDescription())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PermissionResponseDto> getPermissionById(@PathVariable UUID id) {
        return permissionService.getPermissionById(id)
                .map(p -> PermissionResponseDto.builder().id(p.getId()).name(p.getName()).description(p.getDescription()).build())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponseDto>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        List<PermissionResponseDto> responseDtos = permissions.stream()
                .map(p -> PermissionResponseDto.builder().id(p.getId()).name(p.getName()).description(p.getDescription()).build())
                .collect(Collectors.toList());
        return ResponseEntity.ok(responseDtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponseDto> updatePermission(@PathVariable UUID id, @Valid @RequestBody PermissionRequestDto permissionDto) {
        Permission updatedPermission = permissionService.updatePermission(id, permissionDto);
        PermissionResponseDto responseDto = PermissionResponseDto.builder()
                .id(updatedPermission.getId())
                .name(updatedPermission.getName())
                .description(updatedPermission.getDescription())
                .build();
        return ResponseEntity.ok(responseDto);
        // Not: Service katmanında ResourceNotFoundException veya DuplicateResourceException fırlatılırsa,
        // GlobalExceptionHandler bunları yakalayıp uygun HTTP yanıtını dönecektir.
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePermission(@PathVariable UUID id) {
        permissionService.deletePermission(id);
        return ResponseEntity.noContent().build();
        // Not: Service katmanında ResourceNotFoundException veya IllegalStateException (RPM_004) fırlatılırsa,
        // GlobalExceptionHandler bunları yakalayacaktır.
    }
}


