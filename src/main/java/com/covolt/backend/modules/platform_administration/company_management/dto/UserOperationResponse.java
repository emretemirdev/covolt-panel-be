package com.covolt.backend.modules.platform_administration.company_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user operations (add, remove, transfer, etc.)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOperationResponse {
    
    private UUID userId;
    private String email;
    private String username;
    private String fullName;
    private String operation; // "ADDED", "REMOVED", "TRANSFERRED", "ROLES_UPDATED"
    private String status; // "SUCCESS", "FAILED", "PARTIAL"
    private String message;
    private LocalDateTime operationTime;
    
    // Company information
    private UUID companyId;
    private String companyName;
    
    // For transfer operations
    private UUID previousCompanyId;
    private String previousCompanyName;
    
    // Additional details
    private Object details; // Can contain operation-specific information
}
