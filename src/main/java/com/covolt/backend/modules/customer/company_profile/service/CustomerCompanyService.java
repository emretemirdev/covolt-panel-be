package com.covolt.backend.modules.customer.company_profile.service;

import com.covolt.backend.modules.customer.company_profile.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for customer company profile operations
 */
public interface CustomerCompanyService {

    /**
     * Get current user's company profile
     * 
     * @return Company profile DTO
     */
    CompanyProfileDto getMyCompanyProfile();
    
    /**
     * Update current user's company profile
     * 
     * @param request Company profile update request
     * @return Updated company profile DTO
     */
    CompanyProfileDto updateMyCompanyProfile(UpdateCompanyProfileRequest request);
    
    /**
     * Get current user's company users
     * 
     * @param pageable Pagination information
     * @return Page of company users DTOs
     */
    Page<CompanyUsersDto> getMyCompanyUsers(Pageable pageable);
    
    /**
     * Get current user's company subscription information
     * 
     * @return Company subscription DTO
     */
    CompanySubscriptionDto getMyCompanySubscription();
    
    /**
     * Get current authenticated user's company ID
     * 
     * @return Company ID
     */
    UUID getCurrentUserCompanyId();
}
