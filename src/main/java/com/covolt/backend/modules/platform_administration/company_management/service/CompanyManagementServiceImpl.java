package com.covolt.backend.modules.platform_administration.company_management.service;

import com.covolt.backend.core.exception.DuplicateRegistrationException;
import com.covolt.backend.core.exception.ResourceNotFoundException;
import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.CompanySubscription;
import com.covolt.backend.core.model.Role;
import com.covolt.backend.core.model.SubscriptionPlan;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import com.covolt.backend.core.repository.CompanyRepository;
import com.covolt.backend.core.repository.RoleRepository;
import com.covolt.backend.core.repository.SubscriptionPlanRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.modules.platform_administration.company_management.dto.*;
import com.covolt.backend.service.CompanySubscriptionService;
import com.covolt.backend.service.email.EmailService;
import com.covolt.backend.service.email.dto.PasswordResetEmailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CompanyManagementService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyManagementServiceImpl implements CompanyManagementService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CompanySubscriptionService companySubscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Retrieves a paginated list of companies filtered by optional status, name, and type.
     *
     * Filters are applied only if provided and valid; invalid status or type filters are ignored.
     * Returns companies as {@link CompanyDto} objects.
     *
     * @param statusStr optional company status filter as a string
     * @param name optional company name filter (partial match, case-insensitive)
     * @param typeStr optional company type filter as a string
     * @param pageable pagination and sorting information
     * @return a page of companies matching the provided filters
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CompanyDto> getAllCompanies(String statusStr, String name, String typeStr, Pageable pageable) {
        log.debug("Getting all companies with filters - status: {}, name: {}, type: {}", statusStr, name, typeStr);

        Specification<Company> spec = Specification.where(null);

        if (statusStr != null && !statusStr.isEmpty()) {
            try {
                CompanyStatus status = CompanyStatus.valueOf(statusStr.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid company status filter: {}", statusStr);
            }
        }

        if (name != null && !name.isEmpty()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
        }

        if (typeStr != null && !typeStr.isEmpty()) {
            try {
                CompanyType type = CompanyType.valueOf(typeStr.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid company type filter: {}", typeStr);
            }
        }

        Page<Company> companies = companyRepository.findAll(spec, pageable);
        log.debug("Found {} companies matching the criteria", companies.getTotalElements());

        return companies.map(this::mapToCompanyDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyDto getCompanyById(UUID companyId) {
        log.debug("Getting company by ID: {}", companyId);
        Company company = findCompanyById(companyId);
        return mapToCompanyDto(company);
    }

    @Override
    @Transactional
    public CompanyDto createCompany(CreateCompanyRequest request) {
        log.debug("Creating new company: {}", request.getName());

        Company company = Company.builder()
                .name(request.getName())
                .identifier(request.getIdentifier())
                .type(request.getType())
                .status(CompanyStatus.ACTIVE) // Default to active
                .address(request.getAddress())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .build();

        Company savedCompany = companyRepository.save(company);
        log.info("Created new company with ID: {}", savedCompany.getId());

        // Start trial subscription
        companySubscriptionService.startTrial(savedCompany);
        log.debug("Started trial subscription for company: {}", savedCompany.getId());

        return mapToCompanyDto(savedCompany);
    }

    @Override
    @Transactional
    public CompanyDto updateCompany(UUID companyId, UpdateCompanyRequest request) {
        log.debug("Updating company with ID: {}", companyId);

        Company company = findCompanyById(companyId);

        if (request.getName() != null) {
            company.setName(request.getName());
        }

        if (request.getIdentifier() != null) {
            company.setIdentifier(request.getIdentifier());
        }

        if (request.getType() != null) {
            company.setType(request.getType());
        }

        if (request.getAddress() != null) {
            company.setAddress(request.getAddress());
        }

        if (request.getContactEmail() != null) {
            company.setContactEmail(request.getContactEmail());
        }

        if (request.getContactPhone() != null) {
            company.setContactPhone(request.getContactPhone());
        }

        Company updatedCompany = companyRepository.save(company);
        log.info("Updated company with ID: {}", updatedCompany.getId());

        return mapToCompanyDto(updatedCompany);
    }

    @Override
    @Transactional
    public CompanyDto updateCompanyStatus(UUID companyId, UpdateCompanyStatusRequest request) {
        log.debug("Updating status for company with ID: {} to {}", companyId, request.getStatus());

        Company company = findCompanyById(companyId);

        company.setStatus(request.getStatus());

        Company updatedCompany = companyRepository.save(company);
        log.info("Updated status for company with ID: {} to {}", updatedCompany.getId(), updatedCompany.getStatus());

        return mapToCompanyDto(updatedCompany);
    }

    @Override
    @Transactional
    public CompanyDto createCompanySubscription(UUID companyId, CreateCompanySubscriptionRequest request) {
        log.debug("Creating subscription for company with ID: {}", companyId);

        Company company = findCompanyById(companyId);

        SubscriptionPlan plan = subscriptionPlanRepository.findById(request.getPlanId())
                .orElseThrow(() -> {
                    log.error("Subscription plan not found with ID: {}", request.getPlanId());
                    return new ResourceNotFoundException("Subscription plan not found with ID: " + request.getPlanId());
                });

        CompanySubscription subscription = CompanySubscription.builder()
                .company(company)
                .plan(plan)
                .status(request.getStatus())
                .startDate(request.getStartDate() != null ? request.getStartDate() : Instant.now())
                .endDate(request.getEndDate())
                .build();

        company.getCompanySubscriptions().add(subscription);

        Company updatedCompany = companyRepository.save(company);
        log.info("Created subscription for company with ID: {}", updatedCompany.getId());

        return mapToCompanyDto(updatedCompany);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> getCompanyUsers(UUID companyId, Pageable pageable) {
        log.debug("Getting users for company with ID: {}", companyId);

        Company company = findCompanyById(companyId);

        Page<User> users = userRepository.findByCompany(company, pageable);
        log.debug("Found {} users for company with ID: {}", users.getTotalElements(), companyId);

        return users.map(this::mapToUserDto);
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyStatisticsDto getCompanyStatistics() {
        log.debug("Getting company statistics");

        long totalCompanies = companyRepository.count();
        long activeCompanies = companyRepository.countByStatus(CompanyStatus.ACTIVE);
        long suspendedCompanies = companyRepository.countByStatus(CompanyStatus.SUSPENDED);
        long pendingVerificationCompanies = companyRepository.countByStatus(CompanyStatus.PENDING_VERIFICATION);
        long deletedCompanies = companyRepository.countByStatus(CompanyStatus.DELETED);

        long totalUsers = userRepository.count();
        double averageUsersPerCompany = totalCompanies > 0 ? (double) totalUsers / totalCompanies : 0;

        // Subscription statistics
        long companiesWithActiveSubscription = companyRepository.countCompaniesWithSubscriptionStatus(UserSubscriptionStatus.ACTIVE);
        long companiesWithTrialSubscription = companyRepository.countCompaniesWithSubscriptionStatus(UserSubscriptionStatus.TRIAL);
        long companiesWithExpiredSubscription = companyRepository.countCompaniesWithSubscriptionStatus(UserSubscriptionStatus.EXPIRED);

        log.debug("Calculated company statistics - total: {}, active: {}, suspended: {}",
                totalCompanies, activeCompanies, suspendedCompanies);

        return CompanyStatisticsDto.builder()
                .totalCompanies(totalCompanies)
                .activeCompanies(activeCompanies)
                .suspendedCompanies(suspendedCompanies)
                .pendingVerificationCompanies(pendingVerificationCompanies)
                .deletedCompanies(deletedCompanies)
                .totalUsers(totalUsers)
                .averageUsersPerCompany(averageUsersPerCompany)
                .companiesWithActiveSubscription(companiesWithActiveSubscription)
                .companiesWithTrialSubscription(companiesWithTrialSubscription)
                .companiesWithExpiredSubscription(companiesWithExpiredSubscription)
                .build();
    }

    @Override
    public Company findCompanyById(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company not found with ID: {}", companyId);
                    return new ResourceNotFoundException("Company not found with ID: " + companyId);
                });
    }

    /**
     * Maps a Company entity to a CompanyDto
     */
    private CompanyDto mapToCompanyDto(Company company) {
        // Get user count for the company
        long userCount = userRepository.countByCompany(company);

        // Get active subscription for the company
        final String[] subscriptionPlanRef = {"None"};
        final String[] subscriptionStatusRef = {"None"};
        final Instant[] subscriptionEndDateRef = {null};

        companySubscriptionService.getCurrentActiveSubscription(company)
                .ifPresent(subscription -> {
                    if (subscription.getPlan() != null) {
                        subscriptionPlanRef[0] = subscription.getPlan().getName();
                    }
                    subscriptionStatusRef[0] = subscription.getStatus().name();
                    if (subscription.getStatus() == UserSubscriptionStatus.TRIAL) {
                        subscriptionEndDateRef[0] = subscription.getTrialEndDate();
                    } else {
                        subscriptionEndDateRef[0] = subscription.getEndDate();
                    }
                });

        return CompanyDto.builder()
                .id(company.getId())
                .name(company.getName())
                .identifier(company.getIdentifier())
                .type(company.getType())
                .status(company.getStatus())
                .address(company.getAddress())
                .contactEmail(company.getContactEmail())
                .contactPhone(company.getContactPhone())
                .createdAt(company.getCreatedAt())
                .updatedAt(company.getUpdatedAt())
                .userCount((int) userCount)
                .subscriptionPlan(subscriptionPlanRef[0])
                .subscriptionStatus(subscriptionStatusRef[0])
                .subscriptionEndDate(subscriptionEndDateRef[0])
                .build();
    }

    /**
     * Converts a User entity to a UserDto containing basic user information.
     *
     * @param user the User entity to convert
     * @return a UserDto with the user's ID, email, username, full name, phone number, enabled and locked status, and creation date
     */
    private UserDto mapToUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Adds a new user to a specified company, assigning roles if provided and optionally sending a welcome email.
     *
     * Throws a DuplicateRegistrationException if a user with the given email or username already exists.
     *
     * @param companyId the ID of the company to which the user will be added
     * @param request the details of the user to add, including email, username, password, roles, and email preferences
     * @return a response containing details of the user addition operation
     */

    @Override
    @Transactional
    public UserOperationResponse addUserToCompany(UUID companyId, AddUserToCompanyRequest request) {
        log.debug("Adding user to company - companyId: {}, email: {}", companyId, request.getEmail());

        Company company = findCompanyById(companyId);

        // Check if user already exists
        if (userRepository.existsByEmailOrUsername(request.getEmail(), request.getUsername())) {
            log.warn("User already exists with email: {} or username: {}", request.getEmail(), request.getUsername());
            throw new DuplicateRegistrationException("User already exists with this email or username");
        }

        // Create new user
        User newUser = User.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword())) // Encode password
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .enabled(request.isEnabled())
                .locked(request.isLocked())
                .company(company)
                .build();

        // Assign roles if provided
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (UUID roleId : request.getRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
                roles.add(role);
            }
            newUser.setRoles(roles);
        }

        User savedUser = userRepository.save(newUser);
        log.info("Successfully added user {} to company {}", savedUser.getEmail(), company.getName());

        // Send welcome email if requested
        if (request.isSendWelcomeEmail()) {
            try {
                emailService.sendWelcomeEmail(
                        savedUser.getEmail(),
                        savedUser.getFullName(),
                        company.getName(),
                        request.getPassword() // Send original password in email
                );
            } catch (Exception e) {
                log.warn("Failed to send welcome email to user: {}", savedUser.getEmail(), e);
            }
        }

        return UserOperationResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .username(savedUser.getUsername())
                .fullName(savedUser.getFullName())
                .operation("ADDED")
                .status("SUCCESS")
                .message("User successfully added to company")
                .operationTime(LocalDateTime.now())
                .companyId(company.getId())
                .companyName(company.getName())
                .build();
    }

    /**
     * Removes a user from a specified company by dissociating the user and disabling their account.
     *
     * @param companyId the ID of the company from which the user will be removed
     * @param userId the ID of the user to remove
     * @return a response containing details of the removal operation
     * @throws ResourceNotFoundException if the user does not exist
     * @throws IllegalArgumentException if the user does not belong to the specified company
     */
    @Override
    @Transactional
    public UserOperationResponse removeUserFromCompany(UUID companyId, UUID userId) {
        log.debug("Removing user from company - companyId: {}, userId: {}", companyId, userId);

        Company company = findCompanyById(companyId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify user belongs to the company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("User does not belong to the specified company");
        }

        // Remove user (soft delete by setting company to null or hard delete)
        // For this implementation, we'll set company to null (soft removal)
        user.setCompany(null);
        user.setEnabled(false); // Disable the user

        User updatedUser = userRepository.save(user);
        log.info("Successfully removed user {} from company {}", user.getEmail(), company.getName());

        return UserOperationResponse.builder()
                .userId(updatedUser.getId())
                .email(updatedUser.getEmail())
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .operation("REMOVED")
                .status("SUCCESS")
                .message("User successfully removed from company")
                .operationTime(LocalDateTime.now())
                .previousCompanyId(companyId)
                .previousCompanyName(company.getName())
                .build();
    }

    /**
     * Transfers a user from their current company to a specified target company, updating company association and roles as requested.
     *
     * If new roles are provided, assigns them to the user, either replacing or adding to existing roles based on the request. Optionally sends a transfer notification email to the user. Throws an exception if the user, target company, or any specified role does not exist, or if the user is not currently associated with a company.
     *
     * @param userId the ID of the user to transfer
     * @param request the transfer details, including target company, roles, and notification preferences
     * @return a response containing details of the transfer operation
     */
    @Override
    @Transactional
    public UserOperationResponse transferUser(UUID userId, TransferUserRequest request) {
        log.debug("Transferring user - userId: {}, targetCompanyId: {}", userId, request.getTargetCompanyId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        Company sourceCompany = user.getCompany();
        if (sourceCompany == null) {
            throw new IllegalStateException("User is not associated with any company");
        }

        Company targetCompany = findCompanyById(request.getTargetCompanyId());

        // Transfer user to new company
        user.setCompany(targetCompany);

        // Handle roles
        if (request.getNewRoleIds() != null && !request.getNewRoleIds().isEmpty()) {
            Set<Role> newRoles = new HashSet<>();
            for (UUID roleId : request.getNewRoleIds()) {
                Role role = roleRepository.findById(roleId)
                        .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
                newRoles.add(role);
            }

            if (request.isKeepCurrentRoles()) {
                user.getRoles().addAll(newRoles);
            } else {
                user.setRoles(newRoles);
            }
        } else if (!request.isKeepCurrentRoles()) {
            user.getRoles().clear();
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully transferred user {} from company {} to company {}",
                user.getEmail(), sourceCompany.getName(), targetCompany.getName());

        // Send transfer notification email if requested
        if (request.isNotifyUser()) {
            try {
                emailService.sendUserTransferNotification(
                        user.getEmail(),
                        user.getFullName(),
                        sourceCompany.getName(),
                        targetCompany.getName(),
                        request.getTransferReason()
                );
            } catch (Exception e) {
                log.warn("Failed to send transfer notification email to user: {}", user.getEmail(), e);
            }
        }

        return UserOperationResponse.builder()
                .userId(updatedUser.getId())
                .email(updatedUser.getEmail())
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .operation("TRANSFERRED")
                .status("SUCCESS")
                .message("User successfully transferred to new company")
                .operationTime(LocalDateTime.now())
                .companyId(targetCompany.getId())
                .companyName(targetCompany.getName())
                .previousCompanyId(sourceCompany.getId())
                .previousCompanyName(sourceCompany.getName())
                .details(Map.of("transferReason", request.getTransferReason() != null ? request.getTransferReason() : ""))
                .build();
    }

    /**
     * Updates the roles of a user within a specified company, either replacing or adding to existing roles based on the request.
     *
     * If requested, sends a notification email to the user about the role update.
     *
     * @param companyId the ID of the company to which the user belongs
     * @param userId the ID of the user whose roles are to be updated
     * @param request the role update request containing role IDs, replacement flag, notification flag, and optional change reason
     * @return a response detailing the outcome of the user role update operation
     * @throws ResourceNotFoundException if the company, user, or any specified role is not found
     * @throws IllegalArgumentException if the user does not belong to the specified company
     */
    @Override
    @Transactional
    public UserOperationResponse updateUserRoles(UUID companyId, UUID userId, UpdateUserRolesRequest request) {
        log.debug("Updating user roles - companyId: {}, userId: {}", companyId, userId);

        Company company = findCompanyById(companyId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify user belongs to the company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("User does not belong to the specified company");
        }

        // Get new roles
        Set<Role> newRoles = new HashSet<>();
        for (UUID roleId : request.getRoleIds()) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found with ID: " + roleId));
            newRoles.add(role);
        }

        // Update user roles
        if (request.isReplaceExistingRoles()) {
            user.setRoles(newRoles);
        } else {
            user.getRoles().addAll(newRoles);
        }

        User updatedUser = userRepository.save(user);
        log.info("Successfully updated roles for user {} in company {}", user.getEmail(), company.getName());

        // Send role update notification email if requested
        if (request.isNotifyUser()) {
            try {
                String roleNames = newRoles.stream()
                        .map(Role::getName)
                        .collect(Collectors.joining(", "));

                emailService.sendRoleUpdateNotification(
                        user.getEmail(),
                        user.getFullName(),
                        company.getName(),
                        roleNames,
                        request.getChangeReason()
                );
            } catch (Exception e) {
                log.warn("Failed to send role update notification email to user: {}", user.getEmail(), e);
            }
        }

        return UserOperationResponse.builder()
                .userId(updatedUser.getId())
                .email(updatedUser.getEmail())
                .username(updatedUser.getUsername())
                .fullName(updatedUser.getFullName())
                .operation("ROLES_UPDATED")
                .status("SUCCESS")
                .message("User roles successfully updated")
                .operationTime(LocalDateTime.now())
                .companyId(company.getId())
                .companyName(company.getName())
                .details(Map.of("changeReason", request.getChangeReason() != null ? request.getChangeReason() : ""))
                .build();
    }

    /**
     * Resets the password for a user within a specified company and optionally sends a notification email.
     *
     * If requested, marks the user to force a password change on next login (pending implementation).
     * Returns a response indicating the outcome of the operation, including status, metadata, and email notification details.
     *
     * @param companyId the unique identifier of the company to which the user belongs
     * @param userId the unique identifier of the user whose password is being reset
     * @param request the password reset request containing the new password, notification, and force change options
     * @return a {@link PasswordResetResponse} detailing the result of the password reset operation
     *
     * @throws IllegalArgumentException if the user does not belong to the specified company
     * @throws ResourceNotFoundException if the company or user is not found
     */
    @Override
    @Transactional
    public PasswordResetResponse resetUserPassword(UUID companyId, UUID userId, PasswordResetRequest request) {
        log.debug("Resetting password for user - companyId: {}, userId: {}", companyId, userId);

        Company company = findCompanyById(companyId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify user belongs to the company
        if (!user.getCompany().getId().equals(companyId)) {
            throw new IllegalArgumentException("User does not belong to the specified company");
        }

        try {
            // Generate new password and encode it
            String newPassword = request.getNewPassword();
            String encodedPassword = passwordEncoder.encode(newPassword);

            // Update user password
            user.setPassword(encodedPassword);

            // Set force password change if requested
            if (request.isForcePasswordChange()) {
                // You might want to add a field to User entity for this
                // user.setMustChangePassword(true);
            }

            User updatedUser = userRepository.save(user);

            // Send email notification if requested
            boolean emailSent = false;
            if (request.isSendNotification()) {
                try {
                    PasswordResetEmailDto emailDto = PasswordResetEmailDto.builder()
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .resetToken("N/A") // Not using token-based reset for admin reset
                            .resetUrl("https://app.covolt.com/login") // Direct to login page
                            .requestTime(LocalDateTime.now())
                            .expirationMinutes(0) // Immediate reset
                            .companyName(company.getName())
                            .requestedBy("Platform Admin")
                            .build();

                    emailSent = emailService.sendPasswordResetEmail(emailDto);
                } catch (Exception e) {
                    log.warn("Failed to send password reset email to user: {}", user.getEmail(), e);
                }
            }

            log.info("Successfully reset password for user {} in company {}", user.getEmail(), company.getName());

            return PasswordResetResponse.builder()
                    .userId(updatedUser.getId())
                    .email(updatedUser.getEmail())
                    .username(updatedUser.getUsername())
                    .fullName(updatedUser.getFullName())
                    .operation("PASSWORD_RESET")
                    .status("SUCCESS")
                    .message("Password successfully reset")
                    .operationTime(LocalDateTime.now())
                    .companyId(company.getId())
                    .companyName(company.getName())
                    .emailSent(emailSent)
                    .forcePasswordChange(request.isForcePasswordChange())
                    .details(Map.of(
                            "resetReason", request.getResetReason() != null ? request.getResetReason() : "",
                            "emailNotificationRequested", request.isSendNotification(),
                            "emailSent", emailSent
                    ))
                    .build();

        } catch (Exception e) {
            log.error("Failed to reset password for user {} in company {}", user.getEmail(), company.getName(), e);

            return PasswordResetResponse.builder()
                    .userId(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .fullName(user.getFullName())
                    .operation("PASSWORD_RESET")
                    .status("FAILED")
                    .message("Failed to reset password: " + e.getMessage())
                    .operationTime(LocalDateTime.now())
                    .companyId(company.getId())
                    .companyName(company.getName())
                    .emailSent(false)
                    .forcePasswordChange(false)
                    .details(Map.of("error", e.getMessage()))
                    .build();
        }
    }
}
