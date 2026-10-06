package com.bank.los.administration.rbac;

import com.bank.los.administration.audit.service.AdminAuditService;
import com.bank.los.administration.master.entity.MasterPermission;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.MasterPermissionRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.administration.rbac.dto.BankRbacSummaryResponse;
import com.bank.los.administration.rbac.dto.CreateMasterPermissionRequest;
import com.bank.los.administration.rbac.dto.MasterPermissionResponse;
import com.bank.los.administration.rbac.service.AdministrationRbacService;
import com.bank.los.bank.master.entity.Permission;
import com.bank.los.bank.master.repository.PermissionRepository;
import com.bank.los.bank.rbac.service.BankRbacService;
import com.bank.los.common.exception.BusinessException;
import com.bank.los.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationRbacServiceTest {

    @Mock
    private MasterPermissionRepository masterPermissionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private BankRbacService bankRbacService;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdministrationRbacService administrationRbacService;

    private UserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        adminPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("admin@losplatform.com")
                .role("INTERNAL_ADMIN")
                .build();
    }

    @Test
    @DisplayName("Admin creates master permission successfully")
    void testCreatePermission_Success() {
        CreateMasterPermissionRequest request = CreateMasterPermissionRequest.builder()
                .code("LOOKUP_BANK_EXPORT")
                .description("Export bank lookups")
                .module("LOOKUP")
                .isSystem(false)
                .build();

        when(masterPermissionRepository.existsByCode("LOOKUP_BANK_EXPORT")).thenReturn(false);
        when(masterPermissionRepository.save(any(MasterPermission.class))).thenAnswer(i -> {
            MasterPermission p = i.getArgument(0);
            p.setId(50);
            return p;
        });

        MasterPermissionResponse response = administrationRbacService.createPermission(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("LOOKUP_BANK_EXPORT", response.getCode());
        assertEquals("LOOKUP", response.getModule());
        verify(masterPermissionRepository, times(1)).save(any(MasterPermission.class));
        verify(adminAuditService, times(1)).logAdminAction(any(), any(), eq("CREATE_MASTER_PERMISSION"), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Admin creating duplicate master permission code throws BusinessException")
    void testCreatePermission_DuplicateCode_ThrowsException() {
        CreateMasterPermissionRequest request = CreateMasterPermissionRequest.builder()
                .code("LOOKUP_BANK_VIEW")
                .description("Duplicate")
                .module("LOOKUP")
                .build();

        when(masterPermissionRepository.existsByCode("LOOKUP_BANK_VIEW")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                administrationRbacService.createPermission(request, adminPrincipal));
    }

    @Test
    @DisplayName("Admin can inspect Bank RBAC summary including roles, designations, and overrides")
    void testGetBankRbacSummary() {
        Organization org = Organization.builder()
                .id(1L)
                .bankCode("HDFC01")
                .bankName("HDFC Bank")
                .dbName("los_hdfc01_db")
                .build();

        when(organizationRepository.findByCode("HDFC01")).thenReturn(Optional.of(org));
        when(bankRbacService.getAllRolesWithPermissions("los_hdfc01_db")).thenReturn(List.of());
        when(bankRbacService.getAllDesignationMappings("los_hdfc01_db")).thenReturn(List.of());
        when(bankRbacService.getAllPermissionOverrides("los_hdfc01_db")).thenReturn(List.of());

        BankRbacSummaryResponse summary = administrationRbacService.getBankRbacSummary("HDFC01");

        assertNotNull(summary);
        assertEquals("HDFC01", summary.getBankCode());
        assertEquals("HDFC Bank", summary.getBankName());
        assertEquals("los_hdfc01_db", summary.getDbName());
    }

    @Test
    @DisplayName("Sync master permissions to bank adds missing permissions without overwriting existing")
    void testSyncPermissionsToBank() {
        Organization org = Organization.builder()
                .id(1L)
                .bankCode("HDFC01")
                .dbName("los_hdfc01_db")
                .build();

        when(organizationRepository.findByCode("HDFC01")).thenReturn(Optional.of(org));

        MasterPermission mp1 = MasterPermission.builder().id(1).code("PERM_1").description("Desc 1").module("MOD").build();
        MasterPermission mp2 = MasterPermission.builder().id(2).code("PERM_2").description("Desc 2").module("MOD").build();
        when(masterPermissionRepository.findAll()).thenReturn(List.of(mp1, mp2));

        when(permissionRepository.findByCode("PERM_1")).thenReturn(Optional.of(Permission.builder().id(1).code("PERM_1").build()));
        when(permissionRepository.findByCode("PERM_2")).thenReturn(Optional.empty());
        when(permissionRepository.save(any(Permission.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> administrationRbacService.syncPermissionsToBank("HDFC01"));
        verify(permissionRepository, times(1)).save(any(Permission.class));
    }
}
