package com.bank.los.administration.lookup;

import com.bank.los.administration.audit.service.AdminAuditService;
import com.bank.los.administration.lookup.dto.*;
import com.bank.los.administration.lookup.service.AdministrationLookupService;
import com.bank.los.administration.master.entity.MasterLookupSubType;
import com.bank.los.administration.master.entity.MasterLookupType;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.MasterLookupSubTypeRepository;
import com.bank.los.administration.master.repository.MasterLookupTypeRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.bank.master.entity.BankLookupSubType;
import com.bank.los.bank.master.entity.BankLookupType;
import com.bank.los.bank.master.repository.BankLookupSubTypeRepository;
import com.bank.los.bank.master.repository.BankLookupTypePermissionRepository;
import com.bank.los.bank.master.repository.BankLookupTypeRepository;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrationLookupServiceTest {

    @Mock
    private MasterLookupTypeRepository masterLookupTypeRepository;

    @Mock
    private MasterLookupSubTypeRepository masterLookupSubTypeRepository;

    @Mock
    private com.bank.los.administration.master.repository.MasterLookupTypePermissionRepository masterLookupTypePermissionRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private BankLookupTypeRepository bankLookupTypeRepository;

    @Mock
    private BankLookupSubTypeRepository bankLookupSubTypeRepository;

    @Mock
    private BankLookupTypePermissionRepository bankLookupTypePermissionRepository;

    @Mock
    private AdminAuditService adminAuditService;

    @InjectMocks
    private AdministrationLookupService lookupService;

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
    @DisplayName("Admin should create master lookup type successfully")
    void testCreateLookupType_Success() {
        CreateLookupTypeRequest request = CreateLookupTypeRequest.builder()
                .code("10008")
                .description("Collateral Type")
                .isFixed(false)
                .isActive(true)
                .build();

        when(masterLookupTypeRepository.existsByCode("10008")).thenReturn(false);
        when(masterLookupTypeRepository.save(any(MasterLookupType.class))).thenAnswer(i -> {
            MasterLookupType t = i.getArgument(0);
            t.setId(10L);
            return t;
        });

        LookupTypeResponse response = lookupService.createLookupType(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("10008", response.getCode());
        assertEquals("Collateral Type", response.getDescription());
        assertFalse(response.getIsFixed());
    }

    @Test
    @DisplayName("Admin creates master lookup type with custom permission codes (101, 102, 103)")
    void testCreateLookupType_WithPermissions() {
        CreateLookupTypeRequest request = CreateLookupTypeRequest.builder()
                .code("10010")
                .description("Custom Lookup")
                .isFixed(false)
                .isActive(true)
                .permissions(List.of("101", "102", "103"))
                .build();

        when(masterLookupTypeRepository.existsByCode("10010")).thenReturn(false);
        when(masterLookupTypeRepository.save(any(MasterLookupType.class))).thenAnswer(i -> {
            MasterLookupType t = i.getArgument(0);
            t.setId(11L);
            return t;
        });

        LookupTypeResponse response = lookupService.createLookupType(request, adminPrincipal);

        assertNotNull(response);
        assertEquals("10010", response.getCode());
        verify(masterLookupTypePermissionRepository, times(3)).save(any());
    }

    @Test
    @DisplayName("Admin creating duplicate lookup type code throws BusinessException")
    void testCreateLookupType_DuplicateCode() {
        CreateLookupTypeRequest request = CreateLookupTypeRequest.builder()
                .code("10001")
                .description("Duplicate")
                .build();

        when(masterLookupTypeRepository.existsByCode("10001")).thenReturn(true);

        assertThrows(BusinessException.class, () -> lookupService.createLookupType(request, adminPrincipal));
    }

    @Test
    @DisplayName("Deleting system fixed master lookup type throws BusinessException")
    void testDeleteLookupType_SystemFixed_ThrowsException() {
        MasterLookupType fixedType = MasterLookupType.builder()
                .id(1L)
                .code("10001")
                .description("Bank User Status")
                .isFixed(true)
                .build();

        when(masterLookupTypeRepository.findByCode("10001")).thenReturn(Optional.of(fixedType));

        assertThrows(BusinessException.class, () -> lookupService.deleteLookupType("10001"));
    }

    @Test
    @DisplayName("Admin should add sub type / option to master lookup")
    void testAddSubType_Success() {
        MasterLookupType type = MasterLookupType.builder()
                .id(2L)
                .code("10002")
                .description("Designation")
                .isFixed(false)
                .build();

        CreateLookupSubTypeRequest subTypeRequest = CreateLookupSubTypeRequest.builder()
                .subTypeCode("10")
                .subTypeDescription("Managing Director")
                .isFixed(false)
                .displayOrder(10)
                .build();

        when(masterLookupTypeRepository.findByCode("10002")).thenReturn(Optional.of(type));
        when(masterLookupSubTypeRepository.existsByLookupTypeCodeAndSubTypeCode("10002", "10")).thenReturn(false);
        when(masterLookupSubTypeRepository.save(any(MasterLookupSubType.class))).thenAnswer(i -> {
            MasterLookupSubType st = i.getArgument(0);
            st.setId(50L);
            return st;
        });

        LookupSubTypeResponse response = lookupService.addSubType("10002", subTypeRequest, adminPrincipal);

        assertNotNull(response);
        assertEquals("10", response.getSubTypeCode());
        assertEquals("Managing Director", response.getSubTypeDescription());
        assertEquals("10002", response.getLookupTypeCode());
    }

    @Test
    @DisplayName("Sync master lookups into Bank DB propagates catalogue")
    void testSyncMasterLookupsToBank() {
        Organization org = Organization.builder()
                .id(1L)
                .bankCode("HDFC01")
                .dbName("los_hdfc01_db")
                .build();

        when(organizationRepository.findByCode("HDFC01")).thenReturn(Optional.of(org));

        MasterLookupType mt = MasterLookupType.builder().id(1L).code("10001").description("Status").isFixed(true).isActive(true).build();
        MasterLookupSubType mst = MasterLookupSubType.builder().id(1L).lookupTypeCode("10001").subTypeCode("1").subTypeDescription("Entered").isFixed(true).build();

        when(masterLookupTypeRepository.findAll()).thenReturn(List.of(mt));
        when(masterLookupSubTypeRepository.findAll()).thenReturn(List.of(mst));

        when(bankLookupTypeRepository.findByCode("10001")).thenReturn(Optional.empty());
        when(bankLookupTypeRepository.save(any(BankLookupType.class))).thenAnswer(i -> i.getArgument(0));

        when(bankLookupSubTypeRepository.findByLookupTypeCodeAndSubTypeCode("10001", "1")).thenReturn(Optional.empty());
        when(bankLookupSubTypeRepository.save(any(BankLookupSubType.class))).thenAnswer(i -> i.getArgument(0));

        assertDoesNotThrow(() -> lookupService.syncMasterLookupsToBank("HDFC01"));
        verify(bankLookupTypeRepository, atLeastOnce()).save(any(BankLookupType.class));
        verify(bankLookupSubTypeRepository, atLeastOnce()).save(any(BankLookupSubType.class));
    }

    @Test
    @DisplayName("Admin can update bank lookup permissions")
    void testUpdateBankLookupPermissions() {
        Organization org = Organization.builder()
                .id(1L)
                .bankCode("HDFC01")
                .dbName("los_hdfc01_db")
                .build();

        when(organizationRepository.findByCode("HDFC01")).thenReturn(Optional.of(org));

        BankLookupType loanType = BankLookupType.builder()
                .id(4L)
                .code("10004")
                .description("Loan Type")
                .isFixed(false)
                .build();

        when(bankLookupTypeRepository.findByCode("10004")).thenReturn(Optional.of(loanType));
        when(bankLookupTypeRepository.save(any(BankLookupType.class))).thenAnswer(i -> i.getArgument(0));

        UpdateBankLookupPermissionsRequest req = UpdateBankLookupPermissionsRequest.builder()
                .permissions(Set.of("VIEW", "ADD", "ACTIVATE"))
                .build();

        LookupTypeResponse response = lookupService.updateBankLookupPermissions("HDFC01", "10004", req, adminPrincipal);

        assertNotNull(response);
        verify(bankLookupTypePermissionRepository, times(1)).deleteByLookupTypeCode("10004");
        verify(adminAuditService, times(1)).logAdminAction(any(), any(), eq("UPDATE_BANK_LOOKUP_PERMISSIONS"), any(), eq("HDFC01"), any(), any());
    }
}
