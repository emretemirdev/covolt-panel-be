package com.covolt.backend.modules.platform_administration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionRequestDto {
    @NotBlank(message = "İzin adı boş olamaz.")
    @Size(min = 3, max = 100, message = "İzin adı 3 ile 100 karakter arasında olmalıdır.")
    // İzin adları için bir format belirleyebiliriz (örn: SADECE_BUYUK_HARF_VE_ALTTIRE)
    @Pattern(regexp = "^[A-Z_]+$", message = "İzin adı sadece büyük harf ve alt çizgi içerebilir (örn: USER_CREATE).")
    private String name;

    @Size(max = 255, message = "Açıklama en fazla 255 karakter olabilir.")
    private String description;
}