package com.covolt.backend.modules.platform_administration.company_management.dto;

import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for company information including subscription details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDto {
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

    // Statistics and additional information
    private Integer userCount;
    private String subscriptionPlan;
    private String subscriptionStatus;
    private Instant subscriptionEndDate;
}
