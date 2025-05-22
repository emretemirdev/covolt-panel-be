package com.covolt.backend.modules.customer.company_profile.controller;

import com.covolt.backend.modules.customer.company_profile.dto.*;
import com.covolt.backend.modules.customer.company_profile.service.CustomerCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for customer company profile operations
 */
@RestController
@RequestMapping("/api/v1/customer/company")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Customer - Company Profile", description = "APIs for customers to manage their own company information")
@Slf4j
public class CustomerCompanyController {

    private final CustomerCompanyService customerCompanyService;

    @GetMapping("/profile")
    @Operation(summary = "Get my company profile", description = "Retrieves the current user's company profile information")
    public ResponseEntity<CompanyProfileDto> getMyCompanyProfile() {
        log.debug("REST request to get current user's company profile");
        return ResponseEntity.ok(customerCompanyService.getMyCompanyProfile());
    }

    @PutMapping("/profile")
    @Operation(summary = "Update my company profile", description = "Updates the current user's company profile information")
    public ResponseEntity<CompanyProfileDto> updateMyCompanyProfile(
            @Valid @RequestBody UpdateCompanyProfileRequest request) {
        log.debug("REST request to update current user's company profile");
        return ResponseEntity.ok(customerCompanyService.updateMyCompanyProfile(request));
    }

    @GetMapping("/users")
    @Operation(summary = "Get my company users", description = "Retrieves all users in the current user's company")
    public ResponseEntity<Page<CompanyUsersDto>> getMyCompanyUsers(Pageable pageable) {
        log.debug("REST request to get current user's company users");
        return ResponseEntity.ok(customerCompanyService.getMyCompanyUsers(pageable));
    }

    @GetMapping("/subscription")
    @Operation(summary = "Get my company subscription", description = "Retrieves the current user's company subscription information")
    public ResponseEntity<CompanySubscriptionDto> getMyCompanySubscription() {
        log.debug("REST request to get current user's company subscription");
        return ResponseEntity.ok(customerCompanyService.getMyCompanySubscription());
    }
}
