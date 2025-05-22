package com.covolt.backend.modules.customer.company_profile.service;

import com.covolt.backend.core.exception.ResourceNotFoundException;
import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.CompanySubscription;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import com.covolt.backend.core.repository.CompanyRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.modules.customer.company_profile.dto.*;
import com.covolt.backend.service.CompanySubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of CustomerCompanyService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerCompanyServiceImpl implements CustomerCompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanySubscriptionService companySubscriptionService;

    /**
     * Retrieves the profile of the company associated with the currently authenticated user.
     *
     * @return the company profile data for the current user
     */
    @Override
    @Transactional(readOnly = true)
    public CompanyProfileDto getMyCompanyProfile() {
        log.debug("Getting company profile for current user");

        UUID companyId = getCurrentUserCompanyId();
        Company company = findCompanyById(companyId);

        return mapToCompanyProfileDto(company);
    }

    /**
     * Updates the profile of the currently authenticated user's company with the provided fields.
     *
     * Only non-null fields in the request are applied to the company profile. Returns the updated company profile as a DTO.
     *
     * @param request the fields to update in the company profile
     * @return the updated company profile
     */
    @Override
    @Transactional
    public CompanyProfileDto updateMyCompanyProfile(UpdateCompanyProfileRequest request) {
        log.debug("Updating company profile for current user");

        UUID companyId = getCurrentUserCompanyId();
        Company company = findCompanyById(companyId);

        // Update company fields if provided
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
        log.info("Company profile updated for company ID: {}", companyId);

        return mapToCompanyProfileDto(updatedCompany);
    }

    /**
     * Retrieves a paginated list of users belonging to the currently authenticated user's company.
     *
     * @param pageable pagination and sorting information
     * @return a page of company user DTOs for the current user's company
     */
    @Override
    @Transactional(readOnly = true)
    public Page<CompanyUsersDto> getMyCompanyUsers(Pageable pageable) {
        log.debug("Getting company users for current user");

        UUID companyId = getCurrentUserCompanyId();
        Company company = findCompanyById(companyId);

        Page<User> users = userRepository.findByCompany(company, pageable);
        log.debug("Found {} users for company ID: {}", users.getTotalElements(), companyId);

        return users.map(this::mapToCompanyUsersDto);
    }

    /**
     * Retrieves the current active subscription for the authenticated user's company.
     *
     * @return the active company subscription as a {@link CompanySubscriptionDto}
     * @throws ResourceNotFoundException if no active subscription exists for the company
     */
    @Override
    @Transactional(readOnly = true)
    public CompanySubscriptionDto getMyCompanySubscription() {
        log.debug("Getting company subscription for current user");

        UUID companyId = getCurrentUserCompanyId();
        Company company = findCompanyById(companyId);

        return companySubscriptionService.getCurrentActiveSubscription(company)
                .map(this::mapToCompanySubscriptionDto)
                .orElseThrow(() -> {
                    log.warn("No active subscription found for company ID: {}", companyId);
                    return new ResourceNotFoundException("No active subscription found for your company");
                });
    }

    /**
     * Retrieves the company ID associated with the currently authenticated user.
     *
     * @return the UUID of the user's company
     * @throws AccessDeniedException if the user is not authenticated
     * @throws ResourceNotFoundException if the user or their company is not found
     */
    @Override
    public UUID getCurrentUserCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        String username = authentication.getName();
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (user.getCompany() == null) {
            throw new ResourceNotFoundException("User is not associated with any company");
        }

        return user.getCompany().getId();
    }

    /**
     * Retrieves a company entity by its ID or throws a ResourceNotFoundException if not found.
     *
     * @param companyId the unique identifier of the company to retrieve
     * @return the Company entity corresponding to the given ID
     * @throws ResourceNotFoundException if no company exists with the specified ID
     */
    private Company findCompanyById(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company not found with ID: {}", companyId);
                    return new ResourceNotFoundException("Company not found with ID: " + companyId);
                });
    }

    /**
     * Maps a {@link Company} entity to a {@link CompanyProfileDto}, including user count and current subscription details.
     *
     * @param company the company entity to map
     * @return a DTO containing company profile information, total users, and current subscription status
     */
    private CompanyProfileDto mapToCompanyProfileDto(Company company) {
        // Get user count for the company
        long userCount = userRepository.countByCompany(company);

        // Get active subscription for the company
        final String[] subscriptionPlanRef = {"None"};
        final String[] subscriptionStatusRef = {"None"};
        final LocalDateTime[] subscriptionEndDateRef = {null};
        final Boolean[] isTrialActiveRef = {false};

        companySubscriptionService.getCurrentActiveSubscription(company)
                .ifPresent(subscription -> {
                    if (subscription.getPlan() != null) {
                        subscriptionPlanRef[0] = subscription.getPlan().getName();
                    }
                    subscriptionStatusRef[0] = subscription.getStatus().name();

                    if (subscription.getStatus() == UserSubscriptionStatus.TRIAL) {
                        isTrialActiveRef[0] = subscription.isTrialActive();
                        if (subscription.getTrialEndDate() != null) {
                            subscriptionEndDateRef[0] = LocalDateTime.ofInstant(subscription.getTrialEndDate(), ZoneId.systemDefault());
                        }
                    } else {
                        if (subscription.getEndDate() != null) {
                            subscriptionEndDateRef[0] = LocalDateTime.ofInstant(subscription.getEndDate(), ZoneId.systemDefault());
                        }
                    }
                });

        return CompanyProfileDto.builder()
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
                .totalUsers((int) userCount)
                .currentSubscriptionPlan(subscriptionPlanRef[0])
                .subscriptionStatus(subscriptionStatusRef[0])
                .subscriptionEndDate(subscriptionEndDateRef[0])
                .isTrialActive(isTrialActiveRef[0])
                .build();
    }

    /**
     * Converts a User entity to a CompanyUsersDto, including user details and assigned roles.
     *
     * @param user the User entity to convert
     * @return a CompanyUsersDto containing the user's information and roles
     */
    private CompanyUsersDto mapToCompanyUsersDto(User user) {
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList());

        return CompanyUsersDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .build();
    }

    /**
     * Converts a {@link CompanySubscription} entity to a {@link CompanySubscriptionDto}, extracting plan details, features, status, dates, and calculated days remaining.
     *
     * @param subscription the company subscription entity to convert
     * @return a DTO containing subscription plan information, status, relevant dates, features, and flags indicating activity and trial status
     */
    private CompanySubscriptionDto mapToCompanySubscriptionDto(CompanySubscription subscription) {
        List<String> features = subscription.getPlan() != null && subscription.getPlan().getFeatures() != null
                ? subscription.getPlan().getFeatures().stream()
                    .collect(Collectors.toList())
                : List.of();

        Integer maxUsers = null; // SubscriptionPlan'da maxUsers field'ı yok, null olarak bırakıyoruz
        Boolean isActive = subscription.hasActiveAccess();
        Boolean isTrial = subscription.getStatus() == UserSubscriptionStatus.TRIAL;

        Long daysRemaining = null;
        if (subscription.getStatus() == UserSubscriptionStatus.TRIAL && subscription.getTrialEndDate() != null) {
            daysRemaining = Duration.between(Instant.now(), subscription.getTrialEndDate()).toDays();
        } else if (subscription.getEndDate() != null) {
            daysRemaining = Duration.between(Instant.now(), subscription.getEndDate()).toDays();
        }

        return CompanySubscriptionDto.builder()
                .id(subscription.getId())
                .planName(subscription.getPlan() != null ? subscription.getPlan().getName() : "Unknown")
                .planDescription(subscription.getPlan() != null ? subscription.getPlan().getDescription() : "No description")
                .status(subscription.getStatus())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .trialEndDate(subscription.getTrialEndDate())
                .createdAt(subscription.getCreatedAt())
                .features(features)
                .maxUsers(maxUsers)
                .isActive(isActive)
                .isTrial(isTrial)
                .daysRemaining(daysRemaining)
                .build();
    }
}
