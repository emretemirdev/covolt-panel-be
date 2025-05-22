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

    @Override
    @Transactional(readOnly = true)
    public CompanyProfileDto getMyCompanyProfile() {
        log.debug("Getting company profile for current user");

        UUID companyId = getCurrentUserCompanyId();
        Company company = findCompanyById(companyId);

        return mapToCompanyProfileDto(company);
    }

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

    private Company findCompanyById(UUID companyId) {
        return companyRepository.findById(companyId)
                .orElseThrow(() -> {
                    log.error("Company not found with ID: {}", companyId);
                    return new ResourceNotFoundException("Company not found with ID: " + companyId);
                });
    }

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
