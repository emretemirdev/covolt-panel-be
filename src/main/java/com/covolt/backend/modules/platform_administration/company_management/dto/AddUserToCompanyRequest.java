package com.covolt.backend.modules.platform_administration.company_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for adding a new user to a company
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddUserToCompanyRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3-50 characters")
    private String username;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6-100 characters")
    private String password;
    
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2-100 characters")
    private String fullName;
    
    @Size(max = 20, message = "Phone number must be at most 20 characters")
    private String phoneNumber;
    
    @Builder.Default
    private boolean enabled = true;
    
    @Builder.Default
    private boolean locked = false;
    
    // Role IDs to assign to the user
    private List<UUID> roleIds;
    
    // Whether to send welcome email to the user
    @Builder.Default
    private boolean sendWelcomeEmail = true;
}
