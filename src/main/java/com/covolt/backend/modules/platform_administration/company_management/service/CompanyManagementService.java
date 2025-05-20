package com.covolt.backend.modules.platform_administration.company_management.service;

import com.covolt.backend.core.model.Company;
import com.covolt.backend.modules.platform_administration.company_management.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for company management operations
 */
public interface CompanyManagementService {

    /**
     * Get all companies with optional filtering
     * 
     * @param status Optional status filter
     * @param name Optional name filter (partial match)
     * @param type Optional type filter
     * @param pageable Pagination information
     * @return Page of company DTOs
     */
    Page<CompanyDto> getAllCompanies(String status, String name, String type, Pageable pageable);
    
    /**
     * Get company by ID
     * 
     * @param companyId Company ID
     * @return Company DTO
     */
    CompanyDto getCompanyById(UUID companyId);
    
    /**
     * Create a new company
     * 
     * @param request Company creation request
     * @return Created company DTO
     */
    CompanyDto createCompany(CreateCompanyRequest request);
    
    /**
     * Update an existing company
     * 
     * @param companyId Company ID
     * @param request Company update request
     * @return Updated company DTO
     */
    CompanyDto updateCompany(UUID companyId, UpdateCompanyRequest request);
    
    /**
     * Update company status
     * 
     * @param companyId Company ID
     * @param request Status update request
     * @return Updated company DTO
     */
    CompanyDto updateCompanyStatus(UUID companyId, UpdateCompanyStatusRequest request);
    
    /**
     * Create a subscription for a company
     * 
     * @param companyId Company ID
     * @param request Subscription creation request
     * @return Updated company DTO
     */
    CompanyDto createCompanySubscription(UUID companyId, CreateCompanySubscriptionRequest request);
    
    /**
     * Get users for a company
     * 
     * @param companyId Company ID
     * @param pageable Pagination information
     * @return Page of user DTOs
     */
    Page<UserDto> getCompanyUsers(UUID companyId, Pageable pageable);
    
    /**
     * Get company statistics
     * 
     * @return Company statistics DTO
     */
    CompanyStatisticsDto getCompanyStatistics();
    
    /**
     * Find company by ID or throw exception
     * 
     * @param companyId Company ID
     * @return Company entity
     */
    Company findCompanyById(UUID companyId);
}
