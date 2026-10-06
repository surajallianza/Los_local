package com.bank.los.bank.user.service;

import com.bank.los.administration.master.entity.LoginDirectory;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.LoginDirectoryRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.bank.master.entity.Branch;
import com.bank.los.bank.master.entity.OrganizationRole;
import com.bank.los.bank.master.entity.OrganizationUser;
import com.bank.los.bank.master.repository.BranchRepository;
import com.bank.los.bank.master.repository.OrganizationRoleRepository;
import com.bank.los.bank.master.repository.OrganizationUserRepository;
import com.bank.los.bank.user.dto.AdminResetUserPasswordRequest;
import com.bank.los.bank.user.dto.CreateUserRequest;
import com.bank.los.bank.user.dto.UserResponse;
import com.bank.los.common.constant.ApplicationConstants;
import com.bank.los.common.exception.BusinessException;
import com.bank.los.common.exception.ResourceNotFoundException;
import com.bank.los.common.validation.PasswordPolicy;
import com.bank.los.config.OrganizationContext;
import com.bank.los.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final OrganizationUserRepository organizationUserRepository;
    private final BranchRepository branchRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final LoginDirectoryRepository loginDirectoryRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllUsers(UserPrincipal principal, Long organizationId) {
        boolean isInternalAdmin = isInternalAdmin(principal);

        if (isInternalAdmin) {
            if (organizationId != null) {
                OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
                Organization org = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
                OrganizationContext.setCurrentOrganization(org.getDbName());
                OrganizationContext.setCurrentOrgCode(org.getCode());
                return organizationUserRepository.findAll().stream()
                        .map(u -> mapToResponse(u, org))
                        .collect(Collectors.toList());
            } else {
                // Return all users across all active organizations
                OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
                List<Organization> orgs = organizationRepository.findAll();
                List<UserResponse> allUsers = new ArrayList<>();
                for (Organization org : orgs) {
                    try {
                        OrganizationContext.setCurrentOrganization(org.getDbName());
                        OrganizationContext.setCurrentOrgCode(org.getCode());
                        List<UserResponse> orgUsers = organizationUserRepository.findAll().stream()
                                .map(u -> mapToResponse(u, org))
                                .collect(Collectors.toList());
                        allUsers.addAll(orgUsers);
                    } catch (Exception ex) {
                        log.debug("Notice listing users for org {}: {}", org.getCode(), ex.getMessage());
                    }
                }
                return allUsers;
            }
        } else {
            // Organization admin / staff querying their own organization
            OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
            OrganizationContext.setCurrentOrgCode(principal.getOrganizationCode());
            return organizationUserRepository.findAll().stream()
                    .map(u -> mapToResponse(u, null))
                    .collect(Collectors.toList());
        }
    }

    public UserResponse getUserById(UserPrincipal principal, Long id, Long organizationId) {
        boolean isInternalAdmin = isInternalAdmin(principal);
        Organization targetOrg = null;

        if (isInternalAdmin) {
            if (organizationId != null) {
                OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
                targetOrg = organizationRepository.findById(organizationId)
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
                OrganizationContext.setCurrentOrganization(targetOrg.getDbName());
                OrganizationContext.setCurrentOrgCode(targetOrg.getCode());
            } else {
                OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            }
        } else {
            OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
            OrganizationContext.setCurrentOrgCode(principal.getOrganizationCode());
        }

        final Organization finalOrg = targetOrg;
        OrganizationUser user = organizationUserRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return mapToResponse(user, finalOrg);
    }

    public UserResponse createUser(UserPrincipal principal, CreateUserRequest request) {
        PasswordPolicy.validate(request.getPassword());
        boolean isInternalAdmin = isInternalAdmin(principal);

        Organization org;
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        if (isInternalAdmin) {
            if (request.getOrganizationId() != null) {
                org = organizationRepository.findById(request.getOrganizationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", request.getOrganizationId()));
            } else if (request.getOrganizationUuid() != null) {
                org = organizationRepository.findByUuid(request.getOrganizationUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "uuid", request.getOrganizationUuid()));
            } else if (request.getOrganizationCode() != null && !request.getOrganizationCode().isBlank()) {
                org = organizationRepository.findByCode(request.getOrganizationCode().trim().toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Organization", "code", request.getOrganizationCode()));
            } else {
                throw new BusinessException("ORGANIZATION_REQUIRED", "Please specify organizationId, organizationUuid, or organizationCode when creating a bank user as Super Admin");
            }
        } else {
            org = organizationRepository.findByCode(principal.getOrganizationCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Organization", "code", principal.getOrganizationCode()));
        }

        String orgDb = org.getDbName();
        String orgCode = org.getCode();

        // 1. Check globally in Master Login Directory for unique email
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        if (loginDirectoryRepository.findByEmail(request.getEmail().trim().toLowerCase()).isPresent()) {
            throw new BusinessException("EMAIL_EXISTS", "User with email '" + request.getEmail() + "' is already registered in the platform");
        }

        // 2. Switch to target Organization DB
        OrganizationContext.setCurrentOrganization(orgDb);
        OrganizationContext.setCurrentOrgCode(orgCode);

        // Parse Name if full name passed
        String firstName = request.getFirstName();
        String middleName = request.getMiddleName();
        String lastName = request.getLastName();

        if ((firstName == null || firstName.isBlank()) && request.getName() != null && !request.getName().isBlank()) {
            String[] parts = request.getName().trim().split("\\s+");
            if (parts.length == 1) {
                firstName = parts[0];
                lastName = parts[0];
            } else if (parts.length == 2) {
                firstName = parts[0];
                lastName = parts[1];
            } else {
                firstName = parts[0];
                middleName = parts[1];
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < parts.length; i++) {
                    if (i > 2) sb.append(" ");
                    sb.append(parts[i]);
                }
                lastName = sb.toString();
            }
        }

        if (firstName == null || firstName.isBlank()) {
            firstName = request.getEmail().split("@")[0];
        }
        if (lastName == null || lastName.isBlank()) {
            lastName = "Staff";
        }

        String username = request.getUsername();
        if (username == null || username.isBlank()) {
            username = request.getEmail().split("@")[0].toLowerCase();
        } else {
            username = username.trim().toLowerCase();
        }

        if (organizationUserRepository.existsByUsername(username)) {
            throw new BusinessException("USERNAME_EXISTS", "User with username '" + username + "' already exists in " + org.getName());
        }

        if (organizationUserRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BusinessException("EMAIL_EXISTS", "User with email '" + request.getEmail() + "' already exists in " + org.getName());
        }

        // 3. Resolve Role in organization DB
        OrganizationRole role = null;
        if (request.getRoleId() != null) {
            role = organizationRoleRepository.findById(request.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));
        } else if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String roleName = request.getRoleName().trim().toUpperCase();
            role = organizationRoleRepository.findByName(roleName)
                    .orElseGet(() -> autoSeedRole(roleName));
        } else {
            String defaultRole = isInternalAdmin ? "ADMIN" : "VIEWER";
            role = organizationRoleRepository.findByName(defaultRole)
                    .orElseGet(() -> autoSeedRole(defaultRole));
        }

        // 4. Resolve Branch in organization DB
        Branch loginBranch = null;
        Long targetBranchId = request.getLoginBranchId();
        String branchIdentifier = request.getLoginBranch();

        if (targetBranchId == null && branchIdentifier != null && !branchIdentifier.isBlank()) {
            try {
                targetBranchId = Long.parseLong(branchIdentifier.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (targetBranchId != null) {
            loginBranch = branchRepository.findById(targetBranchId).orElse(null);
        }
        if (loginBranch == null && branchIdentifier != null && !branchIdentifier.isBlank()) {
            loginBranch = branchRepository.findByName(branchIdentifier.trim())
                    .or(() -> branchRepository.findByCode(branchIdentifier.trim()))
                    .orElse(null);
        }

        if (loginBranch == null) {
            loginBranch = branchRepository.findAll().stream().findFirst()
                    .orElseGet(() -> branchRepository.save(Branch.builder()
                            .name(org.getName() + " Main Branch")
                            .code(org.getCode() + "-BR-01")
                            .address("Headquarters")
                            .city("Mumbai")
                            .state("Maharashtra")
                            .pincode("400001")
                            .status("ACTIVE")
                            .build()));
        }

        String empNo = request.getEmpNo();
        if (empNo == null || empNo.isBlank()) {
            empNo = "EMP-" + orgCode + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else {
            empNo = empNo.trim();
        }

        String initialStatus = request.getStatus();
        if (initialStatus == null || initialStatus.isBlank()) {
            initialStatus = isInternalAdmin ? ApplicationConstants.UserStatus.OPERATIVE : ApplicationConstants.UserStatus.PENDING_VERIFICATION;
        }

        Boolean twoFa = request.getTwoFaEnabled();
        if (twoFa == null) {
            twoFa = true;
        }

        Long creatorId = principal != null ? principal.getId() : (request.getCreatedBy() != null ? request.getCreatedBy() : 1L);

        OrganizationUser user = OrganizationUser.builder()
                .empNo(empNo)
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .twoFaEnabled(twoFa)
                .status(initialStatus)
                .isActive(true)
                .role(role)
                .loginBranch(loginBranch)
                .multiBranchAccess(Boolean.TRUE.equals(request.getMultiBranchAccess()))
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .dob(request.getDob())
                .email(request.getEmail().trim().toLowerCase())
                .mobile(request.getMobile())
                .gender(request.getGender())
                .designation(request.getDesignation() != null ? request.getDesignation() : (role.getName() + " - " + org.getName()))
                .loginOnHolidays(Boolean.TRUE.equals(request.getLoginOnHolidays()))
                .loginTime(request.getLoginTime())
                .logoutTime(request.getLogoutTime())
                .inactiveSessionTimeout(request.getInactiveSessionTimeout() != null ? request.getInactiveSessionTimeout() : 1800)
                .lastLoginDate(request.getLastLoginDate())
                .noOfBadLogins(request.getNoOfBadLogins() != null ? request.getNoOfBadLogins() : 0)
                .verifiedBy(request.getVerifiedBy())
                .verifiedDate(request.getVerifiedDate())
                .modifiedBy(request.getModifiedBy())
                .build();
        user.setCreatedBy(creatorId);

        OrganizationUser savedUser = organizationUserRepository.save(user);

        // 5. Register in Master Login Directory for dynamic routing
        try {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            LoginDirectory loginDirectory = LoginDirectory.builder()
                    .userCode(savedUser.getEmpNo())
                    .email(savedUser.getEmail())
                    .phone(savedUser.getMobile())
                    .organization(org)
                    .userType(ApplicationConstants.UserTypes.STAFF)
                    .build();

            loginDirectoryRepository.save(loginDirectory);
        } finally {
            OrganizationContext.setCurrentOrganization(orgDb);
            OrganizationContext.setCurrentOrgCode(orgCode);
        }

        log.info("Successfully created staff user empNo={} username={} role={} for organization={}",
                savedUser.getEmpNo(), savedUser.getUsername(), role.getName(), orgCode);
        return mapToResponse(savedUser, org);
    }

    @Transactional
    public UserResponse verifyUser(UserPrincipal principal, Long userId) {
        String orgDb = principal.getOrganizationDbName();
        OrganizationContext.setCurrentOrganization(orgDb);

        OrganizationUser user = organizationUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!isInternalAdmin(principal) && user.getCreatedBy() != null && user.getCreatedBy().equals(principal.getId())) {
            throw new BusinessException("MAKER_CHECKER_VIOLATION", "The creator of the user cannot verify/approve the same user record (Maker-Checker policy).");
        }

        user.setStatus(ApplicationConstants.UserStatus.OPERATIVE);
        user.setVerifiedBy(principal.getId());
        user.setVerifiedDate(LocalDateTime.now());
        user.setModifiedBy(principal.getId());
        user.setUpdatedAt(LocalDateTime.now());

        OrganizationUser updated = organizationUserRepository.save(user);
        log.info("User id={} verified and marked OPERATIVE by verifier={}", userId, principal.getId());
        return mapToResponse(updated, null);
    }

    @Transactional
    public void adminResetPassword(UserPrincipal principal, Long userId, AdminResetUserPasswordRequest request) {
        PasswordPolicy.validate(request.getNewPassword());
        String orgDb = principal.getOrganizationDbName();
        OrganizationContext.setCurrentOrganization(orgDb);

        OrganizationUser user = organizationUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setNoOfBadLogins(0);
        user.setModifiedBy(principal.getId());
        user.setUpdatedAt(LocalDateTime.now());

        organizationUserRepository.save(user);
        log.info("Password reset by admin id={} for user id={}", principal.getId(), userId);
    }

    public UserResponse mapToResponse(OrganizationUser user, Organization org) {
        String fullName = user.getFullName();
        String roleName = user.getRole() != null ? user.getRole().getName() : null;
        Integer roleId = user.getRole() != null ? user.getRole().getId() : null;
        String branchName = user.getLoginBranch() != null ? user.getLoginBranch().getName() : null;
        Long branchId = user.getLoginBranch() != null ? user.getLoginBranch().getId() : null;

        return UserResponse.builder()
                .pkid(user.getId())
                .id(user.getId())
                .organizationId(org != null ? org.getId() : null)
                .organizationCode(org != null ? org.getCode() : null)
                .organizationName(org != null ? org.getName() : null)
                .empNo(user.getEmpNo())
                .username(user.getUsername())
                .name(fullName)
                .firstName(user.getFirstName())
                .middleName(user.getMiddleName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .mobile(user.getMobile())
                .gender(user.getGender())
                .dob(user.getDob())
                .designation(user.getDesignation())
                .status(user.getStatus())
                .isActive(user.getIsActive())
                .twoFa(user.getTwoFaEnabled())
                .twoFaEnabled(user.getTwoFaEnabled())
                .role(roleName)
                .roleId(roleId)
                .roleName(roleName)
                .mBrAccess(user.getMultiBranchAccess())
                .multiBranchAccess(user.getMultiBranchAccess())
                .loginBranch(branchName)
                .loginBranchId(branchId)
                .loginBranchName(branchName)
                .holidayLogin(user.getLoginOnHolidays())
                .loginOnHolidays(user.getLoginOnHolidays())
                .loginTime(user.getLoginTime())
                .logoutTime(user.getLogoutTime())
                .inactiveSessionTimeout(user.getInactiveSessionTimeout())
                .noOfBadLogins(user.getNoOfBadLogins())
                .lastlogindate(user.getLastLoginDate())
                .lastLoginDate(user.getLastLoginDate())
                .lastLoginTime(user.getLastLoginTime())
                .createdBy(user.getCreatedBy())
                .createdDate(user.getCreatedAt())
                .createdAt(user.getCreatedAt())
                .verifiedBy(user.getVerifiedBy())
                .verifiedDate(user.getVerifiedDate())
                .modifiedBy(user.getModifiedBy())
                .modifiedDate(user.getUpdatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }


    private boolean isInternalAdmin(UserPrincipal principal) {
        return principal != null && (
                ApplicationConstants.Roles.INTERNAL_ADMIN.equalsIgnoreCase(principal.getRole()) ||
                ApplicationConstants.Roles.SUPER_ADMIN.equalsIgnoreCase(principal.getRole()) ||
                ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(principal.getUserType())
        );
    }

    private OrganizationRole autoSeedRole(String roleName) {
        String name = roleName != null ? roleName.trim().toUpperCase() : "ADMIN";
        String panel = "CUSTOMER".equalsIgnoreCase(name) ? "CUSTOMER" : "BANK_NBFC";
        String description = switch (name) {
            case "SUPER_ADMIN" -> "Bank/NBFC Super Admin — full control within this Bank/NBFC";
            case "ADMIN" -> "Bank/NBFC internal admin — configures org and manages users";
            case "MAKER" -> "Creates and initiates loan records for Checker approval";
            case "CHECKER" -> "Reviews and approves Maker actions";
            case "VIEWER" -> "Read-only access";
            case "CUSTOMER" -> "Loan applicant";
            default -> name + " role";
        };
        return organizationRoleRepository.save(OrganizationRole.builder()
                .name(name)
                .panel(panel)
                .description(description)
                .build());
    }
}
