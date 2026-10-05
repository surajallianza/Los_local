package com.example.demo.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

/**
 * Unified Request and Data Transfer Object (DTO) for Lead Management.
 * Encapsulates all fields across the 4-step Lead Management flow:
 * 1. Personal Details: Customer Name, DOB, Age, Customer Type, Mobile Number, OTP, PAN Card,
 *    PAN Validate, Aadhaar Card, Aadhaar Validate, Residential Status, Gender, Marital Status,
 *    Passport No., De-Duplicate Check, Blacklist Check (plus optional lastName, email, pinCode, dependents).
 * 2. Loan Details: Loan Product Type, Loan Amount, Purpose of Loan, Tenure, No. of Instalments,
 *    EMI, Property Value, Security Amount (plus optional downPaymentCollateral).
 * 3. Income Profile: Employment Type, Annual Income, Designation, Employer Name, Location, State,
 *    Take Home Pay, Deductions / EMIs Payable, Bank Name, Account Number, Account Statement Consent,
 *    CIBIL Liability Check, Debt-to-Income (DTI), Loan-to-Value (LTV), Debt Service Coverage Ratio (DSCR),
 *    Net Disposable Income (NDI).
 * 4. Referral Details: Lead Acquisition Channel, Date, Sourcing Agent / Partner ID, Agent / Partner Name,
 *    Employee ID, Employee Name.
 * System Fields: Lead ID, Lead Status, Assigned Employee, Assignment Timestamp, Assigned By.
 */
@MappedSuperclass
public class LeadRequest {

    // ==========================================
    // System & Identification Fields
    // ==========================================
    @Id
    @Column(name = "lead_id", nullable = false, unique = true)
    @JsonProperty("leadId")
    @JsonAlias({"lead_id", "Lead ID", "id"})
    private String leadId;

    @Column(name = "lead_status")
    @JsonProperty("leadStatus")
    @JsonAlias({"lead_status", "status", "Lead Status"})
    private String leadStatus = "NEW";

    @Column(name = "assigned_employee_id")
    @JsonProperty("assignedEmployeeId")
    @JsonAlias({"assigned_employee_id", "assignedMakerId", "Assigned Employee"})
    private String assignedEmployeeId;

    @Column(name = "assignment_timestamp")
    @JsonProperty("assignmentTimestamp")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonAlias({"assignment_timestamp", "assignmentDate", "Assignment Timestamp"})
    private LocalDateTime assignmentTimestamp;

    @Column(name = "assigned_by")
    @JsonProperty("assignedBy")
    @JsonAlias({"assigned_by", "assignedByEmployeeId", "assignedByAdmin", "Assigned By"})
    private String assignedBy;

    // ==========================================
    // 1. Personal Details
    // ==========================================
    @Column(name = "first_name_business_name")
    @JsonProperty("customerName")
    @JsonAlias({"firstNameBusinessName", "name", "firstName", "First Name / Business Name", "first_name_business_name", "firstNameOrBusinessName"})
    private String customerName;

    @Column(name = "dob")
    @JsonProperty("dateOfBirth")
    @JsonAlias({"dob", "date_of_birth", "DOB", "Date of Birth"})
    private String dateOfBirth;

    @Column(name = "age")
    @JsonProperty("age")
    @JsonAlias({"Age", "applicantAge"})
    private Integer age;

    @Column(name = "customer_type")
    @JsonProperty("customerType")
    @JsonAlias({"customer_type", "Customer Type", "userCategory", "user_category", "User Category"})
    private String customerType = "Individual";

    @Column(name = "mobile_number")
    @JsonProperty("mobileNumber")
    @JsonAlias({"mobile", "mobile_number", "Mobile Number", "phone"})
    private String mobileNumber;

    @Column(name = "otp")
    @JsonProperty("otp")
    @JsonAlias({"otpCode", "OTP", "otpNumber", "OTP Number"})
    private String otp;

    @Column(name = "pan_number")
    @JsonProperty("panNumber")
    @JsonAlias({"pan", "pan_number", "PAN", "panCard", "PAN Card"})
    private String panNumber;

    @Column(name = "pan_validation_status")
    @JsonProperty("panValidationStatus")
    @JsonAlias({"pan_validation_status", "panValidate", "PAN Validate", "panValidation", "panValidateStatus"})
    private String panValidationStatus;

    @Column(name = "aadhaar_number")
    @JsonProperty("aadhaarNumber")
    @JsonAlias({"aadhaar", "aadhaar_number", "Aadhaar", "aadhar", "aadharNumber", "Aadhaar Card", "aadhaarCard"})
    private String aadhaarNumber;

    @Column(name = "aadhaar_validation_status")
    @JsonProperty("aadhaarValidationStatus")
    @JsonAlias({"aadhaar_validation_status", "aadhaarValidate", "Aadhaar Validate", "aadhaarValidation", "aadhaarValidateStatus"})
    private String aadhaarValidationStatus;

    @Column(name = "residential_status")
    @JsonProperty("residentialStatus")
    @JsonAlias({"residential_status", "Residential Status"})
    private String residentialStatus;

    @Column(name = "gender")
    @JsonProperty("gender")
    @JsonAlias({"Gender"})
    private String gender;

    @Column(name = "marital_status")
    @JsonProperty("maritalStatus")
    @JsonAlias({"marital_status", "Marital Status"})
    private String maritalStatus;

    @Column(name = "passport_number")
    @JsonProperty("passportNumber")
    @JsonAlias({"passport_number", "passportNo", "Passport No.", "passport"})
    private String passportNumber;

    @Column(name = "dedupe_status")
    @JsonProperty("dedupeStatus")
    @JsonAlias({"dedupe_status", "deduplicateCheck", "De-Duplicate Check", "dedupe", "deduplicateStatus"})
    private String dedupeStatus;

    @Column(name = "blacklist_status")
    @JsonProperty("blacklistStatus")
    @JsonAlias({"blacklist_status", "blacklistCheck", "Blacklist Check", "blacklist"})
    private String blacklistStatus;

    @Column(name = "last_name")
    @JsonProperty("lastName")
    @JsonAlias({"lastName", "last_name", "Last Name"})
    private String lastName;

    @Column(name = "number_of_dependents")
    @JsonProperty("numberOfDependents")
    @JsonAlias({"noOfDependents", "no_of_dependents", "No. of Dependents", "dependents"})
    private Integer numberOfDependents;

    @Column(name = "email_address")
    @JsonProperty("emailAddress")
    @JsonAlias({"email", "email_address", "Email Address"})
    private String emailAddress;

    @Column(name = "pin_code")
    @JsonProperty("pinCode")
    @JsonAlias({"pin_code", "pincode", "Pin Code", "zipCode"})
    private String pinCode;

    // ==========================================
    // 2. Loan Details
    // ==========================================
    @Column(name = "loan_product_type")
    @JsonProperty("loanProductType")
    @JsonAlias({"loanProduct", "loan_product_type", "Loan Product Type", "loanType", "loan_type", "Loan Type"})
    private String loanProductType;

    @Column(name = "loan_amount")
    @JsonProperty("loanAmount")
    @JsonAlias({"amount", "loan_amount", "Loan Amount"})
    private Double loanAmount;

    @Column(name = "purpose_of_loan")
    @JsonProperty("purposeOfLoan")
    @JsonAlias({"loanPurpose", "loan_purpose", "purpose_of_loan", "Purpose of Loan", "purpose"})
    private String purposeOfLoan;

    @Column(name = "tenure")
    @JsonProperty("tenure")
    @JsonAlias({"tenureMonths", "Tenure"})
    private Integer tenure;

    @Column(name = "number_of_instalments")
    @JsonProperty("numberOfInstalments")
    @JsonAlias({"noOfInstalments", "instalments", "installments", "numberOfInstallments", "No. of Instalments"})
    private Integer numberOfInstalments;

    @Column(name = "emi")
    @JsonProperty("emi")
    @JsonAlias({"EMI", "monthlyEmi", "loanEmi"})
    private Double emi;

    @Column(name = "property_value")
    @JsonProperty("propertyValue")
    @JsonAlias({"property_value", "PropertyValue", "Property Value"})
    private Double propertyValue;

    @Column(name = "security_amount")
    @JsonProperty("securityAmount")
    @JsonAlias({"security_amount", "SecurityAmount", "Security Amount"})
    private Double securityAmount;

    @Column(name = "down_payment_collateral")
    @JsonProperty("downPaymentCollateral")
    @JsonAlias({"down_payment_collateral", "downPayment", "collateral", "Down Payment/Collateral", "downPaymentOrCollateral"})
    private String downPaymentCollateral;

    // ==========================================
    // 3. Income Profile
    // ==========================================
    @Column(name = "employment_type")
    @JsonProperty("employmentType")
    @JsonAlias({"employment_type", "Employment Type"})
    private String employmentType;

    @Column(name = "annual_income")
    @JsonProperty("annualIncome")
    @JsonAlias({"income", "annual_income", "Annual Income"})
    private Double annualIncome;

    @Column(name = "designation")
    @JsonProperty("designation")
    @JsonAlias({"Designation"})
    private String designation;

    @Column(name = "employer_business_name")
    @JsonProperty("employerName")
    @JsonAlias({"employer_business_name", "employerBusinessName", "employer_name", "Employer / Business Name", "Employer Name"})
    private String employerName;

    @Column(name = "location")
    @JsonProperty("location")
    @JsonAlias({"Location", "city", "City"})
    private String location;

    @Column(name = "state")
    @JsonProperty("state")
    @JsonAlias({"State"})
    private String state;

    @Column(name = "take_home_pay")
    @JsonProperty("takeHomePay")
    @JsonAlias({"take_home_pay", "takeHomeIncome", "Take Home Pay"})
    private Double takeHomePay;

    @Column(name = "deductions_or_emis_payable")
    @JsonProperty("deductionsOrEmisPayable")
    @JsonAlias({"deductions_or_emis_payable", "deductions", "emisPayable", "Deductions / EMIs Payable", "deductionsPayable"})
    private Double deductionsOrEmisPayable;

    @Column(name = "bank_name")
    @JsonProperty("bankName")
    @JsonAlias({"bank_name", "Bank Name"})
    private String bankName;

    @Column(name = "primary_bank_account")
    @JsonProperty("accountNumber")
    @JsonAlias({"primary_bank_account", "primaryBankAccount", "bankAccountNumber", "Primary Bank Account", "Account Number"})
    private String accountNumber;

    @Column(name = "account_statement_consent")
    @JsonProperty("accountStatementConsent")
    @JsonAlias({"account_statement_consent", "statementConsent", "Account Statement Consent"})
    private Boolean accountStatementConsent;

    @Column(name = "cibil_liability_check")
    @JsonProperty("cibilLiabilityCheck")
    @JsonAlias({"cibil_liability_check", "cibilScore", "cibilCheck", "CIBIL Liability Check", "cibilLiabilityCheckStatus"})
    private String cibilLiabilityCheck;

    @Column(name = "debt_to_income_ratio")
    @JsonProperty("debtToIncomeRatio")
    @JsonAlias({"debt_to_income_ratio", "dti", "Debt-to-Income (DTI)", "dtiRatio"})
    private Double debtToIncomeRatio;

    @Column(name = "loan_to_value_ratio")
    @JsonProperty("loanToValueRatio")
    @JsonAlias({"loan_to_value_ratio", "ltv", "Loan-to-Value (LTV)", "ltvRatio"})
    private Double loanToValueRatio;

    @Column(name = "debt_service_coverage_ratio")
    @JsonProperty("debtServiceCoverageRatio")
    @JsonAlias({"debt_service_coverage_ratio", "dscr", "Debt Service Coverage Ratio (DSCR)", "dscrRatio"})
    private Double debtServiceCoverageRatio;

    @Column(name = "net_disposable_income")
    @JsonProperty("netDisposableIncome")
    @JsonAlias({"net_disposable_income", "ndi", "Net Disposable Income (NDI)", "ndiIncome"})
    private Double netDisposableIncome;

    // ==========================================
    // 4. Referral Details
    // ==========================================
    @Column(name = "sourcing_channel")
    @JsonProperty("leadAcquisitionChannel")
    @JsonAlias({"sourcing_channel", "sourcingChannel", "lead_acquisition_channel", "Lead Acquisition Channel", "Sourcing Channel"})
    private String leadAcquisitionChannel;

    @Column(name = "referral_date")
    @JsonProperty("referralDate")
    @JsonAlias({"referral_date", "date", "Date", "Referral Date"})
    private String referralDate;

    @Column(name = "lsp_partner_code")
    @JsonProperty("sourcingAgentPartnerId")
    @JsonAlias({"lsp_partner_code", "lspPartnerCode", "sourcing_agent_partner_id", "LSP / Partner Code", "Sourcing Agent / Partner ID"})
    private String sourcingAgentPartnerId;

    @Column(name = "agent_partner_name")
    @JsonProperty("agentPartnerName")
    @JsonAlias({"agent_partner_name", "agentName", "partnerName", "Agent/Partner Name", "agentOrPartnerName", "Agent / Partner Name"})
    private String agentPartnerName;

    @Column(name = "sourcing_employee_id")
    @JsonProperty("sourcingEmployeeId")
    @JsonAlias({"sourcing_employee_id", "referralEmployeeId", "Employee ID", "employeeId"})
    private String sourcingEmployeeId;

    @Column(name = "sourcing_employee_name")
    @JsonProperty("sourcingEmployeeName")
    @JsonAlias({"sourcing_employee_name", "referralEmployeeName", "employeeName", "Employee Name"})
    private String sourcingEmployeeName;

    // ==========================================
    // Constructors
    // ==========================================
    public LeadRequest() {
    }

    // ==========================================
    // Getters and Setters: System Fields
    // ==========================================
    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getLeadStatus() {
        return leadStatus;
    }

    public void setLeadStatus(String leadStatus) {
        this.leadStatus = leadStatus;
    }

    public String getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    public void setAssignedEmployeeId(String assignedEmployeeId) {
        this.assignedEmployeeId = assignedEmployeeId;
    }

    public LocalDateTime getAssignmentTimestamp() {
        return assignmentTimestamp;
    }

    public void setAssignmentTimestamp(LocalDateTime assignmentTimestamp) {
        this.assignmentTimestamp = assignmentTimestamp;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    // ==========================================
    // Getters and Setters: 1. Personal Details
    // ==========================================
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    @JsonIgnore
    public String getFirstNameBusinessName() {
        return customerName;
    }

    @JsonIgnore
    public void setFirstNameBusinessName(String firstNameBusinessName) {
        this.customerName = firstNameBusinessName;
    }

    @JsonIgnore
    public String getName() {
        return customerName;
    }

    @JsonIgnore
    public void setName(String name) {
        this.customerName = name;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    @JsonIgnore
    public String getDob() {
        return dateOfBirth;
    }

    @JsonIgnore
    public void setDob(String dob) {
        this.dateOfBirth = dob;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getCustomerType() {
        return customerType;
    }

    public void setCustomerType(String customerType) {
        this.customerType = customerType;
    }

    @JsonIgnore
    public String getUserCategory() {
        return customerType;
    }

    @JsonIgnore
    public void setUserCategory(String userCategory) {
        this.customerType = userCategory;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    @JsonIgnore
    public String getMobile() {
        return mobileNumber;
    }

    @JsonIgnore
    public void setMobile(String mobile) {
        this.mobileNumber = mobile;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getPanNumber() {
        return panNumber;
    }

    public void setPanNumber(String panNumber) {
        this.panNumber = panNumber;
    }

    @JsonIgnore
    public String getPan() {
        return panNumber;
    }

    @JsonIgnore
    public void setPan(String pan) {
        this.panNumber = pan;
    }

    @JsonIgnore
    public String getPanCard() {
        return panNumber;
    }

    @JsonIgnore
    public void setPanCard(String panCard) {
        this.panNumber = panCard;
    }

    public String getPanValidationStatus() {
        return panValidationStatus;
    }

    public void setPanValidationStatus(String panValidationStatus) {
        this.panValidationStatus = panValidationStatus;
    }

    public String getAadhaarNumber() {
        return aadhaarNumber;
    }

    public void setAadhaarNumber(String aadhaarNumber) {
        this.aadhaarNumber = aadhaarNumber;
    }

    @JsonIgnore
    public String getAadhaar() {
        return aadhaarNumber;
    }

    @JsonIgnore
    public void setAadhaar(String aadhaar) {
        this.aadhaarNumber = aadhaar;
    }

    @JsonIgnore
    public String getAadhaarCard() {
        return aadhaarNumber;
    }

    @JsonIgnore
    public void setAadhaarCard(String aadhaarCard) {
        this.aadhaarNumber = aadhaarCard;
    }

    public String getAadhaarValidationStatus() {
        return aadhaarValidationStatus;
    }

    public void setAadhaarValidationStatus(String aadhaarValidationStatus) {
        this.aadhaarValidationStatus = aadhaarValidationStatus;
    }

    public String getResidentialStatus() {
        return residentialStatus;
    }

    public void setResidentialStatus(String residentialStatus) {
        this.residentialStatus = residentialStatus;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getMaritalStatus() {
        return maritalStatus;
    }

    public void setMaritalStatus(String maritalStatus) {
        this.maritalStatus = maritalStatus;
    }

    public String getPassportNumber() {
        return passportNumber;
    }

    public void setPassportNumber(String passportNumber) {
        this.passportNumber = passportNumber;
    }

    @JsonIgnore
    public String getPassportNo() {
        return passportNumber;
    }

    @JsonIgnore
    public void setPassportNo(String passportNo) {
        this.passportNumber = passportNo;
    }

    public String getDedupeStatus() {
        return dedupeStatus;
    }

    public void setDedupeStatus(String dedupeStatus) {
        this.dedupeStatus = dedupeStatus;
    }

    public String getBlacklistStatus() {
        return blacklistStatus;
    }

    public void setBlacklistStatus(String blacklistStatus) {
        this.blacklistStatus = blacklistStatus;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Integer getNumberOfDependents() {
        return numberOfDependents;
    }

    public void setNumberOfDependents(Integer numberOfDependents) {
        this.numberOfDependents = numberOfDependents;
    }

    @JsonIgnore
    public Integer getNoOfDependents() {
        return numberOfDependents;
    }

    @JsonIgnore
    public void setNoOfDependents(Integer noOfDependents) {
        this.numberOfDependents = noOfDependents;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    // ==========================================
    // Getters and Setters: 2. Loan Details
    // ==========================================
    public String getLoanProductType() {
        return loanProductType;
    }

    public void setLoanProductType(String loanProductType) {
        this.loanProductType = loanProductType;
    }

    @JsonIgnore
    public String getLoanType() {
        return loanProductType;
    }

    @JsonIgnore
    public void setLoanType(String loanType) {
        this.loanProductType = loanType;
    }

    public Double getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(Double loanAmount) {
        this.loanAmount = loanAmount;
    }

    public String getPurposeOfLoan() {
        return purposeOfLoan;
    }

    public void setPurposeOfLoan(String purposeOfLoan) {
        this.purposeOfLoan = purposeOfLoan;
    }

    @JsonIgnore
    public String getLoanPurpose() {
        return purposeOfLoan;
    }

    @JsonIgnore
    public void setLoanPurpose(String loanPurpose) {
        this.purposeOfLoan = loanPurpose;
    }

    public Integer getTenure() {
        return tenure;
    }

    public void setTenure(Integer tenure) {
        this.tenure = tenure;
    }

    public Integer getNumberOfInstalments() {
        return numberOfInstalments;
    }

    public void setNumberOfInstalments(Integer numberOfInstalments) {
        this.numberOfInstalments = numberOfInstalments;
    }

    @JsonIgnore
    public Integer getNoOfInstalments() {
        return numberOfInstalments;
    }

    @JsonIgnore
    public void setNoOfInstalments(Integer noOfInstalments) {
        this.numberOfInstalments = noOfInstalments;
    }

    public Double getEmi() {
        return emi;
    }

    public void setEmi(Double emi) {
        this.emi = emi;
    }

    public Double getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(Double propertyValue) {
        this.propertyValue = propertyValue;
    }

    public Double getSecurityAmount() {
        return securityAmount;
    }

    public void setSecurityAmount(Double securityAmount) {
        this.securityAmount = securityAmount;
    }

    public String getDownPaymentCollateral() {
        return downPaymentCollateral;
    }

    public void setDownPaymentCollateral(String downPaymentCollateral) {
        this.downPaymentCollateral = downPaymentCollateral;
    }

    // ==========================================
    // Getters and Setters: 3. Income Profile
    // ==========================================
    public String getEmploymentType() {
        return employmentType;
    }

    public void setEmploymentType(String employmentType) {
        this.employmentType = employmentType;
    }

    public Double getAnnualIncome() {
        return annualIncome;
    }

    public void setAnnualIncome(Double annualIncome) {
        this.annualIncome = annualIncome;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getEmployerName() {
        return employerName;
    }

    public void setEmployerName(String employerName) {
        this.employerName = employerName;
    }

    @JsonIgnore
    public String getEmployerBusinessName() {
        return employerName;
    }

    @JsonIgnore
    public void setEmployerBusinessName(String employerBusinessName) {
        this.employerName = employerBusinessName;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Double getTakeHomePay() {
        return takeHomePay;
    }

    public void setTakeHomePay(Double takeHomePay) {
        this.takeHomePay = takeHomePay;
    }

    public Double getDeductionsOrEmisPayable() {
        return deductionsOrEmisPayable;
    }

    public void setDeductionsOrEmisPayable(Double deductionsOrEmisPayable) {
        this.deductionsOrEmisPayable = deductionsOrEmisPayable;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    @JsonIgnore
    public String getPrimaryBankAccount() {
        return accountNumber;
    }

    @JsonIgnore
    public void setPrimaryBankAccount(String primaryBankAccount) {
        this.accountNumber = primaryBankAccount;
    }

    public Boolean getAccountStatementConsent() {
        return accountStatementConsent;
    }

    public void setAccountStatementConsent(Boolean accountStatementConsent) {
        this.accountStatementConsent = accountStatementConsent;
    }

    public String getCibilLiabilityCheck() {
        return cibilLiabilityCheck;
    }

    public void setCibilLiabilityCheck(String cibilLiabilityCheck) {
        this.cibilLiabilityCheck = cibilLiabilityCheck;
    }

    public Double getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(Double dti) {
        this.debtToIncomeRatio = dti;
    }

    public Double getLoanToValueRatio() {
        return loanToValueRatio;
    }

    public void setLoanToValueRatio(Double ltv) {
        this.loanToValueRatio = ltv;
    }

    public Double getDebtServiceCoverageRatio() {
        return debtServiceCoverageRatio;
    }

    public void setDebtServiceCoverageRatio(Double dscr) {
        this.debtServiceCoverageRatio = dscr;
    }

    public Double getNetDisposableIncome() {
        return netDisposableIncome;
    }

    public void setNetDisposableIncome(Double ndi) {
        this.netDisposableIncome = ndi;
    }

    // ==========================================
    // Getters and Setters: 4. Referral Details
    // ==========================================
    public String getLeadAcquisitionChannel() {
        return leadAcquisitionChannel;
    }

    public void setLeadAcquisitionChannel(String leadAcquisitionChannel) {
        this.leadAcquisitionChannel = leadAcquisitionChannel;
    }

    @JsonIgnore
    public String getSourcingChannel() {
        return leadAcquisitionChannel;
    }

    @JsonIgnore
    public void setSourcingChannel(String sourcingChannel) {
        this.leadAcquisitionChannel = sourcingChannel;
    }

    public String getReferralDate() {
        return referralDate;
    }

    public void setReferralDate(String referralDate) {
        this.referralDate = referralDate;
    }

    @JsonIgnore
    public String getDate() {
        return referralDate;
    }

    @JsonIgnore
    public void setDate(String date) {
        this.referralDate = date;
    }

    public String getSourcingAgentPartnerId() {
        return sourcingAgentPartnerId;
    }

    public void setSourcingAgentPartnerId(String sourcingAgentPartnerId) {
        this.sourcingAgentPartnerId = sourcingAgentPartnerId;
    }

    @JsonIgnore
    public String getLspPartnerCode() {
        return sourcingAgentPartnerId;
    }

    @JsonIgnore
    public void setLspPartnerCode(String lspPartnerCode) {
        this.sourcingAgentPartnerId = lspPartnerCode;
    }

    public String getAgentPartnerName() {
        return agentPartnerName;
    }

    public void setAgentPartnerName(String agentPartnerName) {
        this.agentPartnerName = agentPartnerName;
    }

    public String getSourcingEmployeeId() {
        return sourcingEmployeeId;
    }

    public void setSourcingEmployeeId(String sourcingEmployeeId) {
        this.sourcingEmployeeId = sourcingEmployeeId;
    }

    @JsonIgnore
    public String getEmployeeId() {
        return sourcingEmployeeId;
    }

    @JsonIgnore
    public void setEmployeeId(String employeeId) {
        this.sourcingEmployeeId = employeeId;
    }

    public String getSourcingEmployeeName() {
        return sourcingEmployeeName;
    }

    public void setSourcingEmployeeName(String sourcingEmployeeName) {
        this.sourcingEmployeeName = sourcingEmployeeName;
    }

    @JsonIgnore
    public String getEmployeeName() {
        return sourcingEmployeeName;
    }

    @JsonIgnore
    public void setEmployeeName(String employeeName) {
        this.sourcingEmployeeName = employeeName;
    }
}
