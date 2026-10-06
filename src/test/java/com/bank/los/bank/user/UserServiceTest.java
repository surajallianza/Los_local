package com.bank.los.bank.user;

import com.bank.los.administration.organization.dto.CreateOrganizationRequest;
import com.bank.los.administration.organization.dto.OrganizationResponse;
import com.bank.los.administration.organization.service.OrganizationService;
import com.bank.los.bank.auth.dto.request.LoginRequest;
import com.bank.los.bank.auth.dto.request.VerifyOtpRequest;
import com.bank.los.bank.auth.dto.response.LoginResponse;
import com.bank.los.bank.auth.service.AuthenticationService;
import com.bank.los.bank.user.dto.CreateUserRequest;
import com.bank.los.bank.user.dto.UserResponse;
import com.bank.los.bank.user.service.UserService;
import com.bank.los.config.OrganizationContext;
import com.bank.los.security.UserPrincipal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("local")
class UserServiceTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    @DisplayName("Should register bank with organizational info and allow Super Admin to create Bank Admin who logs in")
    void testBankOnboardingAndAdminAssignmentFlow() {
        // 1. Super Admin registers a new Bank (only organization info)
        String dbName = "los_icici99_db";

        CreateOrganizationRequest orgRequest = CreateOrganizationRequest.builder()
                .bankName("ICICI Bank Ltd")
                .legalName("ICICI Bank Limited")
                .bankType("BANK")
                .registrationNumber("REG-MH-2024-9900")
                .pan("AAACI1234F")
                .cin("L65191PN1994PLC076333")
                .regulatoryAuthorityId(java.util.UUID.randomUUID())
                .regulatoryStatus("ACTIVE")
                .country("India")
                .contactEmail("contact@icicibank.com")
                .contactPhone("+912240001000")
                .dbName(dbName)
                .dbHost("localhost")
                .dbPort(5432)
                .build();

        OrganizationResponse orgResponse = organizationService.createOrganization(orgRequest);
        assertNotNull(orgResponse);
        assertNotNull(orgResponse.getId());
        assertEquals("contact@icicibank.com", orgResponse.getContactEmail());

        // 2. Super Admin creates a Bank Admin user for this new Bank
        UserPrincipal superAdminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@losplatform.com")
                .userCode("EMP-MST-001")
                .role("INTERNAL_ADMIN")
                .userType("INTERNAL")
                .organizationDbName(OrganizationContext.MASTER_DB_NAME)
                .tenantDbName(OrganizationContext.MASTER_DB_NAME)
                .organizationCode("MASTER")
                .active(true)
                .build();

        CreateUserRequest adminUserRequest = CreateUserRequest.builder()
                .organizationId(orgResponse.getPkid())
                .username("icici_admin")
                .email("admin@icicibank.com")
                .password("Admin@123")
                .roleName("ADMIN")
                .firstName("Aditya")
                .lastName("Kapoor")
                .mobile("+919811122233")
                .status("OPERATIVE")
                .build();

        UserResponse userResponse = userService.createUser(superAdminPrincipal, adminUserRequest);
        assertNotNull(userResponse);
        assertNotNull(userResponse.getOrganizationCode());
        assertEquals("icici_admin", userResponse.getUsername());
        assertEquals("admin@icicibank.com", userResponse.getEmail());
        assertEquals("ADMIN", userResponse.getRoleName());
        assertEquals("OPERATIVE", userResponse.getStatus());

        // 3. Newly created Bank Admin logs in and gets routed to ICICI bank dashboard
        LoginRequest loginRequest = LoginRequest.builder()
                .email("admin@icicibank.com")
                .password("Admin@123")
                .build();

        LoginResponse loginResp1 = authenticationService.login(loginRequest);
        assertNotNull(loginResp1);
        assertTrue(Boolean.TRUE.equals(loginResp1.getOtpRequired()));

        // Complete 2FA
        LoginResponse loginResp2 = authenticationService.verifyOtp(VerifyOtpRequest.builder()
                .tempSessionToken(loginResp1.getTempSessionToken())
                .otp(loginResp1.getDevOtp())
                .build());

        assertNotNull(loginResp2);
        assertEquals("ADMIN", loginResp2.getUser().getRole());
        assertEquals(userResponse.getOrganizationCode(), loginResp2.getUser().getOrganizationCode());
        assertEquals("/dashboard/admin", loginResp2.getDashboardUrl());

        // 4. Super Admin lists users of this bank
        List<UserResponse> iciciUsers = userService.getAllUsers(superAdminPrincipal, orgResponse.getPkid());
        assertFalse(iciciUsers.isEmpty());
        assertTrue(iciciUsers.stream().anyMatch(u -> "admin@icicibank.com".equals(u.getEmail())));
    }
}
