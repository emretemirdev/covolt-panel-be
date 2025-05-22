package com.covolt.backend.core.security.common; // Veya core.config.security

import lombok.Getter;
import org.springframework.http.HttpMethod;

@Getter
public enum ApiEndpoint {

    // === Authentication Modülü ===
    // Herkesin Erişebileceği Auth Endpoint'leri
    AUTH_REGISTER("/api/auth/register", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),
    AUTH_LOGIN("/api/auth/login", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),
    AUTH_REFRESH_TOKEN("/api/auth/refresh", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),
    // E-posta doğrulama ve şifre sıfırlama endpoint'leri de genellikle permitAll olur
    AUTH_VERIFY_EMAIL("/api/auth/verify/email", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),
    AUTH_RESEND_VERIFICATION_EMAIL("/api/auth/verify/resend", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),
    AUTH_FORGOT_PASSWORD("/api/auth/password/forgot", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),
    AUTH_RESET_PASSWORD("/api/auth/password/reset", HttpMethod.POST, SecurityAccess.PERMIT_ALL, null),

    // Kimlik Doğrulaması Gerektiren Auth Endpoint'leri
    AUTH_LOGOUT("/api/auth/logout", HttpMethod.POST, SecurityAccess.AUTHENTICATED, null),
    AUTH_CHANGE_PASSWORD("/api/auth/password/change", HttpMethod.POST, SecurityAccess.AUTHENTICATED, null), // Mevcut şifreyle değiştirme
    AUTH_USER_AUTHORITIES("/api/auth/user-authorities", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null), // Geçici olarak herkese açık

    // === Kullanıcı Profili (Kendisi İçin) ===
    USER_GET_PROFILE("/api/v1/users/me", HttpMethod.GET, SecurityAccess.AUTHENTICATED, null), // İzin "PROFILE_READ_OWN" olabilir
    USER_UPDATE_PROFILE("/api/v1/users/me", HttpMethod.PUT, SecurityAccess.AUTHENTICATED, null), // İzin "PROFILE_UPDATE_OWN" olabilir

    PERMISSION_CREATE("/api/v1/platform-admin/permissions", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_PERMISSIONS"),
    PERMISSION_GET_ALL("/api/v1/platform-admin/permissions", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_PERMISSIONS"),
    PERMISSION_GET_BY_ID("/api/v1/platform-admin/permissions/{permissionId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_PERMISSIONS"),
    PERMISSION_UPDATE("/api/v1/platform-admin/permissions/{permissionId}", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_PERMISSIONS"),
    PERMISSION_DELETE("/api/v1/platform-admin/permissions/{permissionId}", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_PERMISSIONS"),

    // === Platform Admin - Rol (Role) Yönetimi ===
    // 'MANAGE_ROLES' iznine sahip olanlar erişebilir.
    ROLE_CREATE("/api/v1/platform-admin/roles", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_GET_ALL("/api/v1/platform-admin/roles", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_GET_BY_ID("/api/v1/platform-admin/roles/{roleId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_UPDATE("/api/v1/platform-admin/roles/{roleId}", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_DELETE("/api/v1/platform-admin/roles/{roleId}", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_ASSIGN_PERMISSIONS("/api/v1/platform-admin/roles/{roleId}/permissions", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),
    ROLE_REMOVE_PERMISSIONS("/api/v1/platform-admin/roles/{roleId}/permissions", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_ROLES"),

    // === Platform Admin - Abonelik Planı (Subscription Plan) Yönetimi ===
    // 'MANAGE_SUBSCRIPTION_PLANS' iznine sahip olanlar erişebilir.
    PLAN_CREATE("/api/v1/platform-admin/subscription-plans", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_SUBSCRIPTION_PLANS"),
    PLAN_GET_ALL("/api/v1/platform-admin/subscription-plans", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_SUBSCRIPTION_PLANS"),
    PLAN_GET_BY_ID("/api/v1/platform-admin/subscription-plans/{planId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_SUBSCRIPTION_PLANS"),
    PLAN_UPDATE("/api/v1/platform-admin/subscription-plans/{planId}", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_SUBSCRIPTION_PLANS"),
    PLAN_UPDATE_STATUS("/api/v1/platform-admin/subscription-plans/{planId}/status", HttpMethod.PATCH, SecurityAccess.HAS_AUTHORITY, "MANAGE_SUBSCRIPTION_PLANS"),

    // === Platform Admin - Müşteri Firma (Company) Yönetimi ===
    // 'MANAGE_COMPANIES' iznine sahip olanlar erişebilir.
    COMPANY_GET_ALL_FOR_ADMIN("/api/v1/platform-admin/companies", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_GET_BY_ID_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_CREATE_MANUAL_FOR_ADMIN("/api/v1/platform-admin/companies", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_UPDATE_PROFILE_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_UPDATE_STATUS_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/status", HttpMethod.PATCH, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_CREATE_SUBSCRIPTION_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/subscriptions", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_STATISTICS_FOR_ADMIN("/api/v1/platform-admin/companies/statistics", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),

    // === Platform Admin - Müşteri Firma Kullanıcıları Yönetimi ===
    // 'MANAGE_COMPANY_USERS' (veya daha spesifik) iznine sahip olanlar erişebilir.
    COMPANY_USERS_GET_ALL_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),
    COMPANY_USER_UPDATE_STATUS_FOR_ADMIN("/api/v1/platform-admin/users/{userId}/status", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),

    // Kullanıcı ekleme/çıkarma işlemleri
    COMPANY_USER_ADD_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),
    COMPANY_USER_REMOVE_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users/{userId}", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),
    COMPANY_USER_TRANSFER_FOR_ADMIN("/api/v1/platform-admin/users/{userId}/transfer", HttpMethod.PATCH, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),
    COMPANY_USER_ROLES_UPDATE_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users/{userId}/roles", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),

    // Şifre sıfırlama işlemi
    COMPANY_USER_PASSWORD_RESET_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users/{userId}/password-reset", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),

    // === Email Feature Endpoints ===
    // Email sending operations
    EMAIL_SEND("/api/v1/email/send", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_EMAIL"),
    EMAIL_SEND_ASYNC("/api/v1/email/send-async", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_EMAIL"),
    EMAIL_SCHEDULE("/api/v1/email/schedule", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SCHEDULE_EMAIL"),

    // Bulk email operations
    EMAIL_BULK_SEND("/api/v1/email/bulk/send", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_BULK_EMAIL"),
    EMAIL_BULK_SEND_ASYNC("/api/v1/email/bulk/send-async", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_BULK_EMAIL"),

    // Quick send operations
    EMAIL_QUICK_WELCOME("/api/v1/email/quick/welcome", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_EMAIL"),
    EMAIL_QUICK_VERIFICATION("/api/v1/email/quick/verification", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_EMAIL"),
    EMAIL_QUICK_PASSWORD_RESET("/api/v1/email/quick/password-reset", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "SEND_EMAIL"),

    // Email tracking and management
    EMAIL_GET_BY_ID("/api/v1/email/{emailId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),
    EMAIL_GET_BY_USER("/api/v1/email/user/{userId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),
    EMAIL_GET_BY_COMPANY("/api/v1/email/company/{companyId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),
    EMAIL_GET_BY_TYPE("/api/v1/email/type/{emailType}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),
    EMAIL_GET_BY_DATE_RANGE("/api/v1/email/date-range", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),

    // Bulk email tracking
    EMAIL_BULK_GET_BY_ID("/api/v1/email/bulk/{bulkEmailId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),
    EMAIL_BULK_GET_BY_CAMPAIGN("/api/v1/email/bulk/campaign/{campaignId}", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_EMAIL"),

    // Email management operations
    EMAIL_RETRY("/api/v1/email/{emailId}/retry", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_EMAIL"),
    EMAIL_CANCEL("/api/v1/email/{emailId}/cancel", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_EMAIL"),
    EMAIL_BULK_CANCEL("/api/v1/email/bulk/{bulkEmailId}/cancel", HttpMethod.DELETE, SecurityAccess.HAS_AUTHORITY, "MANAGE_EMAIL"),

    // System status
    EMAIL_STATUS("/api/v1/email/status", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "VIEW_SYSTEM_STATUS"),
    EMAIL_HEALTH("/api/v1/email/health", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),

    // === Customer - Company Profile Management ===
    // Authenticated users can access their own company information
    CUSTOMER_COMPANY_PROFILE_GET("/api/v1/customer/company/profile", HttpMethod.GET, SecurityAccess.AUTHENTICATED, null),
    CUSTOMER_COMPANY_PROFILE_UPDATE("/api/v1/customer/company/profile", HttpMethod.PUT, SecurityAccess.AUTHENTICATED, null),
    CUSTOMER_COMPANY_USERS_GET("/api/v1/customer/company/users", HttpMethod.GET, SecurityAccess.AUTHENTICATED, null),
    CUSTOMER_COMPANY_SUBSCRIPTION_GET("/api/v1/customer/company/subscription", HttpMethod.GET, SecurityAccess.AUTHENTICATED, null),

    // --- Diğer Modüller ve Endpoint'ler Buraya Eklenecek ---
    // DATA_UPLOAD_ENDPOINT("/api/v1/data/upload", HttpMethod.POST, SecurityAccess.AUTHENTICATED, "FEATURE_DATA_UPLOAD") // Özellik bazlı olabilir

    // Swagger / OpenAPI endpoint'leri (permitAll)
    SWAGGER_UI("/swagger-ui/**", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),
    SWAGGER_RESOURCES("/swagger-resources/**", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),
    API_DOCS("/v3/api-docs/**", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),
    SWAGGER_UI_HTML("/swagger-ui.html", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null),
    SWAGGER_CONFIG("/v3/api-docs/swagger-config", HttpMethod.GET, SecurityAccess.PERMIT_ALL, null);

    private final String pathPattern; // Ant path pattern
    private final HttpMethod httpMethod;
    private final SecurityAccess accessType;
    private final String authority; // Rol veya İzin adı (eğer accessType HAS_ROLE veya HAS_AUTHORITY ise)

    ApiEndpoint(String pathPattern, HttpMethod httpMethod, SecurityAccess accessType, String authority) {
        this.pathPattern = pathPattern;
        this.httpMethod = httpMethod;
        this.accessType = accessType;
        this.authority = authority;
    }

    // getPathPattern(), getHttpMethod(), getAccessType(), getAuthority() Lombok @Getter ile otomatik gelir.


    public String getSecurityExpression() {
        switch (accessType) {
            case PERMIT_ALL:
                return "permitAll()";
            case AUTHENTICATED:
                return "authenticated()";
            case HAS_ROLE:
                if (authority == null || authority.trim().isEmpty()) {
                    throw new IllegalStateException("HAS_ROLE için 'authority' alanı (rol adı) boş olamaz: " + this.name());
                }
                return "hasRole('" + authority + "')";
            case HAS_AUTHORITY:
                if (authority == null || authority.trim().isEmpty()) {
                    throw new IllegalStateException("HAS_AUTHORITY için 'authority' alanı (izin adı) boş olamaz: " + this.name());
                }
                return "hasAuthority('" + authority + "')";
            default:
                // Belki de denyAll() veya başka bir varsayılan
                throw new IllegalStateException("Bilinmeyen SecurityAccess tipi: " + accessType);
        }
    }

    /**
     * URL path'indeki {variable} kısımlarını verilen parametrelerle doldurur.
     * @param params Path variable'ları için değerler, sırasıyla.
     * @return Doldurulmuş path.
     */
    public String getPath(Object... params) {
        if (params == null || params.length == 0) {
            return pathPattern;
        }
        try {
            // String.format, {roleId} gibi placeholder'ları anlamaz.
            // Basit bir placeholder değiştirme yapalım:
            String tempPath = pathPattern;
            for (Object param : params) {
                tempPath = tempPath.replaceFirst("\\{[^}]+\\}", param.toString());
            }
            return tempPath;
        } catch (Exception e) {
            // Formatlama hatası olursa orijinal pattern'i dön
            return pathPattern;
        }
    }
}