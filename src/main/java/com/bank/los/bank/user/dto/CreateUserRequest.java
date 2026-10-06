package com.bank.los.bank.user.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body to onboard / create a new bank staff user in the organization's database")
public class CreateUserRequest {

    @JsonProperty("pkid")
    @JsonAlias({"id", "Pkid", "userId"})
    @Schema(description = "User primary key ID (optional)", example = "1")
    private Long id;

    @JsonProperty("organization_id")
    @JsonAlias({"organizationId", "bankId", "bank_id", "Organisation_id", "organisationId", "orgId"})
    @Schema(description = "Target Bank / Organisation ID (numeric PK) to map the user into", example = "17")
    private Long organizationId;

    @JsonProperty("organization_uuid")
    @JsonAlias({"orgUuid", "organizationUuid", "org_uuid"})
    @Schema(description = "Target Bank / Organisation UUID", example = "427775a5-81d8-402d-84a4-fd2cac160566")
    private java.util.UUID organizationUuid;

    @JsonProperty("organization_code")
    @JsonAlias({"organizationCode", "bankCode", "bank_code", "orgCode"})
    @Schema(description = "Target Organisation Code (alternative to organizationId)", example = "SBI01")
    private String organizationCode;

    @JsonProperty("emp_no")
    @JsonAlias({"empNo", "EmpNo", "employeeNumber", "employee_no", "employee_id", "employeeId", "emp_id", "empId"})
    @Schema(description = "Manual Employee ID / Number (e.g. EMP-SBI-001)", example = "EMP-SBI-001")
    private String empNo;

    @NotBlank(message = "first_name is required")
    @JsonProperty("first_name")
    @JsonAlias({"firstName", "FirstName"})
    @Schema(description = "First name of user", example = "Pranav")
    private String firstName;

    @JsonProperty("middle_name")
    @JsonAlias({"middleName", "MiddleName"})
    @Schema(description = "Middle name of user (optional)", example = "Kumar")
    private String middleName;

    @NotBlank(message = "last_name is required")
    @JsonProperty("last_name")
    @JsonAlias({"lastName", "LastName"})
    @Schema(description = "Last name of user", example = "Sharma")
    private String lastName;

    @JsonProperty("name")
    @JsonAlias({"Name", "fullName", "full_name"})
    @Schema(description = "Full name of the user (optional, auto-assembled from first/middle/last name if omitted)", example = "Pranav Kumar Sharma")
    private String name;

    @JsonProperty("username")
    @JsonAlias({"userName", "UserName"})
    @Schema(description = "Unique login username (defaults to email if omitted)", example = "pranav_sbi_checker")
    private String username;

    @NotBlank(message = "Password is required")
    @JsonProperty("password")
    @JsonAlias({"Password", "pwd"})
    @Schema(description = "Initial user password (min 8 chars, 1 upper, 1 lower, 1 digit, 1 special char)", example = "Password@123")
    private String password;

    @NotBlank(message = "Email is required")
    @Email(message = "Valid email is required")
    @JsonProperty("email")
    @JsonAlias({"Email", "mail", "Mail"})
    @Schema(description = "User corporate email address", example = "pranv@sbi.co.in")
    private String email;

    @JsonProperty("mobile")
    @JsonAlias({"Mobile", "phone", "Phone", "contactNumber"})
    @Schema(description = "Mobile number", example = "+919876541010")
    private String mobile;

    @JsonProperty("gender")
    @JsonAlias({"Gender"})
    @Schema(description = "Gender (MALE, FEMALE, OTHER)", example = "MALE")
    private String gender;

    @JsonProperty("dob")
    @JsonAlias({"DOB", "Dob", "dateOfBirth", "birthDate", "date_of_birth", "birth_date"})
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Date of Birth (YYYY-MM-DD)", example = "1992-08-25")
    private LocalDate dob;

    @JsonProperty("designation")
    @JsonAlias({"Designation"})
    @Schema(description = "Official job designation", example = "Bank Administrator")
    private String designation;

    @JsonProperty("role")
    @JsonAlias({"Role", "roleName", "role_name"})
    @Schema(description = "Role name in organization (ADMIN, MAKER, CHECKER, VIEWER)", example = "ADMIN")
    private String roleName;

    @JsonProperty("role_id")
    @JsonAlias({"roleId", "RoleId"})
    @Schema(description = "Role ID in this bank's database schema", example = "1")
    private Integer roleId;

    @JsonProperty("2fA")
    @JsonAlias({"twoFa", "two_fa", "twoFaEnabled", "two_fa_enabled", "2FA"})
    @Schema(description = "Two-factor authentication enabled flag", example = "true", defaultValue = "true")
    @Builder.Default
    private Boolean twoFaEnabled = true;

    @JsonProperty("status")
    @JsonAlias({"Status"})
    @Schema(description = "Initial user status (OPERATIVE, PENDING_VERIFICATION, ACTIVE)", example = "OPERATIVE", defaultValue = "OPERATIVE")
    private String status;

    @JsonProperty("m_br_access")
    @JsonAlias({"mBrAccess", "M_Br_access", "multiBranchAccess", "multi_branch_access", "allow_multibranch", "allow_multi_branch", "allowMultibranch", "allowMultiBranch"})
    @Schema(description = "Allow multi-branch access (1/0 or true/false)", example = "1", defaultValue = "0")
    @Builder.Default
    private Boolean multiBranchAccess = false;

    @JsonProperty("login_branch")
    @JsonAlias({"loginBranch", "Login_Branch", "loginBranchName", "login_branch_name", "branch_name", "branchName"})
    @Schema(description = "Primary login branch ID or Name", example = "1")
    private String loginBranch;

    @JsonProperty("login_branch_id")
    @JsonAlias({"loginBranchId", "branchId", "branch_id"})
    @Schema(description = "Primary login branch ID", example = "1")
    private Long loginBranchId;

    @JsonProperty("holiday_login")
    @JsonAlias({"holidayLogin", "Holiday_Login", "loginOnHolidays", "login_on_holidays", "allow_login_in_holidays", "allow_login_on_holidays", "allowLoginInHolidays", "allowLoginOnHolidays"})
    @Schema(description = "Allow login on weekends/bank holidays (1/0 or true/false)", example = "false", defaultValue = "false")
    @Builder.Default
    private Boolean loginOnHolidays = false;

    @JsonProperty("login_time")
    @JsonAlias({"loginTime", "Login_Time"})
    @JsonFormat(pattern = "HH:mm[:ss]")
    @Schema(description = "Allowed login window start time (HH:mm:ss)", example = "09:00:00")
    private LocalTime loginTime;

    @JsonProperty("logout_time")
    @JsonAlias({"logoutTime", "Logout_Time"})
    @JsonFormat(pattern = "HH:mm[:ss]")
    @Schema(description = "Allowed logout window end time (HH:mm:ss)", example = "19:00:00")
    private LocalTime logoutTime;

    @JsonProperty("no_of_bad_logins")
    @JsonAlias({"noOfBadLogins", "badLogins", "numberOfBadLogins", "bad_logins", "bad_login_count"})
    @Schema(description = "Number of bad logins (default 0)", example = "0", defaultValue = "0")
    @Builder.Default
    private Integer noOfBadLogins = 0;

    @JsonProperty("inactive_session_timeout")
    @JsonAlias({"inactiveSessionTimeout", "Inactive_session_timeout"})
    @Schema(description = "Session inactivity timeout in seconds", example = "1800", defaultValue = "1800")
    @Builder.Default
    private Integer inactiveSessionTimeout = 1800;

    @JsonProperty("lastlogindate")
    @JsonAlias({"lastLoginDate", "last_login_date", "LastLoginDate"})
    @Schema(description = "Last login date")
    private LocalDate lastLoginDate;

    @JsonProperty("created_by")
    @JsonAlias({"createdBy", "CreatedBy"})
    private Long createdBy;

    @JsonProperty("created_date")
    @JsonAlias({"createdDate", "CreatedDate", "createdAt", "created_at"})
    private LocalDateTime createdDate;

    @JsonProperty("verified_by")
    @JsonAlias({"verifiedBy", "VerifiedBy"})
    private Long verifiedBy;

    @JsonProperty("verified_date")
    @JsonAlias({"verifiedDate", "VerifiedDate"})
    private LocalDateTime verifiedDate;

    @JsonProperty("modified_by")
    @JsonAlias({"modifiedBy", "ModifiedBy"})
    private Long modifiedBy;

    @JsonProperty("modified_date")
    @JsonAlias({"modifiedDate", "ModifiedDate", "updatedAt", "updated_at"})
    private LocalDateTime modifiedDate;

    // Custom setters for 0/1 coercion
    @JsonSetter("m_br_access")
    public void setMBrAccess(Object val) {
        this.multiBranchAccess = parseBooleanFlag(val);
    }

    @JsonSetter("allow_multibranch")
    public void setAllowMultiBranch(Object val) {
        this.multiBranchAccess = parseBooleanFlag(val);
    }

    @JsonSetter("multi_branch_access")
    public void setMultiBranchAccessProp(Object val) {
        this.multiBranchAccess = parseBooleanFlag(val);
    }

    @JsonSetter("holiday_login")
    public void setHolidayLoginProp(Object val) {
        this.loginOnHolidays = parseBooleanFlag(val);
    }

    @JsonSetter("allow_login_in_holidays")
    public void setAllowLoginInHolidays(Object val) {
        this.loginOnHolidays = parseBooleanFlag(val);
    }

    @JsonSetter("login_on_holidays")
    public void setLoginOnHolidaysProp(Object val) {
        this.loginOnHolidays = parseBooleanFlag(val);
    }

    @JsonSetter("login_branch")
    public void setLoginBranchProp(Object val) {
        if (val != null) {
            String str = val.toString().trim();
            this.loginBranch = str;
            try {
                this.loginBranchId = Long.parseLong(str);
            } catch (NumberFormatException ignored) {}
        }
    }

    private static Boolean parseBooleanFlag(Object val) {
        if (val instanceof Boolean b) {
            return b;
        } else if (val instanceof Number n) {
            return n.intValue() == 1;
        } else if (val instanceof String s) {
            String trimmed = s.trim();
            return "1".equals(trimmed) || "true".equalsIgnoreCase(trimmed);
        }
        return false;
    }

    public String getFullName() {
        if (firstName != null && !firstName.isBlank()) {
            StringBuilder sb = new StringBuilder(firstName.trim());
            if (middleName != null && !middleName.isBlank()) {
                sb.append(" ").append(middleName.trim());
            }
            if (lastName != null && !lastName.isBlank()) {
                sb.append(" ").append(lastName.trim());
            }
            return sb.toString();
        }
        return name;
    }

    public String getEmployeeId() {
        return empNo;
    }

    public void setEmployeeId(String employeeId) {
        this.empNo = employeeId;
    }

    public LocalDate getDateOfBirth() {
        return dob;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dob = dateOfBirth;
    }
}
