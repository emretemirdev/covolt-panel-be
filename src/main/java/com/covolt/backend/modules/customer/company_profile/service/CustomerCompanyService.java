package com.covolt.backend.modules.customer.company_profile.service;

import com.covolt.backend.modules.customer.company_profile.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for customer company profile operations
 */
public interface CustomerCompanyService {

    /****
 * Retrieves the company profile associated with the currently authenticated user.
 *
 * @return the company profile data for the current user
 */
    CompanyProfileDto getMyCompanyProfile();
    
    /**
 * Updates the current user's company profile with the provided information.
 *
 * @param request the details to update in the company profile
 * @return the updated company profile
 */
    CompanyProfileDto updateMyCompanyProfile(UpdateCompanyProfileRequest request);
    
    /**
 * Retrieves a paginated list of users associated with the current user's company.
 *
 * @param pageable pagination and sorting information
 * @return a page of company user data transfer objects
 */
    Page<CompanyUsersDto> getMyCompanyUsers(Pageable pageable);
    
    /**
 * Retrieves the subscription details of the current user's company.
 *
 * @return the subscription information for the company associated with the authenticated user
 */
    CompanySubscriptionDto getMyCompanySubscription();
    
    /**
 * Retrieves the unique identifier of the company associated with the current authenticated user.
 *
 * @return the UUID of the current user's company
 */
    UUID getCurrentUserCompanyId();
}
