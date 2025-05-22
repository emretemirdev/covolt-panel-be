package com.covolt.backend.modules.platform_administration.company_management.controller;

import com.covolt.backend.modules.platform_administration.company_management.dto.*;
import com.covolt.backend.modules.platform_administration.company_management.service.CompanyManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller for company management operations in the platform administration module
 */
@RestController
@RequestMapping("/api/v1/platform-admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('MANAGE_COMPANIES')")
@Tag(name = "Platform Admin - Company Management", description = "APIs for managing companies in the platform")
@Slf4j
public class PlatformAdminCompanyController {

    private final CompanyManagementService companyManagementService;

    @GetMapping
    @Operation(summary = "Get all companies", description = "Retrieves all companies with optional filtering and pagination")
    public ResponseEntity<Page<CompanyDto>> getAllCompanies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        log.debug("REST request to get all companies with filters - status: {}, name: {}, type: {}", status, name, type);
        return ResponseEntity.ok(companyManagementService.getAllCompanies(status, name, type, pageable));
    }

    @GetMapping("/{companyId}")
    @Operation(summary = "Get company by ID", description = "Retrieves a specific company by its ID")
    public ResponseEntity<CompanyDto> getCompanyById(@PathVariable UUID companyId) {
        log.debug("REST request to get company with ID: {}", companyId);
        return ResponseEntity.ok(companyManagementService.getCompanyById(companyId));
    }

    @PostMapping
    @Operation(summary = "Create company", description = "Creates a new company")
    public ResponseEntity<CompanyDto> createCompany(@Valid @RequestBody CreateCompanyRequest request) {
        log.debug("REST request to create a new company: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(companyManagementService.createCompany(request));
    }

    @PutMapping("/{companyId}")
    @Operation(summary = "Update company", description = "Updates an existing company")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyRequest request) {
        log.debug("REST request to update company with ID: {}", companyId);
        return ResponseEntity.ok(companyManagementService.updateCompany(companyId, request));
    }

    @PatchMapping("/{companyId}/status")
    @Operation(summary = "Update company status", description = "Updates the status of an existing company")
    public ResponseEntity<CompanyDto> updateCompanyStatus(
            @PathVariable UUID companyId,
            @Valid @RequestBody UpdateCompanyStatusRequest request) {
        log.debug("REST request to update status of company with ID: {} to {}", companyId, request.getStatus());
        return ResponseEntity.ok(companyManagementService.updateCompanyStatus(companyId, request));
    }

    @PostMapping("/{companyId}/subscriptions")
    @Operation(summary = "Create company subscription", description = "Creates a new subscription for a company")
    public ResponseEntity<CompanyDto> createCompanySubscription(
            @PathVariable UUID companyId,
            @Valid @RequestBody CreateCompanySubscriptionRequest request) {
        log.debug("REST request to create subscription for company with ID: {}", companyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyManagementService.createCompanySubscription(companyId, request));
    }

    @GetMapping("/{companyId}/users")
    @Operation(summary = "Get company users", description = "Retrieves all users for a specific company")
    public ResponseEntity<Page<UserDto>> getCompanyUsers(
            @PathVariable UUID companyId,
            Pageable pageable) {
        log.debug("REST request to get users for company with ID: {}", companyId);
        return ResponseEntity.ok(companyManagementService.getCompanyUsers(companyId, pageable));
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get company statistics", description = "Retrieves statistics about companies in the platform")
    public ResponseEntity<CompanyStatisticsDto> getCompanyStatistics() {
        log.debug("REST request to get company statistics");
        return ResponseEntity.ok(companyManagementService.getCompanyStatistics());
    }

    // === User Management Endpoints ===

    @PostMapping("/{companyId}/users")
    @PreAuthorize("hasAuthority('MANAGE_COMPANY_USERS')")
    @Operation(summary = "Add user to company", description = "Adds a new user to the specified company")
    public ResponseEntity<UserOperationResponse> addUserToCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody AddUserToCompanyRequest request) {
        log.debug("REST request to add user to company with ID: {}", companyId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyManagementService.addUserToCompany(companyId, request));
    }

    @DeleteMapping("/{companyId}/users/{userId}")
    @PreAuthorize("hasAuthority('MANAGE_COMPANY_USERS')")
    @Operation(summary = "Remove user from company", description = "Removes a user from the specified company")
    public ResponseEntity<UserOperationResponse> removeUserFromCompany(
            @PathVariable UUID companyId,
            @PathVariable UUID userId) {
        log.debug("REST request to remove user {} from company with ID: {}", userId, companyId);
        return ResponseEntity.ok(companyManagementService.removeUserFromCompany(companyId, userId));
    }

    @PatchMapping("/users/{userId}/transfer")
    @PreAuthorize("hasAuthority('MANAGE_COMPANY_USERS')")
    @Operation(summary = "Transfer user to another company", description = "Transfers a user from one company to another")
    public ResponseEntity<UserOperationResponse> transferUser(
            @PathVariable UUID userId,
            @Valid @RequestBody TransferUserRequest request) {
        log.debug("REST request to transfer user {} to company {}", userId, request.getTargetCompanyId());
        return ResponseEntity.ok(companyManagementService.transferUser(userId, request));
    }

    @PutMapping("/{companyId}/users/{userId}/roles")
    @PreAuthorize("hasAuthority('MANAGE_COMPANY_USERS')")
    @Operation(summary = "Update user roles", description = "Updates the roles of a user in the specified company")
    public ResponseEntity<UserOperationResponse> updateUserRoles(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        log.debug("REST request to update roles for user {} in company {}", userId, companyId);
        return ResponseEntity.ok(companyManagementService.updateUserRoles(companyId, userId, request));
    }

    @PostMapping("/{companyId}/users/{userId}/password-reset")
    @PreAuthorize("hasAuthority('MANAGE_COMPANY_USERS')")
    @Operation(summary = "Reset user password", description = "Resets the password of a user and optionally sends email notification")
    public ResponseEntity<PasswordResetResponse> resetUserPassword(
            @PathVariable UUID companyId,
            @PathVariable UUID userId,
            @Valid @RequestBody PasswordResetRequest request) {
        log.debug("REST request to reset password for user {} in company {}", userId, companyId);
        return ResponseEntity.ok(companyManagementService.resetUserPassword(companyId, userId, request));
    }
}
