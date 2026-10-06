package com.bank.los.bank.lead.service;

import com.bank.los.bank.lead.exception.ResourceNotFoundException;
import com.bank.los.bank.lead.exception.ValidationException;
import com.bank.los.bank.master.entity.OrganizationRole;
import com.bank.los.bank.master.entity.OrganizationUser;
import com.bank.los.bank.master.repository.OrganizationRoleRepository;
import com.bank.los.bank.master.repository.OrganizationUserRepository;
import com.bank.los.common.constant.ApplicationConstants;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing Bank User lookups and role/status validations for Lead assignments.
 * Replaces the source module's EmployeeService by connecting Lead operations directly
 * to the bank's existing OrganizationUser (identity.users) entity and repository.
 */
@Slf4j
@Service
public class LeadUserService {

    private final OrganizationUserRepository organizationUserRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public LeadUserService(OrganizationUserRepository organizationUserRepository,
                           OrganizationRoleRepository organizationRoleRepository,
                           PasswordEncoder passwordEncoder) {
        this.organizationUserRepository = organizationUserRepository;
        this.organizationRoleRepository = organizationRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Seeds initial standard test bank users on startup if they don't already exist.
     * Maps the standard demo employees (EMP101-EMP105) directly into the identity.users table.
     */
    @PostConstruct
    @Transactional
    public void initStandardLeadBankUsers() {
        try {
            ensureUser("EMP101", "admin_lead", "Admin User", "ADMIN", ApplicationConstants.UserStatus.OPERATIVE, true);
            ensureUser("EMP102", "maker_lead", "Rahul Verma", "MAKER", ApplicationConstants.UserStatus.OPERATIVE, true);
            ensureUser("EMP103", "sita_lead", "Sita Sharma", "MAKER", "INACTIVE", false);
            ensureUser("EMP104", "checker_lead", "Vikram Malhotra", "CHECKER", ApplicationConstants.UserStatus.OPERATIVE, true);
            ensureUser("EMP105", "pooja_lead", "Pooja Mehta", "MAKER", ApplicationConstants.UserStatus.OPERATIVE, true);
        } catch (Exception e) {
            log.debug("Notice during initStandardLeadBankUsers: {}", e.getMessage());
        }
    }

    private void ensureUser(String empNo, String username, String fullName, String roleName, String status, boolean isActive) {
        if (organizationUserRepository.findByEmpNo(empNo).isEmpty()) {
            OrganizationRole role = organizationRoleRepository.findByName(roleName)
                    .orElseGet(() -> organizationRoleRepository.save(OrganizationRole.builder()
                            .name(roleName)
                            .panel(ApplicationConstants.Panels.BANK_NBFC)
                            .description(roleName + " role")
                            .build()));

            String[] names = fullName.split(" ", 2);
            String firstName = names[0];
            String lastName = names.length > 1 ? names[1] : "Staff";

            organizationUserRepository.save(OrganizationUser.builder()
                    .empNo(empNo)
                    .username(username)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(username + "@bank.com")
                    .mobile("98000" + empNo.replaceAll("\\D", ""))
                    .role(role)
                    .status(status)
                    .isActive(isActive)
                    .passwordHash(passwordEncoder.encode("Password@123"))
                    .build());
        }
    }

    /**
     * Finds an organization bank user by EmpNo, Username, or Numeric ID.
     */
    public Optional<OrganizationUser> findUserByIdentifier(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return Optional.empty();
        }
        String trimmed = identifier.trim();
        Optional<OrganizationUser> user = organizationUserRepository.findByEmpNo(trimmed);
        if (user.isPresent()) {
            return user;
        }
        user = organizationUserRepository.findByUsername(trimmed);
        if (user.isPresent()) {
            return user;
        }
        try {
            return organizationUserRepository.findById(Long.valueOf(trimmed));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    public List<OrganizationUser> getAllUsers() {
        return organizationUserRepository.findAll();
    }

    /**
     * Validates that employee/user exists (404), is ACTIVE/OPERATIVE (400), and has MAKER role (400).
     *
     * @param userIdentifier Employee/User identifier (e.g. EMP102, EMP-HDFC-002, maker01)
     * @return validated active Maker OrganizationUser
     */
    public OrganizationUser validateAndGetMakerUser(String userIdentifier) {
        if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
            throw new ValidationException("Employee ID is required");
        }

        String trimmedId = userIdentifier.trim();
        OrganizationUser user = findUserByIdentifier(trimmedId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID '" + trimmedId + "' not found"));

        boolean isUserActive = Boolean.TRUE.equals(user.getIsActive())
                && user.getStatus() != null
                && ("ACTIVE".equalsIgnoreCase(user.getStatus()) || "OPERATIVE".equalsIgnoreCase(user.getStatus()));

        if (!isUserActive) {
            throw new ValidationException("Employee '" + trimmedId + "' is inactive and cannot be assigned leads");
        }

        String role = (user.getRole() != null && user.getRole().getName() != null) ? user.getRole().getName() : "";
        if (!"MAKER".equalsIgnoreCase(role)) {
            throw new ValidationException("Employee '" + trimmedId + "' does not have MAKER role (Current role: " + role + ")");
        }

        return user;
    }

    /**
     * Validates that assigning user exists (404), is ACTIVE/OPERATIVE (400), and has ADMIN or SUPER_ADMIN role (400).
     *
     * @param adminIdentifier Admin identifier (e.g. EMP101, EMP-HDFC-001, admin)
     * @return validated active Admin OrganizationUser
     */
    public OrganizationUser validateAndGetAdminUser(String adminIdentifier) {
        if (adminIdentifier == null || adminIdentifier.trim().isEmpty()) {
            throw new ValidationException("Assigning Admin Employee ID is required");
        }

        String trimmedId = adminIdentifier.trim();
        OrganizationUser user = findUserByIdentifier(trimmedId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigning Admin employee with ID '" + trimmedId + "' not found"));

        boolean isUserActive = Boolean.TRUE.equals(user.getIsActive())
                && user.getStatus() != null
                && ("ACTIVE".equalsIgnoreCase(user.getStatus()) || "OPERATIVE".equalsIgnoreCase(user.getStatus()));

        if (!isUserActive) {
            throw new ValidationException("Assigning Admin employee '" + trimmedId + "' is inactive");
        }

        String role = (user.getRole() != null && user.getRole().getName() != null) ? user.getRole().getName() : "";
        if (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role)) {
            throw new ValidationException("Only ADMIN users can assign leads to Makers (Employee '" + trimmedId + "' has role: " + role + ")");
        }

        return user;
    }
}
