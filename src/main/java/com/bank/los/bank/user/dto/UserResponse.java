package com.bank.los.bank.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Response representing a bank staff user")
public class UserResponse {

    @JsonProperty("pkid")
    @Schema(description = "User primary key ID", example = "1")
    private Long pkid;

    @JsonProperty("id")
    @Schema(description = "User primary key ID alias", example = "1")
    private Long id;

    @JsonProperty("emp_no")
    @Schema(description = "Employee number", example = "EMP-HDFC-001")
    private String empNo;

    @JsonProperty("organization_id")
    @Schema(description = "Bank / Organisation ID", example = "1")
    private Long organizationId;

    @JsonProperty("organization_code")
    @Schema(description = "Bank / Organisation Code", example = "HDFC01")
    private String organizationCode;

    @JsonProperty("organization_name")
    @Schema(description = "Bank / Organisation Name", example = "HDFC Bank")
    private String organizationName;

    @JsonProperty("email")
    @Schema(description = "Corporate email", example = "vikram.sharma@hdfcbank.com")
    private String email;

    @JsonProperty("username")
    @Schema(description = "Login username", example = "vikram_admin")
    private String username;

    @JsonProperty("name")
    @Schema(description = "Full name", example = "Vikram Sharma")
    private String name;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("middle_name")
    private String middleName;

    @JsonProperty("last_name")
    private String lastName;

    @JsonProperty("dob")
    @Schema(description = "Date of birth")
    private LocalDate dob;

    @JsonProperty("mobile")
    @Schema(description = "Mobile number", example = "+919876543210")
    private String mobile;

    @JsonProperty("gender")
    @Schema(description = "Gender", example = "MALE")
    private String gender;

    @JsonProperty("designation")
    @Schema(description = "Designation", example = "Branch Manager")
    private String designation;

    @JsonProperty("role")
    @Schema(description = "Role name", example = "ADMIN")
    private String role;

    @JsonProperty("role_id")
    @Schema(description = "Role ID", example = "1")
    private Integer roleId;

    @JsonProperty("role_name")
    @Schema(description = "Role name alias", example = "ADMIN")
    private String roleName;

    @JsonProperty("2fA")
    @Schema(description = "Two-factor authentication enabled flag", example = "true")
    private Boolean twoFa;

    @JsonProperty("two_fa_enabled")
    private Boolean twoFaEnabled;

    @JsonProperty("status")
    @Schema(description = "User status", example = "OPERATIVE")
    private String status;

    @JsonProperty("is_active")
    @Schema(description = "Active status flag", example = "true")
    private Boolean isActive;

    @JsonProperty("m_br_access")
    @Schema(description = "Multi-branch access flag", example = "false")
    private Boolean mBrAccess;

    @JsonProperty("multi_branch_access")
    private Boolean multiBranchAccess;

    @JsonProperty("login_branch")
    @Schema(description = "Primary login branch name / info", example = "Main Branch")
    private String loginBranch;

    @JsonProperty("login_branch_id")
    private Long loginBranchId;

    @JsonProperty("login_branch_name")
    private String loginBranchName;

    @JsonProperty("holiday_login")
    @Schema(description = "Holiday login permitted flag", example = "false")
    private Boolean holidayLogin;

    @JsonProperty("login_on_holidays")
    private Boolean loginOnHolidays;

    @JsonProperty("login_time")
    private LocalTime loginTime;

    @JsonProperty("logout_time")
    private LocalTime logoutTime;

    @JsonProperty("inactive_session_timeout")
    private Integer inactiveSessionTimeout;

    @JsonProperty("no_of_bad_logins")
    private Integer noOfBadLogins;

    @JsonProperty("lastlogindate")
    @Schema(description = "Last login date")
    private LocalDate lastlogindate;

    @JsonProperty("last_login_date")
    private LocalDate lastLoginDate;

    @JsonProperty("last_login_time")
    private LocalTime lastLoginTime;

    @JsonProperty("created_by")
    private Long createdBy;

    @JsonProperty("created_date")
    private LocalDateTime createdDate;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("verified_by")
    private Long verifiedBy;

    @JsonProperty("verified_date")
    private LocalDateTime verifiedDate;

    @JsonProperty("modified_by")
    private Long modifiedBy;

    @JsonProperty("modified_date")
    private LocalDateTime modifiedDate;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    public String getFullName() {
        return name;
    }

    @JsonProperty("employee_id")
    public String getEmployeeId() {
        return empNo;
    }

    @JsonProperty("date_of_birth")
    public LocalDate getDateOfBirth() {
        return dob;
    }

    @JsonProperty("allow_multibranch")
    public Boolean getAllowMultibranch() {
        return multiBranchAccess;
    }

    @JsonProperty("allow_login_in_holidays")
    public Boolean getAllowLoginInHolidays() {
        return loginOnHolidays;
    }
}

