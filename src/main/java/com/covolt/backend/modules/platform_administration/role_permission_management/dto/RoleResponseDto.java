package com.covolt.backend.modules.platform_administration.role_permission_management.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class RoleResponseDto {
    private UUID id;
    private String name;
    private String description;
    private Set<PermissionResponseDto> permissions; // İzinleri de göstermek için
    // private LocalDateTime createdAt;
    // private LocalDateTime updatedAt;
}