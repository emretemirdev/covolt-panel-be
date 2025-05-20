package com.covolt.backend.modules.platform_administration.company_management.dto;

import com.covolt.backend.core.model.enums.CompanyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCompanyRequest {
    @Size(min = 2, max = 100, message = "Company name must be between 2-100 characters")
    private String name;
    
    @Size(max = 50, message = "Identifier must be at most 50 characters")
    private String identifier;
    
    private CompanyType type;
    
    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;
    
    @Email(message = "Please provide a valid email address")
    private String contactEmail;
    
    @Size(max = 20, message = "Contact phone must be at most 20 characters")
    private String contactPhone;
}
