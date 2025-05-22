package com.covolt.backend.modules.customer.company_profile.dto;

import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for customer's own company profile information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyProfileDto {
    private UUID id;
    private String name;
    private String identifier;
    private CompanyType type;
    private CompanyStatus status;
    private String address;
    private String contactEmail;
    private String contactPhone;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Company statistics visible to customers
    private Integer totalUsers;
    private String currentSubscriptionPlan;
    private String subscriptionStatus;
    private LocalDateTime subscriptionEndDate;
    private Boolean isTrialActive;
}
