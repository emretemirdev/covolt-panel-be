package com.covolt.backend.modules.customer.company_profile.dto;

import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for company subscription information visible to customers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanySubscriptionDto {
    private UUID id;
    private String planName;
    private String planDescription;
    private UserSubscriptionStatus status;
    private Instant startDate;
    private Instant endDate;
    private Instant trialEndDate;
    private LocalDateTime createdAt;
    
    // Plan features
    private List<String> features;
    private Integer maxUsers;
    private Boolean isActive;
    private Boolean isTrial;
    private Long daysRemaining;
}
