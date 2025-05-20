package com.covolt.backend.modules.platform_administration.company_management.dto;

import com.covolt.backend.core.model.enums.CompanyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a company's status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyStatusRequest {
    @NotNull(message = "Company status cannot be null")
    private CompanyStatus status;
    
    @Size(max = 255, message = "Reason must be at most 255 characters")
    private String reason;
}
