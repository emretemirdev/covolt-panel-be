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
}
