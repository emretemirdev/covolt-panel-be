package com.covolt.backend.modules.customer.company_profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for company users information visible to customers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyUsersDto {
    private UUID id;
    private String email;
    private String username;
    private String fullName;
    private String phoneNumber;
    private boolean enabled;
    private LocalDateTime createdAt;
    private List<String> roles;
}
