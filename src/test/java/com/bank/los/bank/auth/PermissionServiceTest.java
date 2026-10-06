package com.bank.los.bank.auth;

import com.bank.los.bank.auth.service.PermissionService;
import com.bank.los.common.constant.ApplicationConstants;
import com.bank.los.config.BankDataSourceProvider;
import com.bank.los.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private BankDataSourceProvider bankDataSourceProvider;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatementRole;

    @Mock
    private PreparedStatement preparedStatementDesignation;

    @Mock
    private PreparedStatement preparedStatementOverride;

    @Mock
    private ResultSet resultSetRole;

    @Mock
    private ResultSet resultSetDesignation;

    @Mock
    private ResultSet resultSetOverride;

    @InjectMocks
    private PermissionService permissionService;

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(bankDataSourceProvider.getBankDataSource("los_hdfc01_db")).thenReturn(dataSource);
        lenient().when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    @DisplayName("Should resolve base role permissions correctly")
    void testGetEffectivePermissions_BaseRoleOnly() throws Exception {
        when(connection.prepareStatement(contains("identity.role_permissions"))).thenReturn(preparedStatementRole);
        when(preparedStatementRole.executeQuery()).thenReturn(resultSetRole);
        when(resultSetRole.next()).thenReturn(true, true, false);
        when(resultSetRole.getString("code")).thenReturn(
                ApplicationConstants.Permissions.USER_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW
        );

        when(connection.prepareStatement(contains("identity.permission_overrides"))).thenReturn(preparedStatementOverride);
        when(preparedStatementOverride.executeQuery()).thenReturn(resultSetOverride);
        when(resultSetOverride.next()).thenReturn(false);

        List<String> perms = permissionService.getEffectivePermissions("los_hdfc01_db", "ADMIN", null);

        assertNotNull(perms);
        assertEquals(2, perms.size());
        assertTrue(perms.contains(ApplicationConstants.Permissions.USER_VIEW));
        assertTrue(perms.contains(ApplicationConstants.Permissions.LOOKUP_BANK_VIEW));
    }

    @Test
    @DisplayName("Designation mapped to Role should inherit base role permissions")
    void testGetEffectivePermissions_DesignationInheritsRole() throws Exception {
        // Query for MAKER role permissions
        PreparedStatement psMaker = mock(PreparedStatement.class);
        ResultSet rsMaker = mock(ResultSet.class);
        when(psMaker.executeQuery()).thenReturn(rsMaker);
        when(rsMaker.next()).thenReturn(true, false);
        when(rsMaker.getString("code")).thenReturn(ApplicationConstants.Permissions.LOAN_APPLICATION_CREATE);

        // Query for mapped role of Officer -> ADMIN
        PreparedStatement psMap = mock(PreparedStatement.class);
        ResultSet rsMap = mock(ResultSet.class);
        when(psMap.executeQuery()).thenReturn(rsMap);
        when(rsMap.next()).thenReturn(true, false);
        when(rsMap.getString("name")).thenReturn("ADMIN");

        // Query for ADMIN role permissions
        PreparedStatement psAdmin = mock(PreparedStatement.class);
        ResultSet rsAdmin = mock(ResultSet.class);
        when(psAdmin.executeQuery()).thenReturn(rsAdmin);
        when(rsAdmin.next()).thenReturn(true, false);
        when(rsAdmin.getString("code")).thenReturn(ApplicationConstants.Permissions.LOOKUP_BANK_VIEW);

        PreparedStatement psOverride = mock(PreparedStatement.class);
        ResultSet rsOverride = mock(ResultSet.class);
        when(psOverride.executeQuery()).thenReturn(rsOverride);
        when(rsOverride.next()).thenReturn(false);

        when(connection.prepareStatement(contains("identity.designation_role_mappings"))).thenReturn(psMap);
        when(connection.prepareStatement(contains("identity.role_permissions"))).thenReturn(psMaker, psAdmin);
        when(connection.prepareStatement(contains("identity.permission_overrides"))).thenReturn(psOverride);

        List<String> perms = permissionService.getEffectivePermissions("los_hdfc01_db", "MAKER", "Officer");

        assertNotNull(perms);
        assertTrue(perms.contains(ApplicationConstants.Permissions.LOAN_APPLICATION_CREATE));
        assertTrue(perms.contains(ApplicationConstants.Permissions.LOOKUP_BANK_VIEW));
    }

    @Test
    @DisplayName("Bank can DENY an inherited permission on Designation without modifying master or base role")
    void testGetEffectivePermissions_DesignationDenyOverride() throws Exception {
        PreparedStatement psAdmin = mock(PreparedStatement.class);
        ResultSet rsAdmin = mock(ResultSet.class);
        when(psAdmin.executeQuery()).thenReturn(rsAdmin);
        when(rsAdmin.next()).thenReturn(true, true, false);
        when(rsAdmin.getString("code")).thenReturn(
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_DELETE
        );

        PreparedStatement psMap = mock(PreparedStatement.class);
        ResultSet rsMap = mock(ResultSet.class);
        when(psMap.executeQuery()).thenReturn(rsMap);
        when(rsMap.next()).thenReturn(false);

        PreparedStatement psRoleOverride = mock(PreparedStatement.class);
        ResultSet rsRoleOverride = mock(ResultSet.class);
        when(psRoleOverride.executeQuery()).thenReturn(rsRoleOverride);
        when(rsRoleOverride.next()).thenReturn(false);

        // Designation DENY override
        PreparedStatement psDesigOverride = mock(PreparedStatement.class);
        ResultSet rsDesigOverride = mock(ResultSet.class);
        when(psDesigOverride.executeQuery()).thenReturn(rsDesigOverride);
        when(rsDesigOverride.next()).thenReturn(true, false);
        when(rsDesigOverride.getString("permission_code")).thenReturn(ApplicationConstants.Permissions.LOOKUP_BANK_DELETE);
        when(rsDesigOverride.getString("effect")).thenReturn("DENY");

        when(connection.prepareStatement(contains("identity.role_permissions"))).thenReturn(psAdmin);
        when(connection.prepareStatement(contains("identity.designation_role_mappings"))).thenReturn(psMap);
        when(connection.prepareStatement(contains("identity.permission_overrides"))).thenReturn(psRoleOverride, psDesigOverride);

        List<String> perms = permissionService.getEffectivePermissions("los_hdfc01_db", "ADMIN", "General Manager");

        assertNotNull(perms);
        assertTrue(perms.contains(ApplicationConstants.Permissions.LOOKUP_BANK_VIEW));
        assertFalse(perms.contains(ApplicationConstants.Permissions.LOOKUP_BANK_DELETE),
                "LOOKUP_BANK_DELETE must be excluded by designation DENY override");
    }

    @Test
    @DisplayName("Internal Super Admin has all system permissions by default")
    void testInternalAdminHasAllPermissions() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .email("admin@losplatform.com")
                .role("INTERNAL_ADMIN")
                .build();

        assertTrue(permissionService.hasEffectivePermission(principal, ApplicationConstants.Permissions.LOOKUP_MASTER_VIEW));
        assertTrue(permissionService.hasEffectivePermission(principal, ApplicationConstants.Permissions.LOOKUP_BANK_ADD));
    }
}
