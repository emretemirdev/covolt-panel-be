package com.covolt.backend.modules.platform_administration.role_permission_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleRequestDto {

    @NotBlank(message = "Rol adı boş olamaz.")
    @Size(min = 2, max = 50, message = "Rol adı 2 ile 50 karakter arasında olmalıdır.")
    // Rol adlarının "ROLE_" ile başlamasını zorunlu kılmak isteyebiliriz veya servis katmanında bu prefix'i ekleyebiliriz.
    // Şimdilik, kullanıcı direkt "PLATFORM_ADMIN", "COMPANY_ADMIN" gibi girebilir, servis "ROLE_" ekleyebilir.
    // Veya DTO'da direkt "ROLE_PLATFORM_ADMIN" girmesini bekleyebiliriz.
    // @Pattern(regexp = "^ROLE_[A-Z_]+$", message = "Rol adı 'ROLE_' ile başlamalı ve sadece büyük harf ve alt çizgi içermelidir.")
    private String name;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir.")
    private String description;

    // Rol oluşturulurken veya güncellenirken bu role atanacak izinlerin ID'leri.
    // Bu alan opsiyonel olabilir (rolü izinsiz oluşturup sonra izin atamak gibi).
    private Set<UUID> permissionIds;
}