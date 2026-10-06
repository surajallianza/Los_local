package com.bank.los.bank.auth.service;

import com.bank.los.administration.master.entity.InternalUser;
import com.bank.los.administration.master.entity.LoginDirectory;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.InternalUserRepository;
import com.bank.los.administration.master.repository.LoginDirectoryRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.bank.auth.dto.request.*;
import com.bank.los.bank.auth.dto.response.ForgotPasswordResponse;
import com.bank.los.bank.auth.dto.response.LoginResponse;
import com.bank.los.bank.auth.dto.response.UserProfileResponse;
import com.bank.los.bank.auth.mapper.AuthMapper;
import com.bank.los.bank.master.entity.Customer;
import com.bank.los.bank.master.entity.OrganizationUser;
import com.bank.los.bank.master.entity.RefreshToken;
import com.bank.los.bank.master.entity.SelfServiceResetToken;
import com.bank.los.bank.master.entity.SessionActivity;
import com.bank.los.bank.master.repository.CustomerRepository;
import com.bank.los.bank.master.repository.OrganizationUserRepository;
import com.bank.los.bank.master.repository.SelfServiceResetTokenRepository;
import com.bank.los.bank.master.repository.SessionActivityRepository;
import com.bank.los.common.constant.ApplicationConstants;
import com.bank.los.common.exception.BusinessException;
import com.bank.los.common.exception.ResourceNotFoundException;
import com.bank.los.common.exception.UnauthorizedException;
import com.bank.los.common.validation.PasswordPolicy;
import com.bank.los.config.BankContext;
import com.bank.los.config.OrganizationContext;
import com.bank.los.security.JwtTokenProvider;
import com.bank.los.security.SecurityConstants;
import com.bank.los.security.TenantResolutionService;
import com.bank.los.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import com.bank.los.administration.audit.service.AdminAuditService;
import com.bank.los.bank.audit.service.BankAuditService;
import com.bank.los.bank.master.repository.RefreshTokenRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final LoginDirectoryRepository      loginDirectoryRepository;
    private final OrganizationRepository        organizationRepository;
    private final InternalUserRepository        internalUserRepository;
    private final OrganizationUserRepository    organizationUserRepository;
    private final CustomerRepository            customerRepository;
    private final SelfServiceResetTokenRepository resetTokenRepository;
    private final SessionActivityRepository     sessionActivityRepository;
    private final RefreshTokenRepository        refreshTokenRepository;
    private final PasswordEncoder               passwordEncoder;
    private final JwtTokenProvider              jwtTokenProvider;
    private final TokenService                  tokenService;
    private final AuthMapper                    authMapper;
    private final OtpService                    otpService;
    private final EmailService                  emailService;
    private final PermissionService             permissionService;
    private final TenantResolutionService       tenantResolutionService;
    private final BankAuditService              bankAuditService;
    private final AdminAuditService             adminAuditService;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    // =====================================================================
    //  LOGIN  (Step 1 of 2 for bank staff — always requires 2FA)
    // =====================================================================

    public LoginResponse login(LoginRequest request) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        String identifier = request.getEmail().trim();
        log.info("Login attempt for identifier: {}", identifier);

        LoginDirectory directory = loginDirectoryRepository
                .findByEmailOrUserCodeOrPhone(identifier, identifier, identifier)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        Organization org    = directory.getOrganization();
        String       uType  = directory.getUserType();

        if (!ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(uType)) {
            if (org == null) throw new BusinessException("Organization profile misconfigured");
            if (ApplicationConstants.OrgStatus.SUSPENDED.equalsIgnoreCase(org.getStatus()))
                throw new BusinessException("Your organization account is suspended.");
            if (ApplicationConstants.OrgStatus.INACTIVE.equalsIgnoreCase(org.getStatus()))
                throw new BusinessException("Your organization account is inactive.");
            tenantResolutionService.cacheOrganization(org);
        }

        // ── INTERNAL / SUPER_ADMIN ────────────────────────────────────────
        if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(uType)) {
            return handleInternalLogin(identifier, request.getPassword());
        }

        // ── STAFF (ADMIN / CHECKER / MAKER / VIEWER) ─────────────────────
        if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(uType)) {
            return handleStaffLogin(identifier, request.getPassword(), org);
        }

        // ── CUSTOMER ─────────────────────────────────────────────────────
        if (ApplicationConstants.UserTypes.CUSTOMER.equalsIgnoreCase(uType)) {
            return handleCustomerLogin(identifier, request.getPassword(), org);
        }

        throw new BusinessException("Unsupported user type: " + uType);
    }

    private LoginResponse handleInternalLogin(String identifier, String rawPassword) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        InternalUser user = internalUserRepository.findByEmail(identifier)
                .or(() -> internalUserRepository.findByUsername(identifier))
                .or(() -> internalUserRepository.findByEmpNo(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        validateInternalUser(user, rawPassword, identifier);

        user.setNoOfBadLogins(0);
        user.setLastLoginDate(LocalDate.now());
        user.setLastLoginTime(LocalTime.now());
        internalUserRepository.save(user);

        UserPrincipal principal = buildInternalPrincipal(user);
        return buildFullAuthResponse(principal, authMapper.toProfileResponse(user), user.getInactiveSessionTimeout());
    }

    private LoginResponse handleStaffLogin(String identifier, String rawPassword, Organization org) {
        OrganizationContext.setCurrentOrganization(org.getDbName());
        OrganizationContext.setCurrentOrgCode(org.getCode());

        OrganizationUser user = organizationUserRepository.findByEmail(identifier)
                .or(() -> organizationUserRepository.findByUsername(identifier))
                .or(() -> organizationUserRepository.findByEmpNo(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        validateStaffUser(user, rawPassword, identifier);
        enforceLoginWindow(user);
        enforceHolidayRestriction(user);

        // ── Generate & send 2FA OTP (MANDATORY for ALL organization staff) ─────
        String rawOtp = otpService.generateOtp(user.getId());
        emailService.sendOtpEmail(user.getEmail(), rawOtp, user.getFirstName());

        // ── Issue a short-lived temp session token ────────────────────────
        UserPrincipal tempPrincipal = buildStaffPrincipal(user, org);
        String tempToken = jwtTokenProvider.generateTempSessionToken(tempPrincipal);

        LoginResponse.LoginResponseBuilder resp = LoginResponse.builder()
                .otpRequired(true)
                .tempSessionToken(tempToken);

        if (!mailEnabled) {
            resp.devOtp(rawOtp);
        }

        log.info("2FA OTP issued for staff user empNo={}, org={}", user.getEmpNo(), org.getCode());
        return resp.build();
    }

    private LoginResponse handleCustomerLogin(String identifier, String rawPassword, Organization org) {
        OrganizationContext.setCurrentOrganization(org.getDbName());
        OrganizationContext.setCurrentOrgCode(org.getCode());

        Customer customer = customerRepository.findByEmail(identifier)
                .or(() -> customerRepository.findByCustomerCode(identifier))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!Boolean.TRUE.equals(customer.getIsActive()))
            throw new UnauthorizedException("Customer account is deactivated.");

        if (customer.getPasswordHash() != null &&
            !passwordEncoder.matches(rawPassword, customer.getPasswordHash()))
            throw new UnauthorizedException("Invalid credentials");

        customer.setLastLoginAt(LocalDateTime.now());
        customerRepository.save(customer);

        UserPrincipal principal = buildCustomerPrincipal(customer, org);
        return buildFullAuthResponse(principal, authMapper.toProfileResponse(customer, org), 3600);
    }

    // =====================================================================
    //  VERIFY OTP  (Step 2 of 2 — issues full JWT for staff users)
    // =====================================================================

    public LoginResponse verifyOtp(VerifyOtpRequest request) {
        if (!jwtTokenProvider.validateToken(request.getTempSessionToken())) {
            throw new UnauthorizedException("Session expired. Please login again.");
        }

        Claims claims  = jwtTokenProvider.getClaimsFromToken(request.getTempSessionToken());
        Long   userId  = claims.get(SecurityConstants.CLAIM_USER_ID, Number.class).longValue();
        
        Number orgIdNum = claims.get(SecurityConstants.CLAIM_ORG_ID, Number.class);
        if (orgIdNum == null) {
            orgIdNum = claims.get(SecurityConstants.CLAIM_TENANT_ID, Number.class);
        }
        Long orgId = orgIdNum != null ? orgIdNum.longValue() : null;

        String orgCode = claims.get(SecurityConstants.CLAIM_ORG_CODE, String.class);
        if (orgCode == null) {
            orgCode = claims.get(SecurityConstants.CLAIM_BANK_CODE, String.class);
        }
        String userType = claims.get(SecurityConstants.CLAIM_USER_TYPE, String.class);

        if (!ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(userType)) {
            throw new BusinessException("OTP verification only applicable to bank staff.");
        }

        // SECURE RESOLUTION: Always resolve database from Master DB via TenantResolutionService
        String resolvedDb = tenantResolutionService.resolveTenantDb(orgId, orgCode, null, userType);

        OrganizationContext.setCurrentOrganization(resolvedDb);
        BankContext.setCurrentBank(resolvedDb);
        if (orgCode != null) {
            OrganizationContext.setCurrentOrgCode(orgCode);
            BankContext.setCurrentBankCode(orgCode);
        }

        OrganizationUser user = organizationUserRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found."));

        otpService.verifyOtp(userId, request.getOtp());

        user.setNoOfBadLogins(0);
        user.setLastLoginDate(LocalDate.now());
        user.setLastLoginTime(LocalTime.now());
        organizationUserRepository.save(user);

        Organization org = Organization.builder()
                .id(orgId)
                .bankCode(orgCode)
                .dbName(resolvedDb)
                .bankName(orgCode)
                .build();
        UserPrincipal principal = buildStaffPrincipal(user, org);
        LoginResponse response = buildFullAuthResponse(principal, authMapper.toProfileResponse(user, org), user.getInactiveSessionTimeout());

        registerSession(principal.getJti(), userId, user.getInactiveSessionTimeout(), resolvedDb);

        log.info("Staff login complete after 2FA: empNo={}, bank={}", user.getEmpNo(), orgCode);
        return response;
    }

    // =====================================================================
    //  FORGOT PASSWORD
    // =====================================================================

    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        String identifier = request.getEmail().trim();
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        LoginDirectory directory = loginDirectoryRepository
                .findByEmailOrUserCodeOrPhone(identifier, identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + identifier));

        Organization org    = directory.getOrganization();
        String       uType  = directory.getUserType();

        if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(uType) && org != null) {
            OrganizationContext.setCurrentOrganization(org.getDbName());
            String empNo = resolveEmpNoForStaff(identifier, org);
            long recentCount = resetTokenRepository.countRecentByEmpNo(empNo,
                    LocalDateTime.now().minusHours(1));
            if (recentCount >= ApplicationConstants.MAX_RESET_REQUESTS_PER_HOUR) {
                throw new BusinessException("RATE_LIMIT",
                        "Too many password reset requests. Please try again after 1 hour.");
            }
            issueResetToken(empNo, identifier, uType, org);
        } else if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(uType)) {
            issueResetToken(directory.getUserCode(), identifier, uType, null);
        }

        return ForgotPasswordResponse.builder()
                .expiresInSeconds((long)(ApplicationConstants.PASSWORD_RESET_EXPIRY_MINS * 60))
                .message("Password reset instructions sent. Check your email.")
                .build();
    }

    private String resolveEmpNoForStaff(String identifier, Organization org) {
        OrganizationContext.setCurrentOrganization(org.getDbName());
        return organizationUserRepository.findByEmail(identifier)
                .or(() -> organizationUserRepository.findByUsername(identifier))
                .map(OrganizationUser::getEmpNo)
                .orElse(identifier);
    }

    private void issueResetToken(String empNo, String email, String uType, Organization org) {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = sha256Hex(rawToken);

        if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(uType) && org != null) {
            OrganizationContext.setCurrentOrganization(org.getDbName());
            resetTokenRepository.invalidateAllForEmpNo(empNo);
            resetTokenRepository.save(SelfServiceResetToken.builder()
                    .empNo(empNo)
                    .tokenHash(tokenHash)
                    .used(false)
                    .expiresAt(LocalDateTime.now().plusMinutes(ApplicationConstants.PASSWORD_RESET_EXPIRY_MINS))
                    .build());
        }

        emailService.sendPasswordResetEmail(email, rawToken, empNo);
        log.info("Password reset token issued for empNo={}", empNo);
    }

    // =====================================================================
    //  RESET PASSWORD
    // =====================================================================

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordPolicy.validate(request.getNewPassword());
        String identifier = request.getEmail().trim();

        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        LoginDirectory directory = loginDirectoryRepository
                .findByEmailOrUserCodeOrPhone(identifier, identifier, identifier)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String uType = directory.getUserType();
        Organization org = directory.getOrganization();

        if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(uType) && org != null) {
            OrganizationContext.setCurrentOrganization(org.getDbName());
            String empNo = resolveEmpNoForStaff(identifier, org);
            String tokenHash = sha256Hex(request.getResetToken());

            SelfServiceResetToken token = resetTokenRepository
                    .findByTokenHashAndUsedFalseAndExpiresAtAfter(tokenHash, LocalDateTime.now())
                    .orElseThrow(() -> new BusinessException("TOKEN_INVALID",
                            "Reset token is invalid or has expired. Please request a new one."));

            OrganizationUser user = organizationUserRepository.findByEmpNo(empNo)
                    .or(() -> organizationUserRepository.findByEmail(identifier))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setNoOfBadLogins(0);
            user.setStatus(ApplicationConstants.UserStatus.OPERATIVE);
            organizationUserRepository.save(user);

            token.setUsed(true);
            resetTokenRepository.save(token);

            sessionActivityRepository.invalidateAllForUser(user.getId());

        } else if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(uType)) {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            InternalUser user = internalUserRepository.findByEmail(identifier)
                    .or(() -> internalUserRepository.findByUsername(identifier))
                    .orElseThrow(() -> new ResourceNotFoundException("Internal user not found"));

            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            user.setNoOfBadLogins(0);
            internalUserRepository.save(user);
        }

        log.info("Password reset successful for identifier={}", identifier);
    }

    // =====================================================================
    //  CHANGE PASSWORD
    // =====================================================================

    @Transactional
    public void changePassword(UserPrincipal principal, ChangePasswordRequest request) {
        PasswordPolicy.validate(request.getNewPassword());

        if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(principal.getUserType())) {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            InternalUser user = internalUserRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Internal user not found"));
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash()))
                throw new BusinessException("INVALID_PASSWORD", "Current password does not match");
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            internalUserRepository.save(user);

        } else if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(principal.getUserType())) {
            OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
            OrganizationUser user = organizationUserRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash()))
                throw new BusinessException("INVALID_PASSWORD", "Current password does not match");
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            organizationUserRepository.save(user);
            sessionActivityRepository.invalidateAllForUser(user.getId());

        } else {
            OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
            Customer customer = customerRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
            if (customer.getPasswordHash() != null &&
                !passwordEncoder.matches(request.getCurrentPassword(), customer.getPasswordHash()))
                throw new BusinessException("INVALID_PASSWORD", "Current password does not match");
            customer.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
            customerRepository.save(customer);
        }
    }

    // =====================================================================
    //  GET PROFILE
    // =====================================================================

    public UserProfileResponse getCurrentUserProfile(UserPrincipal principal) {
        if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(principal.getUserType())) {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            InternalUser user = internalUserRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Internal user not found"));
            return authMapper.toProfileResponse(user);
        }

        OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
        Organization org = Organization.builder()
                .code(principal.getOrganizationCode())
                .dbName(principal.getOrganizationDbName())
                .name(principal.getOrganizationCode())
                .build();

        if (ApplicationConstants.UserTypes.STAFF.equalsIgnoreCase(principal.getUserType())) {
            OrganizationUser user = organizationUserRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff user not found"));
            return authMapper.toProfileResponse(user, org);
        }
        Customer customer = customerRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        return authMapper.toProfileResponse(customer, org);
    }

    // =====================================================================
    //  LOGOUT
    // =====================================================================

    public void logout(UserPrincipal principal, LogoutRequest request, String ipAddress) {
        log.info("Processing logout request. Principal: {}, Request: {}",
                principal != null ? principal.getUsername() : "anonymous",
                request != null ? (request.getRefreshToken() != null ? "[provided]" : "[empty]") : "[none]");

        if (principal != null) {
            String orgDb = principal.getOrganizationDbName();
            if (orgDb == null) {
                orgDb = ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(principal.getUserType())
                        ? OrganizationContext.MASTER_ORG_ID
                        : OrganizationContext.MASTER_ORG_ID;
            }
            OrganizationContext.setCurrentOrganization(orgDb);
            BankContext.setCurrentBank(orgDb);
            if (principal.getOrganizationCode() != null) {
                OrganizationContext.setCurrentOrgCode(principal.getOrganizationCode());
                BankContext.setCurrentBankCode(principal.getOrganizationCode());
            }
        }

        // 1. If explicit refresh token was provided in request body, revoke it
        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            try {
                tokenService.revokeRefreshToken(request.getRefreshToken());
            } catch (Exception ex) {
                log.debug("Notice revoking explicit refresh token: {}", ex.getMessage());
            }
        }

        // 2. If authenticated principal is available, perform session invalidation, token cleanup, and audit logging
        if (principal != null) {

            // Invalidate session activity
            if (StringUtils.hasText(principal.getJti())) {
                try {
                    sessionActivityRepository.invalidateByJti(principal.getJti());
                } catch (Exception ex) {
                    log.debug("Notice invalidating session by jti: {}", ex.getMessage());
                }
            }
            if (principal.getId() != null) {
                try {
                    sessionActivityRepository.invalidateAllForUser(principal.getId());
                } catch (Exception ex) {
                    log.debug("Notice invalidating session for user: {}", ex.getMessage());
                }
            }

            // Revoke all refresh tokens for this user in their tenant DB / Master DB
            if (principal.getId() != null && principal.getUserType() != null) {
                try {
                    refreshTokenRepository.revokeAllForUser(principal.getId(), principal.getUserType());
                } catch (Exception ex) {
                    log.debug("Notice revoking refresh tokens on logout: {}", ex.getMessage());
                }
            }

            // Audit log
            if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(principal.getUserType())) {
                adminAuditService.logAdminAction(
                        principal.getId(),
                        principal.getUsername(),
                        "USER_LOGOUT",
                        "AUTH",
                        principal.getOrganizationCode(),
                        "Internal admin logged out successfully",
                        ipAddress
                );
            } else {
                bankAuditService.logAction(
                        principal,
                        "USER_LOGOUT",
                        "AUTH",
                        "Bank user logged out successfully",
                        ipAddress
                );
            }

            SecurityContextHolder.clearContext();
            log.info("User {} (type={}, org={}) logged out successfully",
                    principal.getUsername(), principal.getUserType(), principal.getOrganizationCode());
        }
    }

    private void validateInternalUser(InternalUser user, String rawPassword, String identifier) {
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new UnauthorizedException("Account is deactivated. Contact administrator.");
        if (!ApplicationConstants.UserStatus.OPERATIVE.equals(user.getStatus()))
            throw new UnauthorizedException("Account is not operative. Contact administrator.");
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            int attempts = user.getNoOfBadLogins() + 1;
            user.setNoOfBadLogins(attempts);
            if (attempts >= ApplicationConstants.MAX_BAD_LOGIN_ATTEMPTS) {
                user.setIsActive(false);
                internalUserRepository.save(user);
                throw new UnauthorizedException("Account locked after " + attempts + " failed attempts. Contact administrator.");
            }
            internalUserRepository.save(user);
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private void validateStaffUser(OrganizationUser user, String rawPassword, String identifier) {
        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new UnauthorizedException("Account is deactivated. Contact your bank administrator.");
        if (ApplicationConstants.UserStatus.PENDING_VERIFICATION.equals(user.getStatus()))
            throw new UnauthorizedException("Account is pending verification. Contact your bank administrator.");
        if (!ApplicationConstants.UserStatus.OPERATIVE.equals(user.getStatus()))
            throw new UnauthorizedException("Account is not operative. Contact your bank administrator.");
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            int attempts = user.getNoOfBadLogins() + 1;
            user.setNoOfBadLogins(attempts);
            if (attempts >= ApplicationConstants.MAX_BAD_LOGIN_ATTEMPTS) {
                user.setIsActive(false);
                organizationUserRepository.save(user);
                throw new UnauthorizedException("Account locked after " + attempts + " failed attempts.");
            }
            organizationUserRepository.save(user);
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private void enforceLoginWindow(OrganizationUser user) {
        if (user.getLoginTime() == null || user.getLogoutTime() == null) return;
        LocalTime now = LocalTime.now();
        if (now.isBefore(user.getLoginTime()) || now.isAfter(user.getLogoutTime())) {
            throw new BusinessException("LOGIN_WINDOW",
                    String.format("Login allowed only between %s and %s.",
                            user.getLoginTime(), user.getLogoutTime()));
        }
    }

    private void enforceHolidayRestriction(OrganizationUser user) {
        if (Boolean.TRUE.equals(user.getLoginOnHolidays())) return;
    }

    private UserPrincipal buildInternalPrincipal(InternalUser user) {
        String jti = UUID.randomUUID().toString();
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userCode(user.getEmpNo())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().getName() : ApplicationConstants.Roles.INTERNAL_ADMIN)
                .userType(ApplicationConstants.UserTypes.INTERNAL)
                .organizationId(0L)
                .organizationCode("MASTER")
                .organizationDbName(OrganizationContext.MASTER_ORG_ID)
                .active(true)
                .jti(jti)
                .build();
    }

    private UserPrincipal buildStaffPrincipal(OrganizationUser user, Organization org) {
        String jti = UUID.randomUUID().toString();
        String roleName = user.getRole() != null ? user.getRole().getName() : ApplicationConstants.Roles.VIEWER;
        String orgDb = org != null ? org.getDbName() : null;
        List<String> perms = permissionService.getEffectivePermissions(orgDb, roleName, user.getDesignation());

        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userCode(user.getEmpNo())
                .fullName(user.getFullName())
                .role(roleName)
                .designation(user.getDesignation())
                .permissions(perms)
                .userType(ApplicationConstants.UserTypes.STAFF)
                .organizationId(org != null ? org.getId() : null)
                .organizationUuid(org != null ? org.getUuid() : null)
                .organizationCode(org != null ? org.getCode() : null)
                .organizationDbName(orgDb)
                .branchId(user.getLoginBranch() != null ? user.getLoginBranch().getId() : null)
                .active(true)
                .jti(jti)
                .build();
    }

    private UserPrincipal buildCustomerPrincipal(Customer customer, Organization org) {
        return UserPrincipal.builder()
                .id(customer.getId())
                .email(customer.getEmail())
                .userCode(customer.getCustomerCode())
                .fullName(customer.getFirstName() + " " + customer.getLastName())
                .role(ApplicationConstants.Roles.CUSTOMER)
                .userType(ApplicationConstants.UserTypes.CUSTOMER)
                .organizationId(org != null ? org.getId() : null)
                .organizationUuid(org != null ? org.getUuid() : null)
                .organizationCode(org != null ? org.getCode() : null)
                .organizationDbName(org != null ? org.getDbName() : null)
                .branchId(customer.getBranch() != null ? customer.getBranch().getId() : null)
                .active(true)
                .jti(UUID.randomUUID().toString())
                .build();
    }

    private LoginResponse buildFullAuthResponse(UserPrincipal principal, UserProfileResponse profile, int timeoutSecs) {
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        RefreshToken refreshTokenEntity = tokenService.createRefreshToken(principal);
        List<String> permissions = principal.getPermissions() != null ? principal.getPermissions() :
                permissionService.getEffectivePermissions(principal.getOrganizationDbName(), principal.getRole(), principal.getDesignation());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenEntity.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationMs())
                .dashboardUrl(resolveDashboardUrl(principal.getRole()))
                .permissions(permissions)
                .user(profile)
                .build();
    }


    private void registerSession(String jti, Long userId, int timeoutSecs, String orgDb) {
        if (jti == null) return;
        OrganizationContext.setCurrentOrganization(orgDb);
        sessionActivityRepository.save(SessionActivity.builder()
                .jti(jti)
                .userId(userId)
                .lastSeen(LocalDateTime.now())
                .timeoutSecs(timeoutSecs)
                .invalidated(false)
                .build());
    }

    private String resolveDashboardUrl(String role) {
        if (role == null) return "/dashboard";
        return switch (role.toUpperCase()) {
            case "INTERNAL_ADMIN" -> "/dashboard/internal-admin";
            case "ADMIN"          -> "/dashboard/admin";
            case "MAKER"          -> "/dashboard/maker";
            case "CHECKER"        -> "/dashboard/checker";
            case "VIEWER"         -> "/dashboard/viewer";
            case "CUSTOMER"       -> "/dashboard/customer";
            default               -> "/dashboard";
        };
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
