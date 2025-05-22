package com.covolt.backend.modules.platform_administration.company_management.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for transferring a user to another company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferUserRequest {
    
    @NotNull(message = "Target company ID is required")
    private UUID targetCompanyId;
    
    // New role IDs to assign to the user in the target company
    private List<UUID> newRoleIds;
    
    // Whether to keep user's current roles (if compatible)
    @Builder.Default
    private boolean keepCurrentRoles = false;
    
    // Reason for transfer (for audit purposes)
    private String transferReason;
    
    // Whether to send notification to the user about the transfer
    @Builder.Default
    private boolean notifyUser = true;
}
