package com.covolt.backend.modules.platform_administration.company_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating user roles in a company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRolesRequest {
    
    @NotNull(message = "Role IDs are required")
    private List<UUID> roleIds;
    
    // Whether to replace all roles or add to existing ones
    @Builder.Default
    private boolean replaceExistingRoles = true;
    
    // Reason for role change (for audit purposes)
    private String changeReason;
    
    // Whether to send notification to the user about role changes
    @Builder.Default
    private boolean notifyUser = true;
}
