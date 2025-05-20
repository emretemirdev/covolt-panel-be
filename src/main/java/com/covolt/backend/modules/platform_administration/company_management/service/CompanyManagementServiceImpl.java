package com.covolt.backend.modules.platform_administration.company_management.service;

import com.covolt.backend.core.exception.ResourceNotFoundException;
import com.covolt.backend.core.model.Company;
import com.covolt.backend.core.model.CompanySubscription;
import com.covolt.backend.core.model.SubscriptionPlan;
import com.covolt.backend.core.model.User;
import com.covolt.backend.core.model.enums.CompanyStatus;
import com.covolt.backend.core.model.enums.CompanyType;
import com.covolt.backend.core.model.enums.UserSubscriptionStatus;
import com.covolt.backend.core.repository.CompanyRepository;
import com.covolt.backend.core.repository.SubscriptionPlanRepository;
import com.covolt.backend.core.repository.UserRepository;
import com.covolt.backend.modules.platform_administration.company_management.dto.*;
import com.covolt.backend.service.CompanySubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.UUID;

/**
 * Implementation of CompanyManagementService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyManagementServiceImpl implements CompanyManagementService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final CompanySubscriptionService companySubscriptionService;

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
     * Maps a User entity to a UserDto
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
}
