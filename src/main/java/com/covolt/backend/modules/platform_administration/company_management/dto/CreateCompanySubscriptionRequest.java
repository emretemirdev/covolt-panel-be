package com.covolt.backend.modules.platform_administration.company_management.dto;

import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request DTO for creating a new subscription for a company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCompanySubscriptionRequest {
    @NotNull(message = "Subscription plan ID cannot be null")
    private UUID planId;
    
    @NotNull(message = "Subscription status cannot be null")
    private UserSubscriptionStatus status;
    
    private Instant startDate;
    
    private Instant endDate;
    
    @Size(max = 500, message = "Notes must be at most 500 characters")
    private String notes;
}
