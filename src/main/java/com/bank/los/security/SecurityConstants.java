package com.bank.los.security;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String TOKEN_HEADER          = "Authorization";
    public static final String TOKEN_PREFIX          = "Bearer ";
    public static final String BANK_HEADER           = "X-Bank-Code";
    public static final String ORGANIZATION_HEADER   = "X-Organization-Code";
    public static final String TENANT_HEADER         = "X-Tenant-Code"; // alias for backward compatibility

    public static final String CLAIM_USER_ID         = "userId";
    public static final String CLAIM_USER_TYPE       = "userType";
    public static final String CLAIM_ROLE            = "role";
    public static final String CLAIM_ORG_ID          = "orgId";
    public static final String CLAIM_TENANT_ID       = "tenantId";
    public static final String CLAIM_ORG_UUID        = "orgUuid";
    public static final String CLAIM_BANK_CODE       = "bankCode";
    public static final String CLAIM_ORG_CODE        = "orgCode";
    public static final String CLAIM_BANK_DB         = "bankDb";
    public static final String CLAIM_ORG_DB          = "orgDb";
    public static final String CLAIM_TENANT_DB       = "tenantDb";      // alias for backward compatibility
    public static final String CLAIM_BRANCH_ID       = "branchId";
    public static final String CLAIM_FULL_NAME       = "fullName";
    public static final String CLAIM_DESIGNATION     = "designation";
    public static final String CLAIM_PERMISSIONS     = "permissions";
    public static final String CLAIM_JTI             = "jti";          // JWT ID for session tracking
    public static final String CLAIM_TIMEOUT         = "timeoutSecs";  // inactive session timeout in JWT


    public static final String[] PUBLIC_URLS = {
            "/health",
            "/api/v1/health",
            "/api/v1/auth/health",
            "/api/v1/auth/login",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/logout",
            "/api/v1/admin/auth/**",
            "/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/api/leads/**",
            "/api/v1/leads/**",
            "/index.html",
            "/sample_leads.csv"
    };
}
