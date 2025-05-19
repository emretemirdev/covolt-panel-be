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
    COMPANY_CREATE_MANUAL_FOR_ADMIN("/api/v1/platform-admin/companies", HttpMethod.POST, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"), // İzin MANAGE_COMPANIES veya COMPANY_CREATE olabilir
    COMPANY_UPDATE_PROFILE_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),
    COMPANY_UPDATE_STATUS_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/status", HttpMethod.PATCH, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANIES"),

    // === Platform Admin - Müşteri Firma Kullanıcıları Yönetimi ===
    // 'MANAGE_COMPANY_USERS' (veya daha spesifik) iznine sahip olanlar erişebilir.
    COMPANY_USERS_GET_ALL_FOR_ADMIN("/api/v1/platform-admin/companies/{companyId}/users", HttpMethod.GET, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"), // Yeni izin gerekebilir
    COMPANY_USER_UPDATE_STATUS_FOR_ADMIN("/api/v1/platform-admin/users/{userId}/status", HttpMethod.PUT, SecurityAccess.HAS_AUTHORITY, "MANAGE_COMPANY_USERS"),

    // --- Diğer Modüller ve Endpoint'ler Buraya Eklenecek ---
    // Örn: Müşteri Paneli için endpoint'ler
    // COMPANY_OWN_DETAILS_GET("/api/v1/company/me", HttpMethod.GET, SecurityAccess.HAS_ROLE, "ROLE_COMPANY_ADMIN"),
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