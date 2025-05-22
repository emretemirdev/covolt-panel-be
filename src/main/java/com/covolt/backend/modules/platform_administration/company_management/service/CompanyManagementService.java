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
 * Retrieves a paginated list of companies, optionally filtered by status, partial name match, and type.
 *
 * @param status optional filter for company status
 * @param name optional filter for company name (supports partial matches)
 * @param type optional filter for company type
 * @param pageable pagination and sorting information
 * @return a page of company data transfer objects matching the provided filters
 */
    Page<CompanyDto> getAllCompanies(String status, String name, String type, Pageable pageable);

    /**
 * Retrieves a company's details by its unique identifier.
 *
 * @param companyId the unique identifier of the company
 * @return the company's data transfer object
 */
    CompanyDto getCompanyById(UUID companyId);

    /**
 * Creates a new company using the provided creation request.
 *
 * @param request the details required to create the company
 * @return the DTO representing the newly created company
 */
    CompanyDto createCompany(CreateCompanyRequest request);

    /****
 * Updates the details of an existing company with the provided information.
 *
 * @param companyId the unique identifier of the company to update
 * @param request the data containing updated company information
 * @return the updated company data transfer object
 */
    CompanyDto updateCompany(UUID companyId, UpdateCompanyRequest request);

    /**
 * Updates the status of a specified company.
 *
 * @param companyId the unique identifier of the company to update
 * @param request the status update details
 * @return the updated company data transfer object
 */
    CompanyDto updateCompanyStatus(UUID companyId, UpdateCompanyStatusRequest request);

    /**
 * Creates a new subscription for the specified company.
 *
 * @param companyId the unique identifier of the company
 * @param request the subscription creation details
 * @return the updated company data including the new subscription
 */
    CompanyDto createCompanySubscription(UUID companyId, CreateCompanySubscriptionRequest request);

    /**
 * Retrieves a paginated list of users associated with the specified company.
 *
 * @param companyId the unique identifier of the company
 * @param pageable pagination and sorting information
 * @return a page of user DTOs belonging to the company
 */
    Page<UserDto> getCompanyUsers(UUID companyId, Pageable pageable);

    /**
 * Retrieves aggregated statistics about all companies.
 *
 * @return a DTO containing company statistics
 */
    CompanyStatisticsDto getCompanyStatistics();

    /**
 * Retrieves the company entity with the specified ID or throws an exception if not found.
 *
 * @param companyId the unique identifier of the company to retrieve
 * @return the corresponding Company entity
 */
    Company findCompanyById(UUID companyId);

    // === User Management Operations ===

    /**
 * Adds a new user to the specified company.
 *
 * @param companyId the unique identifier of the company
 * @param request the details required to create the new user
 * @return the result of the user addition operation
 */
    UserOperationResponse addUserToCompany(UUID companyId, AddUserToCompanyRequest request);

    /**
 * Removes a user from the specified company.
 *
 * @param companyId the unique identifier of the company
 * @param userId the unique identifier of the user to remove
 * @return the result of the user removal operation
 */
    UserOperationResponse removeUserFromCompany(UUID companyId, UUID userId);

    /**
 * Transfers a user to a different company based on the provided transfer request.
 *
 * @param userId the unique identifier of the user to transfer
 * @param request details of the transfer, including the target company
 * @return the result of the user transfer operation
 */
    UserOperationResponse transferUser(UUID userId, TransferUserRequest request);

    /**
 * Updates the roles assigned to a user within a specified company.
 *
 * @param companyId the unique identifier of the company
 * @param userId the unique identifier of the user whose roles are to be updated
 * @param request the details of the new roles to assign
 * @return the result of the user role update operation
 */
    UserOperationResponse updateUserRoles(UUID companyId, UUID userId, UpdateUserRolesRequest request);

    /**
 * Resets the password for a user within a specified company and sends a notification email.
 *
 * @param companyId the unique identifier of the company
 * @param userId the unique identifier of the user whose password will be reset
 * @param request the password reset request containing new password details
 * @return a response containing the result of the password reset operation
 */
    PasswordResetResponse resetUserPassword(UUID companyId, UUID userId, PasswordResetRequest request);
}
