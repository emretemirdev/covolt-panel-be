package com.covolt.backend.modules.platform_administration.company_management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for company statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyStatisticsDto {
    // Company counts by status
    private long totalCompanies;
    private long activeCompanies;
    private long suspendedCompanies;
    private long pendingVerificationCompanies;
    private long deletedCompanies;
    
    // User statistics
    private long totalUsers;
    private double averageUsersPerCompany;
    
    // Subscription statistics
    private long companiesWithActiveSubscription;
    private long companiesWithTrialSubscription;
    private long companiesWithExpiredSubscription;
}
