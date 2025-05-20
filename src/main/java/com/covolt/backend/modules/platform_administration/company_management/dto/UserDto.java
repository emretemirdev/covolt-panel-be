package com.covolt.backend.modules.platform_administration.company_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user information in company management context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String username;
    private String fullName;
    private String phoneNumber;
    private boolean enabled;
    private boolean locked;
    private LocalDateTime createdAt;
}
