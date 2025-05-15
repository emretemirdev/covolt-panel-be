package com.covolt.backend.modules.platform_administration.role_permission_management.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;
// import java.time.LocalDateTime; // Eğer createdAt/updatedAt DTO'da gösterilecekse

@Data
@Builder
public class PermissionResponseDto {
    private UUID id;
    private String name;
    private String description;
    // private LocalDateTime createdAt; // İsteğe bağlı
    // private LocalDateTime updatedAt; // İsteğe bağlı
}