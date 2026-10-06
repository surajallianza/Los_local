

package com.bank.los.db.init;

import com.bank.los.administration.master.entity.InternalUser;
import com.bank.los.administration.master.entity.LoginDirectory;
import com.bank.los.administration.master.entity.MasterLookupSubType;

import com.bank.los.administration.master.entity.MasterLookupType;
import com.bank.los.administration.master.entity.MasterRole;
import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.InternalUserRepository;
import com.bank.los.administration.master.repository.LoginDirectoryRepository;
import com.bank.los.administration.master.repository.MasterLookupSubTypeRepository;
import com.bank.los.administration.master.repository.MasterLookupTypeRepository;
import com.bank.los.administration.master.repository.MasterRoleRepository;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.bank.master.entity.BankLookupSubType;
import com.bank.los.bank.master.entity.BankLookupType;
import com.bank.los.bank.master.entity.Branch;
import com.bank.los.bank.master.entity.Customer;
import com.bank.los.bank.master.entity.OrganizationRole;
import com.bank.los.bank.master.entity.OrganizationUser;
import com.bank.los.bank.master.entity.Permission;
import com.bank.los.bank.master.repository.BankLookupSubTypeRepository;
import com.bank.los.bank.master.repository.BankLookupTypeRepository;
import com.bank.los.bank.master.repository.BranchRepository;
import com.bank.los.bank.master.repository.CustomerRepository;
import com.bank.los.bank.master.repository.OrganizationRoleRepository;
import com.bank.los.bank.master.repository.OrganizationUserRepository;
import com.bank.los.bank.master.repository.PermissionRepository;
import com.bank.los.common.constant.ApplicationConstants;
import com.bank.los.config.BankContext;
import com.bank.los.config.BankDataSourceProvider;
import com.bank.los.config.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final MasterRoleRepository masterRoleRepository;
    private final OrganizationRepository organizationRepository;
    private final InternalUserRepository internalUserRepository;
    private final LoginDirectoryRepository loginDirectoryRepository;
    private final MasterLookupTypeRepository masterLookupTypeRepository;
    private final MasterLookupSubTypeRepository masterLookupSubTypeRepository;
    private final com.bank.los.administration.master.repository.MasterLookupTypePermissionRepository masterLookupTypePermissionRepository;
    private final com.bank.los.administration.master.repository.MasterPermissionRepository masterPermissionRepository;

    private final OrganizationRoleRepository organizationRoleRepository;
    private final BranchRepository branchRepository;
    private final OrganizationUserRepository organizationUserRepository;
    private final CustomerRepository customerRepository;
    private final PermissionRepository permissionRepository;
    private final BankLookupTypeRepository bankLookupTypeRepository;
    private final BankLookupSubTypeRepository bankLookupSubTypeRepository;
    private final com.bank.los.bank.master.repository.BankLookupTypePermissionRepository bankLookupTypePermissionRepository;
    private final com.bank.los.bank.master.repository.DesignationRoleMappingRepository designationRoleMappingRepository;
    private final com.bank.los.bank.master.repository.PermissionOverrideRepository permissionOverrideRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankDataSourceProvider bankDataSourceProvider;
    private final com.bank.los.security.TenantResolutionService tenantResolutionService;


    @Override
    public void run(String... args) {
        log.info("Checking database initialization and seed data...");
        try {
            seedMasterDatabase();
            seedOrganizationDatabase("los_hdfc01_db", "HDFC01");
            seedOrganizationDatabase("los_bajaj02_db", "BAJAJ02");
            log.info("Database seeding completed successfully.");
        } catch (Exception e) {
            log.warn("Notice during seeding: {}", e.getMessage());
        } finally {
            BankContext.clear();
        }
    }

    // =====================================================================
    //  MASTER DATABASE (Administration Panel)
    // =====================================================================

    private void seedMasterDatabase() {
        BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);

        // Internal platform admin role
        MasterRole adminRole = masterRoleRepository.findByName(ApplicationConstants.Roles.INTERNAL_ADMIN)
                .orElseGet(() -> masterRoleRepository.save(MasterRole.builder()
                        .name(ApplicationConstants.Roles.INTERNAL_ADMIN)
                        .panel(ApplicationConstants.Panels.INTERNAL)
                        .description("Platform team; manages organizations and system features")
                        .build()));

        // Sample organizations
        Organization hdfc = organizationRepository.findByBankCode("HDFC01")
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .bankName("HDFC Bank")
                        .bankCode("HDFC01")
                        .bankType("BANK")
                        .status("ACTIVE")
                        .contactEmail("contact@hdfcbank.com")
                        .contactPhone("+912261606161")
                        .dbName("los_hdfc01_db")
                        .dbHost("localhost")
                        .dbPort(5432)
                        .build()));

        Organization bajaj = organizationRepository.findByBankCode("BAJAJ02")
                .orElseGet(() -> organizationRepository.save(Organization.builder()
                        .bankName("Bajaj Finance Limited")
                        .bankCode("BAJAJ02")
                        .bankType("NBFC")
                        .status("ACTIVE")
                        .contactEmail("customercare@bajajfinserv.in")
                        .contactPhone("+912071576403")
                        .dbName("los_bajaj02_db")
                        .dbHost("localhost")
                        .dbPort(5432)
                        .build()));

        tenantResolutionService.cacheOrganization(hdfc);
        tenantResolutionService.cacheOrganization(bajaj);


        // Super Admin user (platform team)
        if (internalUserRepository.findByEmail("admin@losplatform.com").isEmpty()) {
            InternalUser internalAdmin = internalUserRepository.save(InternalUser.builder()
                    .empNo("EMP-MST-001")
                    .username("superadmin")
                    .role(adminRole)
                    .firstName("Super")
                    .middleName("")
                    .lastName("Administrator")
                    .email("admin@losplatform.com")
                    .mobile("+919999900000")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .isActive(true)
                    .status(ApplicationConstants.UserStatus.OPERATIVE)
                    .loginOnHolidays(true)
                    .inactiveSessionTimeout(3600)
                    .build());

            loginDirectoryRepository.save(LoginDirectory.builder()
                    .userCode(internalAdmin.getEmpNo())
                    .email(internalAdmin.getEmail())
                    .phone(internalAdmin.getMobile())
                    .organization(hdfc)
                    .userType(ApplicationConstants.UserTypes.INTERNAL)
                    .build());
        }

        // Login directory entries for HDFC demo staff
        registerLoginDirectoryEntry("admin@hdfcbank.com", "EMP-HDFC-001", "+919876500001", hdfc, ApplicationConstants.UserTypes.STAFF);
        registerLoginDirectoryEntry("maker@hdfcbank.com", "EMP-HDFC-002", "+919876500002", hdfc, ApplicationConstants.UserTypes.STAFF);
        registerLoginDirectoryEntry("checker@hdfcbank.com", "EMP-HDFC-003", "+919876500003", hdfc, ApplicationConstants.UserTypes.STAFF);
        registerLoginDirectoryEntry("viewer@hdfcbank.com", "EMP-HDFC-004", "+919876500004", hdfc, ApplicationConstants.UserTypes.STAFF);
        registerLoginDirectoryEntry("rajesh.kumar@gmail.com", "CUST-HDFC-1001", "+919876500005", hdfc, ApplicationConstants.UserTypes.CUSTOMER);

        // Login directory entries for Bajaj demo staff
        registerLoginDirectoryEntry("admin@bajajfinance.com", "EMP-BJ-001", "+919876500010", bajaj, ApplicationConstants.UserTypes.STAFF);

        // Seed Master Lookups Catalogue (Tables 51001 and 51101)
        seedMasterLookups();

        // Seed Master Permissions (Table identity.permissions in Master DB)
        seedMasterPermissions();
    }


    private void registerLoginDirectoryEntry(String email, String userCode, String phone, Organization org, String userType) {
        if (loginDirectoryRepository.findByEmail(email).isEmpty()) {
            loginDirectoryRepository.save(LoginDirectory.builder()
                    .email(email)
                    .userCode(userCode)
                    .phone(phone)
                    .organization(org)
                    .userType(userType)
                    .build());
        }
    }

    // =====================================================================
    //  ORGANIZATION DATABASE (Bank / NBFC Panel)
    // =====================================================================

    private void seedOrganizationDatabase(String orgDbName, String orgCode) {
        BankContext.setCurrentBank(orgDbName);
        BankContext.setCurrentBankCode(orgCode);

        // ── Organization Roles ──────────────────────────────────────────
        OrganizationRole superAdminRole = seedRole("SUPER_ADMIN", "BANK_NBFC", "Bank/NBFC Super Admin — full control within this Bank/NBFC");
        OrganizationRole adminRole = seedRole("ADMIN", "BANK_NBFC", "Bank/NBFC internal admin — configures org and assigns roles");
        OrganizationRole makerRole = seedRole("MAKER", "BANK_NBFC", "Creates and initiates loan records for Checker approval");
        OrganizationRole checkerRole = seedRole("CHECKER", "BANK_NBFC", "Reviews and approves Maker actions");
        OrganizationRole viewerRole = seedRole("VIEWER", "BANK_NBFC", "Read-only access");
        seedRole("CUSTOMER", "CUSTOMER", "Loan applicant");

        // ── Branches ──────────────────────────────────────────────────────
        Branch mainBranch = branchRepository.findByCode(orgCode + "-BR-01")
                .orElseGet(() -> branchRepository.save(Branch.builder()
                        .name(orgCode.equals("HDFC01") ? "Mumbai Fort Branch" : "Pune Central Branch")
                        .code(orgCode + "-BR-01")
                        .address("Nariman Point / Senapati Bapat Marg")
                        .city(orgCode.equals("HDFC01") ? "Mumbai" : "Pune")
                        .state("Maharashtra")
                        .pincode("400021")
                        .status("ACTIVE")
                        .build()));

        branchRepository.findByCode(orgCode + "-BR-02")
                .orElseGet(() -> branchRepository.save(Branch.builder()
                        .name("New Delhi Regional Branch")
                        .code(orgCode + "-BR-02")
                        .address("Connaught Place Block B")
                        .city("New Delhi")
                        .state("Delhi")
                        .pincode("110001")
                        .status("ACTIVE")
                        .build()));

        // ── Seed Permissions & Role Mappings ─────────────────────────────
        seedPermissions();

        List<String> adminPerms = List.of(
                ApplicationConstants.Permissions.USER_CREATE,
                ApplicationConstants.Permissions.USER_UPDATE,
                ApplicationConstants.Permissions.USER_VIEW,
                ApplicationConstants.Permissions.USER_DEACTIVATE,
                ApplicationConstants.Permissions.USER_VERIFY,
                ApplicationConstants.Permissions.USER_RESET_PASSWORD,
                ApplicationConstants.Permissions.BRANCH_CREATE,
                ApplicationConstants.Permissions.BRANCH_UPDATE,
                ApplicationConstants.Permissions.BRANCH_VIEW,
                ApplicationConstants.Permissions.REPORT_VIEW,
                ApplicationConstants.Permissions.REPORT_EXPORT,
                ApplicationConstants.Permissions.DASHBOARD_VIEW,
                ApplicationConstants.Permissions.DASHBOARD_ANALYTICS_VIEW,
                ApplicationConstants.Permissions.ROLE_PERMISSION_MANAGE,
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_ADD,
                ApplicationConstants.Permissions.LOOKUP_BANK_ADD_FROM_MASTER,
                ApplicationConstants.Permissions.LOOKUP_BANK_EDIT,
                ApplicationConstants.Permissions.LOOKUP_BANK_DELETE,
                ApplicationConstants.Permissions.LOOKUP_BANK_ACTIVATE,
                ApplicationConstants.Permissions.LOOKUP_BANK_DEACTIVATE
        );
        seedRolePermissions(orgDbName, superAdminRole, adminPerms);
        seedRolePermissions(orgDbName, adminRole, adminPerms);

        seedRolePermissions(orgDbName, makerRole, List.of(
                ApplicationConstants.Permissions.LOAN_APPLICATION_CREATE,
                ApplicationConstants.Permissions.LOAN_APPLICATION_EDIT,
                ApplicationConstants.Permissions.LOAN_APPLICATION_VIEW,
                ApplicationConstants.Permissions.LOAN_APPLICATION_SUBMIT,
                ApplicationConstants.Permissions.CUSTOMER_CREATE,
                ApplicationConstants.Permissions.CUSTOMER_EDIT,
                ApplicationConstants.Permissions.CUSTOMER_VIEW,
                ApplicationConstants.Permissions.DOCUMENT_UPLOAD,
                ApplicationConstants.Permissions.DOCUMENT_VIEW,
                ApplicationConstants.Permissions.DASHBOARD_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_ADD,
                ApplicationConstants.Permissions.LOOKUP_BANK_ADD_FROM_MASTER,
                ApplicationConstants.Permissions.LOOKUP_BANK_EDIT
        ));

        seedRolePermissions(orgDbName, checkerRole, List.of(
                ApplicationConstants.Permissions.LOAN_APPLICATION_VIEW,
                ApplicationConstants.Permissions.LOAN_APPLICATION_VERIFY,
                ApplicationConstants.Permissions.LOAN_APPLICATION_APPROVE,
                ApplicationConstants.Permissions.LOAN_APPLICATION_REJECT,
                ApplicationConstants.Permissions.CUSTOMER_VIEW,
                ApplicationConstants.Permissions.CUSTOMER_VIEW_ALL,
                ApplicationConstants.Permissions.DOCUMENT_VERIFY,
                ApplicationConstants.Permissions.DOCUMENT_VIEW,
                ApplicationConstants.Permissions.DASHBOARD_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_ACTIVATE,
                ApplicationConstants.Permissions.LOOKUP_BANK_DEACTIVATE
        ));

        seedRolePermissions(orgDbName, viewerRole, List.of(
                ApplicationConstants.Permissions.LOAN_APPLICATION_VIEW,
                ApplicationConstants.Permissions.CUSTOMER_VIEW,
                ApplicationConstants.Permissions.CUSTOMER_VIEW_ALL,
                ApplicationConstants.Permissions.DOCUMENT_VIEW,
                ApplicationConstants.Permissions.REPORT_VIEW,
                ApplicationConstants.Permissions.REPORT_EXPORT,
                ApplicationConstants.Permissions.DASHBOARD_VIEW,
                ApplicationConstants.Permissions.DASHBOARD_ANALYTICS_VIEW,
                ApplicationConstants.Permissions.LOOKUP_BANK_VIEW
        ));

        // ── Seed Designation -> Role Mappings ─────────────────────────────
        seedDesignationRoleMappings(adminRole, makerRole, checkerRole);

        // ── Seed Bank Customization Permission Overrides ───────────────────
        seedPermissionOverrides(orgCode);

        // ── Staff Users ───────────────────────────────────────────────────
        if ("HDFC01".equals(orgCode)) {
            createStaffUser("admin@hdfcbank.com", "EMP-HDFC-001", "admin", "Vikram", "Aditya", "Mehta", "+919876500001", "Admin@123", adminRole, mainBranch, "General Manager");
            createStaffUser("maker@hdfcbank.com", "EMP-HDFC-002", "maker01", "Rohan", "Kumar", "Verma", "+919876500002", "Maker@123", makerRole, mainBranch, "Officer");
            createStaffUser("checker@hdfcbank.com", "EMP-HDFC-003", "chk01", "Priyanka", "Devi", "Nair", "+919876500003", "Checker@123", checkerRole, mainBranch, "Asst. Manager");
            createStaffUser("viewer@hdfcbank.com", "EMP-HDFC-004", "view01", "Sanjay", "Rao", "Kulkarni", "+919876500004", "Viewer@123", viewerRole, mainBranch, "Clerk");

            // Demo customer
            if (customerRepository.findByEmail("rajesh.kumar@gmail.com").isEmpty()) {
                customerRepository.save(Customer.builder()
                        .customerCode("CUST-HDFC-1001")
                        .branch(mainBranch)
                        .firstName("Rajesh")
                        .middleName("")
                        .lastName("Kumar")
                        .email("rajesh.kumar@gmail.com")
                        .phone("+919876500005")
                        .passwordHash(passwordEncoder.encode("Customer@123"))
                        .isActive(true)
                        .build());
            }
        } else if ("BAJAJ02".equals(orgCode)) {
            createStaffUser("admin@bajajfinance.com", "EMP-BJ-001", "bj_admin", "Ananya", "R", "Deshmukh", "+919876500010", "Admin@123", adminRole, mainBranch, "Manager");
        }

        // Seed bank-level lookups
        seedBankLookups(orgDbName);
    }


    private OrganizationRole seedRole(String name, String panel, String description) {
        return organizationRoleRepository.findByName(name)
                .orElseGet(() -> organizationRoleRepository.save(OrganizationRole.builder()
                        .name(name)
                        .panel(panel)
                        .description(description)
                        .build()));
    }

    private void seedMasterPermissions() {
        List<Object[]> masterPerms = List.of(
                new Object[]{ApplicationConstants.Permissions.ORGANIZATION_CREATE, "Create new organizations", ApplicationConstants.PermissionModules.SYSTEM, true},
                new Object[]{ApplicationConstants.Permissions.ORGANIZATION_VIEW, "View organizations", ApplicationConstants.PermissionModules.SYSTEM, true},
                new Object[]{ApplicationConstants.Permissions.ORGANIZATION_UPDATE, "Update organizations", ApplicationConstants.PermissionModules.SYSTEM, true},
                new Object[]{ApplicationConstants.Permissions.SYSTEM_AUDIT_VIEW, "View system audit logs", ApplicationConstants.PermissionModules.SYSTEM, true},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_MASTER_VIEW, "View master lookups catalogue", ApplicationConstants.PermissionModules.LOOKUP, true},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_VIEW, "View bank lookup types and sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ADD, "Add custom bank lookup types and sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ADD_FROM_MASTER, "Import lookup sub-types from Master DB catalogue", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_EDIT, "Edit custom bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_DELETE, "Delete custom bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ACTIVATE, "Activate bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_DEACTIVATE, "Deactivate bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP, false},
                new Object[]{ApplicationConstants.Permissions.USER_CREATE, "Create new users", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.USER_UPDATE, "Update user details", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.USER_VIEW, "View user details", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.USER_DEACTIVATE, "Deactivate a user", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.USER_VERIFY, "Verify pending user (2nd admin)", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.USER_RESET_PASSWORD, "Reset a user's password", ApplicationConstants.PermissionModules.USER, false},
                new Object[]{ApplicationConstants.Permissions.BRANCH_CREATE, "Create branches", ApplicationConstants.PermissionModules.BRANCH, false},
                new Object[]{ApplicationConstants.Permissions.BRANCH_UPDATE, "Update branch details", ApplicationConstants.PermissionModules.BRANCH, false},
                new Object[]{ApplicationConstants.Permissions.BRANCH_VIEW, "View branches", ApplicationConstants.PermissionModules.BRANCH, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_CREATE, "Create loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_EDIT, "Edit loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_VIEW, "View loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_SUBMIT, "Submit loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_VERIFY, "Verify loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_APPROVE, "Approve loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_REJECT, "Reject loan applications", ApplicationConstants.PermissionModules.LOAN, false},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_CREATE, "Create customer records", ApplicationConstants.PermissionModules.CUSTOMER, false},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_EDIT, "Edit customer details", ApplicationConstants.PermissionModules.CUSTOMER, false},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_VIEW, "View own customers", ApplicationConstants.PermissionModules.CUSTOMER, false},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_VIEW_ALL, "View all customers in org", ApplicationConstants.PermissionModules.CUSTOMER, false},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_UPLOAD, "Upload documents", ApplicationConstants.PermissionModules.DOCUMENT, false},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_VERIFY, "Verify documents", ApplicationConstants.PermissionModules.DOCUMENT, false},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_VIEW, "View documents", ApplicationConstants.PermissionModules.DOCUMENT, false},
                new Object[]{ApplicationConstants.Permissions.REPORT_VIEW, "View reports", ApplicationConstants.PermissionModules.REPORT, false},
                new Object[]{ApplicationConstants.Permissions.REPORT_EXPORT, "Export reports", ApplicationConstants.PermissionModules.REPORT, false},
                new Object[]{ApplicationConstants.Permissions.DASHBOARD_VIEW, "View dashboard", ApplicationConstants.PermissionModules.DASHBOARD, false},
                new Object[]{ApplicationConstants.Permissions.DASHBOARD_ANALYTICS_VIEW, "View dashboard analytics", ApplicationConstants.PermissionModules.DASHBOARD, false},
                new Object[]{ApplicationConstants.Permissions.ROLE_PERMISSION_MANAGE, "Manage role permissions", ApplicationConstants.PermissionModules.SYSTEM, false}
        );

        for (Object[] p : masterPerms) {
            String code = (String) p[0];
            if (masterPermissionRepository.findByCode(code).isEmpty()) {
                masterPermissionRepository.save(com.bank.los.administration.master.entity.MasterPermission.builder()
                        .code(code)
                        .description((String) p[1])
                        .module((String) p[2])
                        .isSystem((Boolean) p[3])
                        .build());
            }
        }
    }

    private void seedPermissions() {
        List<Object[]> perms = List.of(
                // code, description, module
                new Object[]{ApplicationConstants.Permissions.USER_CREATE, "Create new users", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.USER_UPDATE, "Update user details", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.USER_VIEW, "View user details", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.USER_DEACTIVATE, "Deactivate a user", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.USER_VERIFY, "Verify pending user (2nd admin)", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.USER_RESET_PASSWORD, "Reset a user's password", ApplicationConstants.PermissionModules.USER},
                new Object[]{ApplicationConstants.Permissions.BRANCH_CREATE, "Create branches", ApplicationConstants.PermissionModules.BRANCH},
                new Object[]{ApplicationConstants.Permissions.BRANCH_UPDATE, "Update branch details", ApplicationConstants.PermissionModules.BRANCH},
                new Object[]{ApplicationConstants.Permissions.BRANCH_VIEW, "View branches", ApplicationConstants.PermissionModules.BRANCH},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_CREATE, "Create loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_EDIT, "Edit loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_VIEW, "View loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_SUBMIT, "Submit loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_VERIFY, "Verify loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_APPROVE, "Approve loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.LOAN_APPLICATION_REJECT, "Reject loan applications", ApplicationConstants.PermissionModules.LOAN},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_CREATE, "Create customer records", ApplicationConstants.PermissionModules.CUSTOMER},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_EDIT, "Edit customer details", ApplicationConstants.PermissionModules.CUSTOMER},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_VIEW, "View own customers", ApplicationConstants.PermissionModules.CUSTOMER},
                new Object[]{ApplicationConstants.Permissions.CUSTOMER_VIEW_ALL, "View all customers in org", ApplicationConstants.PermissionModules.CUSTOMER},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_UPLOAD, "Upload documents", ApplicationConstants.PermissionModules.DOCUMENT},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_VERIFY, "Verify documents", ApplicationConstants.PermissionModules.DOCUMENT},
                new Object[]{ApplicationConstants.Permissions.DOCUMENT_VIEW, "View documents", ApplicationConstants.PermissionModules.DOCUMENT},
                new Object[]{ApplicationConstants.Permissions.REPORT_VIEW, "View reports", ApplicationConstants.PermissionModules.REPORT},
                new Object[]{ApplicationConstants.Permissions.REPORT_EXPORT, "Export reports", ApplicationConstants.PermissionModules.REPORT},
                new Object[]{ApplicationConstants.Permissions.DASHBOARD_VIEW, "View dashboard", ApplicationConstants.PermissionModules.DASHBOARD},
                new Object[]{ApplicationConstants.Permissions.DASHBOARD_ANALYTICS_VIEW, "View dashboard analytics", ApplicationConstants.PermissionModules.DASHBOARD},
                new Object[]{ApplicationConstants.Permissions.ROLE_PERMISSION_MANAGE, "Manage role permissions", ApplicationConstants.PermissionModules.SYSTEM},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_VIEW, "View bank lookup types and sub-types", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ADD, "Add custom bank lookup types and sub-types", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ADD_FROM_MASTER, "Import lookup sub-types from Master DB catalogue", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_EDIT, "Edit custom bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_DELETE, "Delete custom bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_ACTIVATE, "Activate bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP},
                new Object[]{ApplicationConstants.Permissions.LOOKUP_BANK_DEACTIVATE, "Deactivate bank lookup sub-types", ApplicationConstants.PermissionModules.LOOKUP}
        );

        for (Object[] p : perms) {
            String code = (String) p[0];
            if (permissionRepository.findByCode(code).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(code)
                        .description((String) p[1])
                        .module((String) p[2])
                        .build());
            }
        }
    }

    private void seedDesignationRoleMappings(OrganizationRole adminRole, OrganizationRole makerRole, OrganizationRole checkerRole) {
        mapDesignation("Gen. Manager", adminRole);
        mapDesignation("General Manager", adminRole);
        mapDesignation("Manager", adminRole);
        mapDesignation("Dy. Manager", adminRole);
        mapDesignation("Asst. Manager", checkerRole);
        mapDesignation("Sr. Officer", makerRole);
        mapDesignation("Officer", makerRole);
        mapDesignation("Clerk", makerRole);
    }

    private void mapDesignation(String designation, OrganizationRole role) {
        if (designationRoleMappingRepository.findByDesignationIgnoreCase(designation).isEmpty()) {
            designationRoleMappingRepository.save(com.bank.los.bank.master.entity.DesignationRoleMapping.builder()
                    .designation(designation)
                    .role(role)
                    .isActive(true)
                    .build());
        }
    }

    private void seedPermissionOverrides(String orgCode) {
        if ("HDFC01".equals(orgCode)) {
            // General Manager -> LOOKUP_BANK_DELETE -> DENY
            createOverrideIfNotExists("DESIGNATION", "General Manager", ApplicationConstants.Permissions.LOOKUP_BANK_DELETE, "DENY", "Policy restriction: General Managers cannot delete bank lookups");
            createOverrideIfNotExists("DESIGNATION", "Gen. Manager", ApplicationConstants.Permissions.LOOKUP_BANK_DELETE, "DENY", "Policy restriction: General Managers cannot delete bank lookups");
        }
    }

    private void createOverrideIfNotExists(String targetType, String targetName, String permissionCode, String effect, String reason) {
        if (!permissionOverrideRepository.existsByTargetTypeAndTargetNameIgnoreCaseAndPermissionCodeIgnoreCase(targetType, targetName, permissionCode)) {
            permissionOverrideRepository.save(com.bank.los.bank.master.entity.PermissionOverride.builder()
                    .targetType(targetType)
                    .targetName(targetName)
                    .permissionCode(permissionCode)
                    .effect(effect)
                    .reason(reason)
                    .isActive(true)
                    .build());
        }
    }

    /**
     * Creates a staff user if they don't already exist.
     * Initial status is OPERATIVE (demo seed — in production new users start as PENDING_VERIFICATION).
     */
    private void createStaffUser(String email, String empNo, String username,
                                 String firstName, String middleName, String lastName,
                                 String mobile, String rawPassword,
                                 OrganizationRole role, Branch branch, String designation) {
        if (organizationUserRepository.findByEmail(email).isEmpty()) {
            organizationUserRepository.save(OrganizationUser.builder()
                    .empNo(empNo)
                    .username(username)
                    .loginBranch(branch)
                    .role(role)
                    .firstName(firstName)
                    .middleName(middleName)
                    .lastName(lastName)
                    .email(email)
                    .mobile(mobile)
                    .designation(designation)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .isActive(true)
                    .status(ApplicationConstants.UserStatus.OPERATIVE)
                    .twoFaEnabled(true)
                    .loginOnHolidays(false)
                    .multiBranchAccess(false)
                    .inactiveSessionTimeout(1800)
                    .noOfBadLogins(0)
                    .build());
        }
    }

    private void seedRolePermissions(String orgDb, OrganizationRole role, List<String> permCodes) {
        if (role == null || permCodes == null) return;
        DataSource ds = bankDataSourceProvider.getBankDataSource(orgDb);
        if (ds == null) return;

        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(true);
            for (String code : permCodes) {
                permissionRepository.findByCode(code).ifPresent(p -> {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO identity.role_permissions (role_id, permission_id) " +
                            "SELECT " + role.getId() + ", " + p.getId() + " WHERE NOT EXISTS (" +
                            "SELECT 1 FROM identity.role_permissions WHERE role_id = " + role.getId() + " AND permission_id = " + p.getId() + ")")) {
                        ps.executeUpdate();
                    } catch (Exception ex) {
                        log.debug("Notice seeding permission {} for role {}: {}", code, role.getName(), ex.getMessage());
                    }
                });
            }
        } catch (Exception e) {
            log.debug("Notice opening connection to seed permissions for role {}: {}", role.getName(), e.getMessage());
        }
    }

    // =========================================================================
    //  LOOKUP CATALOGUE SEEDING (Master & Bank DBs)
    // =========================================================================

    private void seedMasterLookups() {
        // 10001: Bank User Status (Fixed)
        seedMasterLookupType("10001", "Bank User Status", true);
        seedMasterLookupSubType("10001", "Bank User Status", "1", "Entered", true, 1);
        seedMasterLookupSubType("10001", "Bank User Status", "2", "Verified", true, 2);
        seedMasterLookupSubType("10001", "Bank User Status", "3", "In-Operative", true, 3);
        seedMasterLookupSubType("10001", "Bank User Status", "4", "Operative", true, 4);
        seedMasterLookupSubType("10001", "Bank User Status", "5", "Suspended", true, 5);

        // 10002: Designation (Not Fixed - Banks can add/delete designations)
        seedMasterLookupType("10002", "Designation", false);
        seedMasterLookupSubType("10002", "Designation", "1", "Clerk", false, 1);
        seedMasterLookupSubType("10002", "Designation", "2", "Officer", false, 2);
        seedMasterLookupSubType("10002", "Designation", "3", "Sr. Officer", false, 3);
        seedMasterLookupSubType("10002", "Designation", "4", "Asst. Manager", false, 4);
        seedMasterLookupSubType("10002", "Designation", "5", "Manager", false, 5);
        seedMasterLookupSubType("10002", "Designation", "6", "Dy. Manager", false, 6);
        seedMasterLookupSubType("10002", "Designation", "7", "Gen. Manager", false, 7);
        seedMasterLookupSubType("10002", "Designation", "8", "CEO", false, 8);
        seedMasterLookupSubType("10002", "Designation", "9", "Chairman", false, 9);

        // 10003: Role (Fixed)
        seedMasterLookupType("10003", "Role", true);
        seedMasterLookupSubType("10003", "Role", "1", "System Admin", true, 1);
        seedMasterLookupSubType("10003", "Role", "2", "Super Admin", true, 2);
        seedMasterLookupSubType("10003", "Role", "3", "Admin", true, 3);
        seedMasterLookupSubType("10003", "Role", "4", "Checker", true, 4);
        seedMasterLookupSubType("10003", "Role", "5", "Maker", true, 5);
        seedMasterLookupSubType("10003", "Role", "6", "Viewer", true, 6);

        // 10004: Loan Type (Not Fixed - Banks can add/delete loan types)
        seedMasterLookupType("10004", "Loan Type", false);
        seedMasterLookupSubType("10004", "Loan Type", "1", "Personal Loan", false, 1);
        seedMasterLookupSubType("10004", "Loan Type", "2", "Home Loan", false, 2);
        seedMasterLookupSubType("10004", "Loan Type", "3", "Vehicle Loan", false, 3);
        seedMasterLookupSubType("10004", "Loan Type", "4", "Gold Loan", false, 4);
        seedMasterLookupSubType("10004", "Loan Type", "5", "Business Loan", false, 5);
        seedMasterLookupSubType("10004", "Loan Type", "6", "Loan Against Property (LAP)", false, 6);
        seedMasterLookupSubType("10004", "Loan Type", "7", "Working Capital / Cash Credit", false, 7);
        seedMasterLookupSubType("10004", "Loan Type", "8", "Professional Loan", false, 8);
        seedMasterLookupSubType("10004", "Loan Type", "9", "Corporate / Commercial Loan", false, 9);
        seedMasterLookupSubType("10004", "Loan Type", "10", "Educational Loan", false, 10);

        // 10005: Customer Type (Fixed)
        seedMasterLookupType("10005", "Customer Type", true);
        seedMasterLookupSubType("10005", "Customer Type", "1", "10006 (Individual)", true, 1);
        seedMasterLookupSubType("10005", "Customer Type", "2", "10007 (Corporate)", true, 2);

        // 10006: Individual Customer Sub Type (Fixed)
        seedMasterLookupType("10006", "Individual Customer Sub Type", true);
        seedMasterLookupSubType("10006", "Individual Customer Sub Type", "1", "Self", true, 1);
        seedMasterLookupSubType("10006", "Individual Customer Sub Type", "2", "Joint", true, 2);

        // 10007: Corporate Customer Sub Type (Not Fixed)
        seedMasterLookupType("10007", "Corporate Customer Sub Type", false);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "1", "Proprietorship", false, 1);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "2", "Partnership", false, 2);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "3", "Private Limited", false, 3);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "4", "Public Limited", false, 4);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "5", "Trust", false, 5);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "6", "Association Chairman", false, 6);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "7", "Society", false, 7);
        seedMasterLookupSubType("10007", "Corporate Customer Sub Type", "8", "Federation", false, 8);

        // 10008: Lookup Permissions (Fixed - System Permissions Catalogue for Lookups)
        seedMasterLookupType("10008", "Lookup Permissions", true);
        seedMasterLookupSubType("10008", "Lookup Permissions", "101", "VIEW", true, 1);
        seedMasterLookupSubType("10008", "Lookup Permissions", "102", "EDIT", true, 2);
        seedMasterLookupSubType("10008", "Lookup Permissions", "103", "DELETE", true, 3);
        seedMasterLookupSubType("10008", "Lookup Permissions", "104", "ADD", true, 4);
        seedMasterLookupSubType("10008", "Lookup Permissions", "105", "ADD_FROM_MASTER", true, 5);
        seedMasterLookupSubType("10008", "Lookup Permissions", "106", "ACTIVATE", true, 6);
        seedMasterLookupSubType("10008", "Lookup Permissions", "107", "DEACTIVATE", true, 7);

        // 10009: Access Permissions (Fixed - System Access Permissions for Roles)
        seedMasterLookupType("10009", "Access Permissions", true);
        seedMasterLookupSubType("10009", "Access Permissions", "201", "USER_CREATE", true, 1);
        seedMasterLookupSubType("10009", "Access Permissions", "202", "USER_UPDATE", true, 2);
        seedMasterLookupSubType("10009", "Access Permissions", "203", "USER_VIEW", true, 3);
        seedMasterLookupSubType("10009", "Access Permissions", "204", "USER_DEACTIVATE", true, 4);
        seedMasterLookupSubType("10009", "Access Permissions", "205", "USER_VERIFY", true, 5);
        seedMasterLookupSubType("10009", "Access Permissions", "206", "USER_RESET_PASSWORD", true, 6);
        seedMasterLookupSubType("10009", "Access Permissions", "207", "BRANCH_CREATE", true, 7);
        seedMasterLookupSubType("10009", "Access Permissions", "208", "BRANCH_UPDATE", true, 8);
        seedMasterLookupSubType("10009", "Access Permissions", "209", "BRANCH_VIEW", true, 9);
        seedMasterLookupSubType("10009", "Access Permissions", "210", "REPORT_VIEW", true, 10);
        seedMasterLookupSubType("10009", "Access Permissions", "211", "REPORT_EXPORT", true, 11);
        seedMasterLookupSubType("10009", "Access Permissions", "212", "DASHBOARD_VIEW", true, 12);
        seedMasterLookupSubType("10009", "Access Permissions", "213", "DASHBOARD_ANALYTICS_VIEW", true, 13);
        seedMasterLookupSubType("10009", "Access Permissions", "214", "ROLE_PERMISSION_MANAGE", true, 14);
        seedMasterLookupSubType("10009", "Access Permissions", "215", "LOOKUP_BANK_VIEW", true, 15);
        seedMasterLookupSubType("10009", "Access Permissions", "216", "LOOKUP_BANK_ADD", true, 16);
        seedMasterLookupSubType("10009", "Access Permissions", "217", "LOOKUP_BANK_ADD_FROM_MASTER", true, 17);
        seedMasterLookupSubType("10009", "Access Permissions", "218", "LOOKUP_BANK_EDIT", true, 18);
        seedMasterLookupSubType("10009", "Access Permissions", "219", "LOOKUP_BANK_DELETE", true, 19);
        seedMasterLookupSubType("10009", "Access Permissions", "220", "LOOKUP_BANK_ACTIVATE", true, 20);
        seedMasterLookupSubType("10009", "Access Permissions", "221", "LOOKUP_BANK_DEACTIVATE", true, 21);

        // Seed default lookup permissions in master DB for all master lookup types
        List<MasterLookupType> masterTypes = masterLookupTypeRepository.findAll();
        for (MasterLookupType mt : masterTypes) {
            if (masterLookupTypePermissionRepository.findByLookupTypeCode(mt.getCode()).isEmpty()) {
                for (String perm : com.bank.los.bank.lookup.service.BankLookupService.DEFAULT_LOOKUP_PERMISSIONS) {
                    masterLookupTypePermissionRepository.save(com.bank.los.administration.master.entity.MasterLookupTypePermission.builder()
                            .lookupTypeCode(mt.getCode())
                            .permissionCode(perm)
                            .build());
                }
            }
        }
    }

    private void seedMasterLookupType(String code, String description, boolean isFixed) {
        if (masterLookupTypeRepository.findByCode(code).isEmpty()) {
            masterLookupTypeRepository.save(MasterLookupType.builder()
                    .code(code)
                    .description(description)
                    .isFixed(isFixed)
                    .isActive(true)
                    .build());
        }
    }

    private void seedMasterLookupSubType(String lookupTypeCode, String typeDescription,
                                         String subTypeCode, String subTypeDescription,
                                         boolean isFixed, int displayOrder) {
        if (masterLookupSubTypeRepository.findByLookupTypeCodeAndSubTypeCode(lookupTypeCode, subTypeCode).isEmpty()) {
            masterLookupSubTypeRepository.save(MasterLookupSubType.builder()
                    .lookupTypeCode(lookupTypeCode)
                    .typeDescription(typeDescription)
                    .subTypeCode(subTypeCode)
                    .subTypeDescription(subTypeDescription)
                    .isFixed(isFixed)
                    .isActive(true)
                    .displayOrder(displayOrder)
                    .build());
        }
    }

    private void seedBankLookups(String orgDbName) {
        try {
            // Read all from Master DB
            BankContext.setCurrentBank(BankContext.MASTER_DB_NAME);
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            List<MasterLookupType> masterTypes = masterLookupTypeRepository.findAll();
            List<MasterLookupSubType> masterSubTypes = masterLookupSubTypeRepository.findAll();

            // Switch to Tenant DB
            BankContext.setCurrentBank(orgDbName);
            OrganizationContext.setCurrentOrganization(orgDbName);

            for (MasterLookupType mt : masterTypes) {
                if (bankLookupTypeRepository.findByCode(mt.getCode()).isEmpty()) {
                    bankLookupTypeRepository.save(BankLookupType.builder()
                            .code(mt.getCode())
                            .description(mt.getDescription())
                            .isFixed(mt.getIsFixed())
                            .isActive(mt.getIsActive())
                            .build());
                }

                if (bankLookupTypePermissionRepository.findByLookupTypeCode(mt.getCode()).isEmpty()) {
                    for (String perm : com.bank.los.bank.lookup.service.BankLookupService.DEFAULT_LOOKUP_PERMISSIONS) {
                        bankLookupTypePermissionRepository.save(com.bank.los.bank.master.entity.BankLookupTypePermission.builder()
                                .lookupTypeCode(mt.getCode())
                                .permissionCode(perm)
                                .build());
                    }
                }
            }

            for (MasterLookupSubType mst : masterSubTypes) {
                if (bankLookupSubTypeRepository.findByLookupTypeCodeAndSubTypeCode(
                        mst.getLookupTypeCode(), mst.getSubTypeCode()).isEmpty()) {
                    bankLookupSubTypeRepository.save(BankLookupSubType.builder()
                            .lookupTypeCode(mst.getLookupTypeCode())
                            .typeDescription(mst.getTypeDescription())
                            .subTypeCode(mst.getSubTypeCode())
                            .subTypeDescription(mst.getSubTypeDescription())
                            .isFixed(mst.getIsFixed())
                            .isActive(mst.getIsActive())
                            .displayOrder(mst.getDisplayOrder())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("Notice seeding bank lookups for {}: {}", orgDbName, e.getMessage());
        }
    }
}

