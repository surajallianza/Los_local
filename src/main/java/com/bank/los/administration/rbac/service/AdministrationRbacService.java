package com.bank.los.administration.rbac.service;

import com.bank.los.administration.audit.service.AdminAuditService;
import com.bank.los.administration.master.entity.MasterPermission;
import com.bank.los.administration.master.entity.MasterRole;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.MasterPermissionRepository;
import com.bank.los.administration.master.repository.MasterRoleRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.administration.rbac.dto.*;
import com.bank.los.bank.master.entity.OrganizationRole;
import com.bank.los.bank.master.entity.Permission;
import com.bank.los.bank.master.repository.DesignationRoleMappingRepository;
import com.bank.los.bank.master.repository.OrganizationRoleRepository;
import com.bank.los.bank.master.repository.PermissionOverrideRepository;
import com.bank.los.bank.master.repository.PermissionRepository;
import com.bank.los.bank.rbac.dto.BankRolePermissionResponse;
import com.bank.los.bank.rbac.dto.DesignationRoleMappingResponse;
import com.bank.los.bank.rbac.dto.PermissionOverrideResponse;
import com.bank.los.bank.rbac.service.BankRbacService;
import com.bank.los.common.exception.BusinessException;
import com.bank.los.common.exception.ResourceNotFoundException;
import com.bank.los.config.BankContext;
import com.bank.los.config.BankDataSourceProvider;
import com.bank.los.config.OrganizationContext;
import com.bank.los.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministrationRbacService {

    private final MasterPermissionRepository masterPermissionRepository;
    private final MasterRoleRepository masterRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final PermissionRepository permissionRepository;
    private final DesignationRoleMappingRepository designationRoleMappingRepository;
    private final PermissionOverrideRepository permissionOverrideRepository;
    private final BankDataSourceProvider bankDataSourceProvider;
    private final BankRbacService bankRbacService;
    private final AdminAuditService adminAuditService;

    // =========================================================================
    //  MASTER PERMISSIONS MANAGEMENT (Master DB)
    // =========================================================================

    @Transactional(readOnly = true)
    public List<MasterPermissionResponse> getAllPermissions() {
        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        return masterPermissionRepository.findAll().stream()
                .map(this::mapToPermissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MasterPermissionResponse createPermission(CreateMasterPermissionRequest request, UserPrincipal principal) {
        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        String code = request.getCode().trim().toUpperCase();
        if (masterPermissionRepository.existsByCode(code)) {
            throw new BusinessException("Permission with code '" + code + "' already exists in Master DB.");
        }

        MasterPermission permission = MasterPermission.builder()
                .code(code)
                .description(request.getDescription().trim())
                .module(request.getModule().trim().toUpperCase())
                .isSystem(Boolean.TRUE.equals(request.getIsSystem()))
                .build();

        MasterPermission saved = masterPermissionRepository.save(permission);
        log.info("Created Master Permission code={} by admin={}", saved.getCode(), principal != null ? principal.getEmail() : "SYSTEM");

        if (principal != null) {
            adminAuditService.logAdminAction(
                    principal.getId(), principal.getUsername(),
                    "CREATE_MASTER_PERMISSION", "PERMISSIONS", null,
                    "Created permission: " + saved.getCode(), null
            );
        }

        return mapToPermissionResponse(saved);
    }

    @Transactional
    public MasterPermissionResponse updatePermission(Integer id, UpdateMasterPermissionRequest request, UserPrincipal principal) {
        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        MasterPermission permission = masterPermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Master permission not found with ID: " + id));

        permission.setDescription(request.getDescription().trim());
        permission.setModule(request.getModule().trim().toUpperCase());

        MasterPermission saved = masterPermissionRepository.save(permission);
        log.info("Updated Master Permission id={} code={} by admin={}", saved.getId(), saved.getCode(), principal != null ? principal.getEmail() : "SYSTEM");

        if (principal != null) {
            adminAuditService.logAdminAction(
                    principal.getId(), principal.getUsername(),
                    "UPDATE_MASTER_PERMISSION", "PERMISSIONS", null,
                    "Updated permission: " + saved.getCode(), null
            );
        }

        return mapToPermissionResponse(saved);
    }

    // =========================================================================
    //  BANK RBAC SUPERVISION & INSPECTION
    // =========================================================================

    public BankRbacSummaryResponse getBankRbacSummary(String bankCode) {
        Organization org = organizationRepository.findByCode(bankCode)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found with code: " + bankCode));

        String prevBank = BankContext.getCurrentBank();
        String prevOrg = OrganizationContext.getCurrentOrganization();

        try {
            BankContext.setCurrentBank(org.getDbName());
            OrganizationContext.setCurrentOrganization(org.getDbName());

            List<BankRolePermissionResponse> roles = bankRbacService.getAllRolesWithPermissions(org.getDbName());
            List<DesignationRoleMappingResponse> designations = bankRbacService.getAllDesignationMappings(org.getDbName());
            List<PermissionOverrideResponse> overrides = bankRbacService.getAllPermissionOverrides(org.getDbName());

            return BankRbacSummaryResponse.builder()
                    .bankCode(org.getBankCode())
                    .bankName(org.getBankName())
                    .dbName(org.getDbName())
                    .roles(roles)
                    .designations(designations)
                    .permissionOverrides(overrides)
                    .build();
        } finally {
            BankContext.setCurrentBank(prevBank != null ? prevBank : BankContext.MASTER_DB_NAME);
            OrganizationContext.setCurrentOrganization(prevOrg != null ? prevOrg : OrganizationContext.MASTER_ORG_ID);
        }
    }

    @Transactional
    public BankRolePermissionResponse updateBankRolePermissions(String bankCode, String roleName, List<String> permissions, UserPrincipal principal) {
        Organization org = organizationRepository.findByCode(bankCode)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found with code: " + bankCode));

        String prevBank = BankContext.getCurrentBank();
        String prevOrg = OrganizationContext.getCurrentOrganization();

        try {
            BankContext.setCurrentBank(org.getDbName());
            OrganizationContext.setCurrentOrganization(org.getDbName());

            BankRolePermissionResponse response = bankRbacService.updateRolePermissions(roleName, permissions, principal);

            if (principal != null) {
                adminAuditService.logAdminAction(
                        principal.getId(), principal.getEmail(), "UPDATE_BANK_ROLE_PERMISSIONS",
                        "RBAC", bankCode,
                        "Updated permissions for role " + roleName + " in bank " + bankCode + ": " + permissions,
                        null
                );
            }

            return response;
        } finally {
            BankContext.setCurrentBank(prevBank != null ? prevBank : BankContext.MASTER_DB_NAME);
            OrganizationContext.setCurrentOrganization(prevOrg != null ? prevOrg : OrganizationContext.MASTER_ORG_ID);
        }
    }

    public void syncPermissionsToBank(String bankCode) {
        Organization org = organizationRepository.findByCode(bankCode)
                .orElseThrow(() -> new ResourceNotFoundException("Bank not found with code: " + bankCode));

        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        List<MasterPermission> masterPerms = masterPermissionRepository.findAll();

        String prevBank = BankContext.getCurrentBank();
        String prevOrg = OrganizationContext.getCurrentOrganization();

        try {
            BankContext.setCurrentBank(org.getDbName());
            OrganizationContext.setCurrentOrganization(org.getDbName());

            for (MasterPermission mp : masterPerms) {
                if (permissionRepository.findByCode(mp.getCode()).isEmpty()) {
                    permissionRepository.save(Permission.builder()
                            .code(mp.getCode())
                            .description(mp.getDescription())
                            .module(mp.getModule())
                            .build());
                }
            }
            log.info("Master permissions synced to bank={}", bankCode);
        } finally {
            BankContext.setCurrentBank(prevBank != null ? prevBank : BankContext.MASTER_DB_NAME);
            OrganizationContext.setCurrentOrganization(prevOrg != null ? prevOrg : OrganizationContext.MASTER_ORG_ID);
        }
    }

    public void syncPermissionsToAllBanks() {
        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        List<Organization> orgs = organizationRepository.findAll();

        for (Organization org : orgs) {
            try {
                syncPermissionsToBank(org.getBankCode());
            } catch (Exception ex) {
                log.warn("Failed to sync permissions to bank {}: {}", org.getBankCode(), ex.getMessage());
            }
        }
    }

    private MasterPermissionResponse mapToPermissionResponse(MasterPermission p) {
        return MasterPermissionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .description(p.getDescription())
                .module(p.getModule())
                .isSystem(p.getIsSystem())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
