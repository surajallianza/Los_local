package com.bank.los.bank.lead.service;

import com.bank.los.bank.lead.dto.LeadAssignRequest;
import com.bank.los.bank.lead.dto.LeadAssignResponse;
import com.bank.los.bank.lead.dto.LeadRequest;
import com.bank.los.bank.lead.entity.Lead;
import com.bank.los.bank.lead.exception.ResourceNotFoundException;
import com.bank.los.bank.lead.exception.ValidationException;
import com.bank.los.bank.lead.model.CsvImportResult;
import com.bank.los.bank.lead.repository.LeadRepository;
import com.bank.los.bank.master.entity.OrganizationUser;
import com.bank.los.config.BankContext;
import com.bank.los.config.OrganizationContext;
import com.bank.los.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for managing Lead operations with database persistence
 * and strict business rule validation across the 4-step origination flow:
 * Personal Details -> Loan Details -> Income Profile -> Referral Details.
 */
@Slf4j
@Service
public class LeadService {

    private final LeadRepository leadRepository;
    private final LeadValidator leadValidator;
    private final LeadUserService leadUserService;

    public LeadService(LeadRepository leadRepository, LeadValidator leadValidator) {
        this(leadRepository, leadValidator, null);
    }

    @Autowired
    public LeadService(LeadRepository leadRepository, LeadValidator leadValidator, LeadUserService leadUserService) {
        this.leadRepository = leadRepository;
        this.leadValidator = leadValidator;
        this.leadUserService = leadUserService;
    }

    private void ensureTenantContext() {
        if (OrganizationContext.getCurrentOrganization() == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
                if (principal.getOrganizationDbName() != null) {
                    OrganizationContext.setCurrentOrganization(principal.getOrganizationDbName());
                    BankContext.setCurrentBank(principal.getOrganizationDbName());
                }
            }
        }
    }

    /**
     * Generates the next sequential Lead ID in format LDYYYYMM#### (e.g., LD2026100001).
     * Synchronized to guarantee safe sequential numbering during concurrent lead submissions.
     */
    public synchronized String generateNextLeadId() {
        ensureTenantContext();
        String yearMonth = DateTimeFormatter.ofPattern("yyyyMM").format(LocalDate.now());
        String prefix = "LD" + yearMonth;

        List<Lead> leadsWithPrefix = leadRepository.findByLeadIdStartingWithOrderByLeadIdDesc(prefix);
        int maxSeq = 0;

        for (Lead l : leadsWithPrefix) {
            String existingId = l.getLeadId();
            if (existingId != null && existingId.startsWith(prefix) && existingId.length() > prefix.length()) {
                String suffix = existingId.substring(prefix.length());
                try {
                    int seq = Integer.parseInt(suffix);
                    if (seq > maxSeq) {
                        maxSeq = seq;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore non-numeric suffix
                }
            }
        }

        int nextSeq = maxSeq + 1;
        return String.format("%s%04d", prefix, nextSeq);
    }

    /**
     * Creates, validates, duplicate-checks, and saves a new Lead into the database.
     * Auto-generates Lead ID in format LDYYYYMM#### if not provided or empty.
     * Validates format and uniqueness if client supplies a Lead ID.
     * Automatically sets leadStatus = NEW on creation.
     * Enforces duplicate checking on Mobile Number, PAN Number, and Aadhaar Number.
     *
     * @param request LeadRequest DTO containing 4-step form data
     * @return persisted Lead entity
     */
    @Transactional
    public Lead createLead(LeadRequest request) {
        ensureTenantContext();
        if (request == null) {
            throw new ValidationException("Lead payload cannot be null");
        }

        Lead lead = (request instanceof Lead) ? (Lead) request : new Lead(request);

        // 1. Handle Lead ID generation or duplicate check
        if (lead.getLeadId() == null || lead.getLeadId().trim().isEmpty()) {
            lead.setLeadId(generateNextLeadId());
        } else {
            String trimmedId = lead.getLeadId().trim();
            lead.setLeadId(trimmedId);
            if (leadRepository.existsById(trimmedId)) {
                throw new ValidationException("Lead with ID '" + trimmedId + "' already exists");
            }
        }

        // 2. Automatically set initial status to NEW and clear assignment fields
        lead.setLeadStatus("NEW");
        lead.setAssignedEmployeeId(null);
        lead.setAssignmentTimestamp(null);
        lead.setAssignedBy(null);

        // 3. Validate and normalize all fields
        leadValidator.validateAndNormalize(lead);

        // 4. Duplicate checks
        // Duplicate Mobile Number Check
        if (lead.getMobileNumber() != null && !lead.getMobileNumber().trim().isEmpty()) {
            String mobile = lead.getMobileNumber().trim();
            if (leadRepository.existsByMobileNumber(mobile)) {
                throw new ValidationException("Duplicate lead detected: A lead with Mobile Number '" + mobile + "' already exists");
            }
        }

        // Duplicate PAN Check
        if (lead.getPanNumber() != null && !lead.getPanNumber().trim().isEmpty()) {
            String pan = lead.getPanNumber().trim().toUpperCase();
            if (leadRepository.existsByPanNumber(pan)) {
                throw new ValidationException("Duplicate lead detected: A lead with PAN '" + pan + "' already exists");
            }
        }

        // Duplicate Aadhaar Check
        if (lead.getAadhaarNumber() != null && !lead.getAadhaarNumber().trim().isEmpty()) {
            String aadhaar = lead.getAadhaarNumber().trim();
            if (leadRepository.existsByAadhaarNumber(aadhaar)) {
                throw new ValidationException("Duplicate lead detected: A lead with Aadhaar Number '" + aadhaar + "' already exists");
            }
        }

        // 5. Save into database
        Lead saved = leadRepository.save(lead);
        log.info("Lead created successfully: leadId={}, status={}", saved.getLeadId(), saved.getLeadStatus());
        return saved;
    }

    /**
     * Overload for direct Lead entity calls.
     */
    @Transactional
    public Lead createLead(Lead lead) {
        return createLead((LeadRequest) lead);
    }

    /**
     * Imports multiple leads in bulk from an uploaded CSV input stream.
     * Validates each row against business rules, auto-generates IDs if omitted,
     * persists valid leads into the database, and reports row-level errors for invalid rows.
     */
    @Transactional
    public CsvImportResult importLeadsFromCsv(InputStream inputStream) {
        ensureTenantContext();
        if (inputStream == null) {
            throw new ValidationException("CSV input stream cannot be null");
        }

        List<List<String>> records;
        try {
            records = CsvHelper.parseCsv(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ValidationException("Failed to read CSV input: " + e.getMessage());
        }

        if (records.isEmpty()) {
            throw new ValidationException("Uploaded CSV file is empty");
        }

        List<String> headerRow = records.get(0);
        CsvHelper.CsvColumnIndices indices = CsvHelper.resolveIndices(headerRow);
        if (indices.isMissingRequiredHeaders()) {
            throw new ValidationException("CSV is missing mandatory header columns: "
                    + String.join(", ", indices.getMissingRequiredHeaders()));
        }

        int totalDataRows = records.size() - 1;
        int successCount = 0;
        int failureCount = 0;
        List<Lead> importedLeads = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 1; i < records.size(); i++) {
            List<String> row = records.get(i);
            int rowNumber = i + 1; // 1-based CSV line number for accurate user reporting

            Lead lead = new Lead();

            // Lead ID
            if (indices.leadIdCol >= 0 && indices.leadIdCol < row.size() && !row.get(indices.leadIdCol).isBlank()) {
                lead.setLeadId(row.get(indices.leadIdCol).trim());
            }

            // 1. Personal Details
            if (indices.firstNameBusinessNameCol >= 0 && indices.firstNameBusinessNameCol < row.size()) {
                lead.setFirstNameBusinessName(row.get(indices.firstNameBusinessNameCol));
            }
            if (indices.dobCol >= 0 && indices.dobCol < row.size()) {
                lead.setDob(row.get(indices.dobCol));
            }
            if (indices.ageCol >= 0 && indices.ageCol < row.size()) {
                String val = row.get(indices.ageCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setAge(Integer.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.userCategoryCol >= 0 && indices.userCategoryCol < row.size()) {
                lead.setCustomerType(row.get(indices.userCategoryCol));
            }
            if (indices.mobileNumberCol >= 0 && indices.mobileNumberCol < row.size()) {
                lead.setMobileNumber(row.get(indices.mobileNumberCol));
            }
            if (indices.otpCol >= 0 && indices.otpCol < row.size()) {
                lead.setOtp(row.get(indices.otpCol));
            }
            if (indices.panNumberCol >= 0 && indices.panNumberCol < row.size()) {
                lead.setPanNumber(row.get(indices.panNumberCol));
            }
            if (indices.panValidationStatusCol >= 0 && indices.panValidationStatusCol < row.size()) {
                lead.setPanValidationStatus(row.get(indices.panValidationStatusCol));
            }
            if (indices.aadhaarNumberCol >= 0 && indices.aadhaarNumberCol < row.size()) {
                lead.setAadhaarNumber(row.get(indices.aadhaarNumberCol));
            }
            if (indices.aadhaarValidationStatusCol >= 0 && indices.aadhaarValidationStatusCol < row.size()) {
                lead.setAadhaarValidationStatus(row.get(indices.aadhaarValidationStatusCol));
            }
            if (indices.residentialStatusCol >= 0 && indices.residentialStatusCol < row.size()) {
                lead.setResidentialStatus(row.get(indices.residentialStatusCol));
            }
            if (indices.genderCol >= 0 && indices.genderCol < row.size()) {
                lead.setGender(row.get(indices.genderCol));
            }
            if (indices.maritalStatusCol >= 0 && indices.maritalStatusCol < row.size()) {
                lead.setMaritalStatus(row.get(indices.maritalStatusCol));
            }
            if (indices.passportNumberCol >= 0 && indices.passportNumberCol < row.size()) {
                lead.setPassportNumber(row.get(indices.passportNumberCol));
            }
            if (indices.dedupeStatusCol >= 0 && indices.dedupeStatusCol < row.size()) {
                lead.setDedupeStatus(row.get(indices.dedupeStatusCol));
            }
            if (indices.blacklistStatusCol >= 0 && indices.blacklistStatusCol < row.size()) {
                lead.setBlacklistStatus(row.get(indices.blacklistStatusCol));
            }
            if (indices.lastNameCol >= 0 && indices.lastNameCol < row.size()) {
                lead.setLastName(row.get(indices.lastNameCol));
            }
            if (indices.emailAddressCol >= 0 && indices.emailAddressCol < row.size()) {
                lead.setEmailAddress(row.get(indices.emailAddressCol));
            }
            if (indices.pinCodeCol >= 0 && indices.pinCodeCol < row.size()) {
                lead.setPinCode(row.get(indices.pinCodeCol));
            }
            if (indices.numberOfDependentsCol >= 0 && indices.numberOfDependentsCol < row.size()) {
                String val = row.get(indices.numberOfDependentsCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setNumberOfDependents(Integer.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }

            // 2. Loan Details
            if (indices.loanProductTypeCol >= 0 && indices.loanProductTypeCol < row.size()) {
                lead.setLoanProductType(row.get(indices.loanProductTypeCol));
            }
            if (indices.loanAmountCol >= 0 && indices.loanAmountCol < row.size()) {
                String val = row.get(indices.loanAmountCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setLoanAmount(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.purposeOfLoanCol >= 0 && indices.purposeOfLoanCol < row.size()) {
                lead.setPurposeOfLoan(row.get(indices.purposeOfLoanCol));
            }
            if (indices.tenureCol >= 0 && indices.tenureCol < row.size()) {
                String val = row.get(indices.tenureCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setTenure(Integer.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.numberOfInstalmentsCol >= 0 && indices.numberOfInstalmentsCol < row.size()) {
                String val = row.get(indices.numberOfInstalmentsCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setNumberOfInstalments(Integer.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.emiCol >= 0 && indices.emiCol < row.size()) {
                String val = row.get(indices.emiCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setEmi(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.propertyValueCol >= 0 && indices.propertyValueCol < row.size()) {
                String val = row.get(indices.propertyValueCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setPropertyValue(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.securityAmountCol >= 0 && indices.securityAmountCol < row.size()) {
                String val = row.get(indices.securityAmountCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setSecurityAmount(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.downPaymentCollateralCol >= 0 && indices.downPaymentCollateralCol < row.size()) {
                lead.setDownPaymentCollateral(row.get(indices.downPaymentCollateralCol));
            }

            // 3. Income Profile
            if (indices.employmentTypeCol >= 0 && indices.employmentTypeCol < row.size()) {
                lead.setEmploymentType(row.get(indices.employmentTypeCol));
            }
            if (indices.annualIncomeCol >= 0 && indices.annualIncomeCol < row.size()) {
                String val = row.get(indices.annualIncomeCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setAnnualIncome(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.designationCol >= 0 && indices.designationCol < row.size()) {
                lead.setDesignation(row.get(indices.designationCol));
            }
            if (indices.employerBusinessNameCol >= 0 && indices.employerBusinessNameCol < row.size()) {
                lead.setEmployerBusinessName(row.get(indices.employerBusinessNameCol));
            }
            if (indices.locationCol >= 0 && indices.locationCol < row.size()) {
                lead.setLocation(row.get(indices.locationCol));
            }
            if (indices.stateCol >= 0 && indices.stateCol < row.size()) {
                lead.setState(row.get(indices.stateCol));
            }
            if (indices.takeHomePayCol >= 0 && indices.takeHomePayCol < row.size()) {
                String val = row.get(indices.takeHomePayCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setTakeHomePay(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.deductionsOrEmisPayableCol >= 0 && indices.deductionsOrEmisPayableCol < row.size()) {
                String val = row.get(indices.deductionsOrEmisPayableCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setDeductionsOrEmisPayable(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.bankNameCol >= 0 && indices.bankNameCol < row.size()) {
                lead.setBankName(row.get(indices.bankNameCol));
            }
            if (indices.primaryBankAccountCol >= 0 && indices.primaryBankAccountCol < row.size()) {
                lead.setPrimaryBankAccount(row.get(indices.primaryBankAccountCol));
            }
            if (indices.accountStatementConsentCol >= 0 && indices.accountStatementConsentCol < row.size()) {
                String val = row.get(indices.accountStatementConsentCol).trim();
                if (!val.isEmpty()) {
                    lead.setAccountStatementConsent(Boolean.parseBoolean(val) || "yes".equalsIgnoreCase(val) || "1".equals(val));
                }
            }
            if (indices.cibilLiabilityCheckCol >= 0 && indices.cibilLiabilityCheckCol < row.size()) {
                lead.setCibilLiabilityCheck(row.get(indices.cibilLiabilityCheckCol));
            }
            if (indices.debtToIncomeRatioCol >= 0 && indices.debtToIncomeRatioCol < row.size()) {
                String val = row.get(indices.debtToIncomeRatioCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setDebtToIncomeRatio(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.loanToValueRatioCol >= 0 && indices.loanToValueRatioCol < row.size()) {
                String val = row.get(indices.loanToValueRatioCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setLoanToValueRatio(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.debtServiceCoverageRatioCol >= 0 && indices.debtServiceCoverageRatioCol < row.size()) {
                String val = row.get(indices.debtServiceCoverageRatioCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setDebtServiceCoverageRatio(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }
            if (indices.netDisposableIncomeCol >= 0 && indices.netDisposableIncomeCol < row.size()) {
                String val = row.get(indices.netDisposableIncomeCol).trim();
                if (!val.isEmpty()) {
                    try { lead.setNetDisposableIncome(Double.valueOf(val)); } catch (NumberFormatException ignored) {}
                }
            }

            // 4. Referral Details
            if (indices.sourcingChannelCol >= 0 && indices.sourcingChannelCol < row.size()) {
                lead.setSourcingChannel(row.get(indices.sourcingChannelCol));
            }
            if (indices.referralDateCol >= 0 && indices.referralDateCol < row.size()) {
                lead.setReferralDate(row.get(indices.referralDateCol));
            }
            if (indices.lspPartnerCodeCol >= 0 && indices.lspPartnerCodeCol < row.size()) {
                lead.setLspPartnerCode(row.get(indices.lspPartnerCodeCol));
            }
            if (indices.agentPartnerNameCol >= 0 && indices.agentPartnerNameCol < row.size()) {
                lead.setAgentPartnerName(row.get(indices.agentPartnerNameCol));
            }
            if (indices.sourcingEmployeeIdCol >= 0 && indices.sourcingEmployeeIdCol < row.size()) {
                lead.setSourcingEmployeeId(row.get(indices.sourcingEmployeeIdCol));
            }
            if (indices.sourcingEmployeeNameCol >= 0 && indices.sourcingEmployeeNameCol < row.size()) {
                lead.setSourcingEmployeeName(row.get(indices.sourcingEmployeeNameCol));
            }

            // System Fields
            String csvStatus = (indices.leadStatusCol >= 0 && indices.leadStatusCol < row.size() && !row.get(indices.leadStatusCol).isBlank())
                    ? row.get(indices.leadStatusCol).trim() : null;
            String csvAssignedEmp = (indices.assignedEmployeeIdCol >= 0 && indices.assignedEmployeeIdCol < row.size() && !row.get(indices.assignedEmployeeIdCol).isBlank())
                    ? row.get(indices.assignedEmployeeIdCol).trim() : null;
            String csvAssignedBy = (indices.assignedByCol >= 0 && indices.assignedByCol < row.size() && !row.get(indices.assignedByCol).isBlank())
                    ? row.get(indices.assignedByCol).trim() : null;
            LocalDateTime csvAssignTime = null;
            if (indices.assignmentTimestampCol >= 0 && indices.assignmentTimestampCol < row.size() && !row.get(indices.assignmentTimestampCol).isBlank()) {
                try {
                    csvAssignTime = LocalDateTime.parse(row.get(indices.assignmentTimestampCol).trim());
                } catch (Exception ignored) {}
            }

            try {
                Lead saved = createLead(lead);
                if (csvStatus != null || csvAssignedEmp != null || csvAssignedBy != null || csvAssignTime != null) {
                    if (csvStatus != null) saved.setLeadStatus(csvStatus);
                    if (csvAssignedEmp != null) saved.setAssignedEmployeeId(csvAssignedEmp);
                    if (csvAssignedBy != null) saved.setAssignedBy(csvAssignedBy);
                    if (csvAssignTime != null) saved.setAssignmentTimestamp(csvAssignTime);
                    saved = leadRepository.save(saved);
                }
                importedLeads.add(saved);
                successCount++;
            } catch (ValidationException ve) {
                failureCount++;
                errors.add("Row " + rowNumber + ": " + String.join(", ", ve.getErrors()));
            } catch (Exception ex) {
                failureCount++;
                errors.add("Row " + rowNumber + ": " + ex.getMessage());
            }
        }

        String summary = String.format("Processed %d rows: %d successfully imported, %d failed.",
                totalDataRows, successCount, failureCount);

        return new CsvImportResult(totalDataRows, successCount, failureCount, importedLeads, errors, summary);
    }

    /**
     * Updates an existing Lead's details across all 4 sections:
     * 1. Personal Details
     * 2. Loan Details
     * 3. Income Profile
     * 4. Referral Details
     * System-managed fields (Lead ID, Lead Status, Assigned Employee, Assignment Timestamp, Assigned By)
     * are strictly preserved and cannot be overwritten by client input.
     *
     * @param leadId Lead identifier to update
     * @param updateData Payload containing updated values
     * @return saved updated Lead
     */
    @Transactional
    public Lead updateLead(String leadId, LeadRequest updateData) {
        ensureTenantContext();
        if (leadId == null || leadId.trim().isEmpty()) {
            throw new ValidationException("Lead ID path parameter cannot be empty");
        }
        if (updateData == null) {
            throw new ValidationException("Lead update payload cannot be null");
        }

        String trimmedId = leadId.trim();
        Lead existingLead = leadRepository.findById(trimmedId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead with ID '" + trimmedId + "' not found"));

        // 1. Personal Details
        if (updateData.getFirstNameBusinessName() != null) existingLead.setFirstNameBusinessName(updateData.getFirstNameBusinessName());
        if (updateData.getLastName() != null) existingLead.setLastName(updateData.getLastName());
        if (updateData.getDob() != null) existingLead.setDob(updateData.getDob());
        if (updateData.getAge() != null) existingLead.setAge(updateData.getAge());
        if (updateData.getCustomerType() != null) existingLead.setCustomerType(updateData.getCustomerType());
        if (updateData.getMobileNumber() != null) existingLead.setMobileNumber(updateData.getMobileNumber());
        if (updateData.getOtp() != null) existingLead.setOtp(updateData.getOtp());
        if (updateData.getPanNumber() != null) existingLead.setPanNumber(updateData.getPanNumber());
        if (updateData.getPanValidationStatus() != null) existingLead.setPanValidationStatus(updateData.getPanValidationStatus());
        if (updateData.getAadhaarNumber() != null) existingLead.setAadhaarNumber(updateData.getAadhaarNumber());
        if (updateData.getAadhaarValidationStatus() != null) existingLead.setAadhaarValidationStatus(updateData.getAadhaarValidationStatus());
        if (updateData.getResidentialStatus() != null) existingLead.setResidentialStatus(updateData.getResidentialStatus());
        if (updateData.getGender() != null) existingLead.setGender(updateData.getGender());
        if (updateData.getMaritalStatus() != null) existingLead.setMaritalStatus(updateData.getMaritalStatus());
        if (updateData.getPassportNumber() != null) existingLead.setPassportNumber(updateData.getPassportNumber());
        if (updateData.getDedupeStatus() != null) existingLead.setDedupeStatus(updateData.getDedupeStatus());
        if (updateData.getBlacklistStatus() != null) existingLead.setBlacklistStatus(updateData.getBlacklistStatus());
        if (updateData.getNumberOfDependents() != null) existingLead.setNumberOfDependents(updateData.getNumberOfDependents());
        if (updateData.getEmailAddress() != null) existingLead.setEmailAddress(updateData.getEmailAddress());
        if (updateData.getPinCode() != null) existingLead.setPinCode(updateData.getPinCode());

        // 2. Loan Details
        if (updateData.getLoanProductType() != null) existingLead.setLoanProductType(updateData.getLoanProductType());
        if (updateData.getLoanAmount() != null) existingLead.setLoanAmount(updateData.getLoanAmount());
        if (updateData.getPurposeOfLoan() != null) existingLead.setPurposeOfLoan(updateData.getPurposeOfLoan());
        if (updateData.getTenure() != null) existingLead.setTenure(updateData.getTenure());
        if (updateData.getNumberOfInstalments() != null) existingLead.setNumberOfInstalments(updateData.getNumberOfInstalments());
        if (updateData.getEmi() != null) existingLead.setEmi(updateData.getEmi());
        if (updateData.getPropertyValue() != null) existingLead.setPropertyValue(updateData.getPropertyValue());
        if (updateData.getSecurityAmount() != null) existingLead.setSecurityAmount(updateData.getSecurityAmount());
        if (updateData.getDownPaymentCollateral() != null) existingLead.setDownPaymentCollateral(updateData.getDownPaymentCollateral());

        // 3. Income Profile
        if (updateData.getEmploymentType() != null) existingLead.setEmploymentType(updateData.getEmploymentType());
        if (updateData.getAnnualIncome() != null) existingLead.setAnnualIncome(updateData.getAnnualIncome());
        if (updateData.getDesignation() != null) existingLead.setDesignation(updateData.getDesignation());
        if (updateData.getEmployerBusinessName() != null) existingLead.setEmployerBusinessName(updateData.getEmployerBusinessName());
        if (updateData.getLocation() != null) existingLead.setLocation(updateData.getLocation());
        if (updateData.getState() != null) existingLead.setState(updateData.getState());
        if (updateData.getTakeHomePay() != null) existingLead.setTakeHomePay(updateData.getTakeHomePay());
        if (updateData.getDeductionsOrEmisPayable() != null) existingLead.setDeductionsOrEmisPayable(updateData.getDeductionsOrEmisPayable());
        if (updateData.getBankName() != null) existingLead.setBankName(updateData.getBankName());
        if (updateData.getPrimaryBankAccount() != null) existingLead.setPrimaryBankAccount(updateData.getPrimaryBankAccount());
        if (updateData.getAccountStatementConsent() != null) existingLead.setAccountStatementConsent(updateData.getAccountStatementConsent());
        if (updateData.getCibilLiabilityCheck() != null) existingLead.setCibilLiabilityCheck(updateData.getCibilLiabilityCheck());
        if (updateData.getDebtToIncomeRatio() != null) existingLead.setDebtToIncomeRatio(updateData.getDebtToIncomeRatio());
        if (updateData.getLoanToValueRatio() != null) existingLead.setLoanToValueRatio(updateData.getLoanToValueRatio());
        if (updateData.getDebtServiceCoverageRatio() != null) existingLead.setDebtServiceCoverageRatio(updateData.getDebtServiceCoverageRatio());
        if (updateData.getNetDisposableIncome() != null) existingLead.setNetDisposableIncome(updateData.getNetDisposableIncome());

        // 4. Referral Details
        if (updateData.getSourcingChannel() != null) existingLead.setSourcingChannel(updateData.getSourcingChannel());
        if (updateData.getReferralDate() != null) existingLead.setReferralDate(updateData.getReferralDate());
        if (updateData.getLspPartnerCode() != null) existingLead.setLspPartnerCode(updateData.getLspPartnerCode());
        if (updateData.getAgentPartnerName() != null) existingLead.setAgentPartnerName(updateData.getAgentPartnerName());
        if (updateData.getSourcingEmployeeId() != null) existingLead.setSourcingEmployeeId(updateData.getSourcingEmployeeId());
        if (updateData.getSourcingEmployeeName() != null) existingLead.setSourcingEmployeeName(updateData.getSourcingEmployeeName());

        // System-managed fields: leadId, leadStatus, assignedEmployeeId, assignmentTimestamp, assignedBy are NOT modified here

        // Validate updated fields
        leadValidator.validateAndNormalize(existingLead);

        // Duplicate check for mobileNumber if changed
        if (existingLead.getMobileNumber() != null) {
            Optional<Lead> dupMobile = leadRepository.findByMobileNumber(existingLead.getMobileNumber());
            if (dupMobile.isPresent() && !dupMobile.get().getLeadId().equals(trimmedId)) {
                throw new ValidationException("Duplicate lead detected: A lead with Mobile Number '" + existingLead.getMobileNumber() + "' already exists");
            }
        }

        // Duplicate check for panNumber if changed
        if (existingLead.getPanNumber() != null) {
            Optional<Lead> dupPan = leadRepository.findByPanNumber(existingLead.getPanNumber());
            if (dupPan.isPresent() && !dupPan.get().getLeadId().equals(trimmedId)) {
                throw new ValidationException("Duplicate lead detected: A lead with PAN '" + existingLead.getPanNumber() + "' already exists");
            }
        }

        // Duplicate check for aadhaarNumber if changed
        if (existingLead.getAadhaarNumber() != null) {
            Optional<Lead> dupAadhaar = leadRepository.findByAadhaarNumber(existingLead.getAadhaarNumber());
            if (dupAadhaar.isPresent() && !dupAadhaar.get().getLeadId().equals(trimmedId)) {
                throw new ValidationException("Duplicate lead detected: A lead with Aadhaar Number '" + existingLead.getAadhaarNumber() + "' already exists");
            }
        }

        return leadRepository.save(existingLead);
    }

    /**
     * Overload for direct Lead entity calls.
     */
    @Transactional
    public Lead updateLead(String leadId, Lead lead) {
        return updateLead(leadId, (LeadRequest) lead);
    }

    /**
     * Assigns a Lead to an active Bank User with MAKER role.
     * Validates that lead exists, Maker user exists and is active, and Assigner has ADMIN role.
     * Updates assignedEmployeeId, sets leadStatus = ASSIGNED, stores assignmentTimestamp, assignedBy.
     *
     * @param leadId Lead identifier to assign
     * @param request Payload containing employeeId (user identifier) and optional assignedBy
     * @return LeadAssignResponse matching API contract
     */
    @Transactional
    public LeadAssignResponse assignLead(String leadId, LeadAssignRequest request) {
        ensureTenantContext();
        if (leadId == null || leadId.trim().isEmpty()) {
            throw new ValidationException("Lead ID path parameter cannot be empty");
        }
        if (request == null || request.getEmployeeId() == null || request.getEmployeeId().trim().isEmpty()) {
            throw new ValidationException("Employee ID is required");
        }

        String trimmedLeadId = leadId.trim();
        String trimmedEmployeeId = request.getEmployeeId().trim();

        // 1. Validate Lead exists
        Lead lead = leadRepository.findById(trimmedLeadId)
                .orElseThrow(() -> new ResourceNotFoundException("Lead with ID '" + trimmedLeadId + "' not found"));

        // 2. Validate Maker User exists, is ACTIVE/OPERATIVE, and has MAKER role
        if (leadUserService == null) {
            throw new IllegalStateException("LeadUserService is not configured");
        }
        OrganizationUser makerUser = leadUserService.validateAndGetMakerUser(trimmedEmployeeId);

        // 3. Validate Admin Assigner (assignedBy)
        String assignerId = (request.getAssignedBy() != null && !request.getAssignedBy().trim().isEmpty())
                ? request.getAssignedBy().trim()
                : "EMP101";

        OrganizationUser adminUser = leadUserService.validateAndGetAdminUser(assignerId);

        // 4. Update assignment state (use empNo if available, otherwise username)
        String assignedEmpCode = (makerUser.getEmpNo() != null && !makerUser.getEmpNo().isBlank())
                ? makerUser.getEmpNo() : makerUser.getUsername();
        String assignedByCode = (adminUser.getEmpNo() != null && !adminUser.getEmpNo().isBlank())
                ? adminUser.getEmpNo() : adminUser.getUsername();

        lead.setAssignedEmployeeId(assignedEmpCode);
        lead.setLeadStatus("ASSIGNED");
        lead.setAssignmentTimestamp(LocalDateTime.now());
        lead.setAssignedBy(assignedByCode);

        // 5. Save to database
        leadRepository.save(lead);
        log.info("Lead {} successfully assigned to Maker user {} by {}", lead.getLeadId(), assignedEmpCode, assignedByCode);

        // 6. Build and return success response
        return new LeadAssignResponse(
                "Lead assigned successfully",
                lead.getLeadId(),
                lead.getAssignedEmployeeId(),
                lead.getLeadStatus(),
                lead.getAssignedBy()
        );
    }

    /**
     * Exports all persisted leads into an RFC-4180 compliant CSV byte array matching CsvHelper.CSV_HEADERS.
     */
    public byte[] exportLeadsToCsv() {
        ensureTenantContext();
        List<Lead> leads = leadRepository.findAll();
        StringBuilder sb = new StringBuilder();

        // Write Header
        sb.append(CsvHelper.toCsvLine(List.of(CsvHelper.CSV_HEADERS)));

        // Write Lead Rows
        for (Lead lead : leads) {
            List<String> row = List.of(
                    lead.getLeadId() != null ? lead.getLeadId() : "",
                    // 1. Personal Details
                    lead.getFirstNameBusinessName() != null ? lead.getFirstNameBusinessName() : "",
                    lead.getDob() != null ? lead.getDob() : "",
                    lead.getAge() != null ? String.valueOf(lead.getAge()) : "",
                    lead.getCustomerType() != null ? lead.getCustomerType() : "Individual",
                    lead.getMobileNumber() != null ? lead.getMobileNumber() : "",
                    lead.getOtp() != null ? lead.getOtp() : "",
                    lead.getPanNumber() != null ? lead.getPanNumber() : "",
                    lead.getPanValidationStatus() != null ? lead.getPanValidationStatus() : "",
                    lead.getAadhaarNumber() != null ? lead.getAadhaarNumber() : "",
                    lead.getAadhaarValidationStatus() != null ? lead.getAadhaarValidationStatus() : "",
                    lead.getResidentialStatus() != null ? lead.getResidentialStatus() : "",
                    lead.getGender() != null ? lead.getGender() : "",
                    lead.getMaritalStatus() != null ? lead.getMaritalStatus() : "",
                    lead.getPassportNumber() != null ? lead.getPassportNumber() : "",
                    lead.getDedupeStatus() != null ? lead.getDedupeStatus() : "",
                    lead.getBlacklistStatus() != null ? lead.getBlacklistStatus() : "",
                    lead.getLastName() != null ? lead.getLastName() : "",
                    lead.getEmailAddress() != null ? lead.getEmailAddress() : "",
                    lead.getPinCode() != null ? lead.getPinCode() : "",
                    lead.getNumberOfDependents() != null ? String.valueOf(lead.getNumberOfDependents()) : "",
                    // 2. Loan Details
                    lead.getLoanProductType() != null ? lead.getLoanProductType() : "",
                    lead.getLoanAmount() != null ? String.valueOf(lead.getLoanAmount()) : "",
                    lead.getPurposeOfLoan() != null ? lead.getPurposeOfLoan() : "",
                    lead.getTenure() != null ? String.valueOf(lead.getTenure()) : "",
                    lead.getNumberOfInstalments() != null ? String.valueOf(lead.getNumberOfInstalments()) : "",
                    lead.getEmi() != null ? String.valueOf(lead.getEmi()) : "",
                    lead.getPropertyValue() != null ? String.valueOf(lead.getPropertyValue()) : "",
                    lead.getSecurityAmount() != null ? String.valueOf(lead.getSecurityAmount()) : "",
                    lead.getDownPaymentCollateral() != null ? lead.getDownPaymentCollateral() : "",
                    // 3. Income Profile
                    lead.getEmploymentType() != null ? lead.getEmploymentType() : "",
                    lead.getAnnualIncome() != null ? String.valueOf(lead.getAnnualIncome()) : "",
                    lead.getDesignation() != null ? lead.getDesignation() : "",
                    lead.getEmployerBusinessName() != null ? lead.getEmployerBusinessName() : "",
                    lead.getLocation() != null ? lead.getLocation() : "",
                    lead.getState() != null ? lead.getState() : "",
                    lead.getTakeHomePay() != null ? String.valueOf(lead.getTakeHomePay()) : "",
                    lead.getDeductionsOrEmisPayable() != null ? String.valueOf(lead.getDeductionsOrEmisPayable()) : "",
                    lead.getBankName() != null ? lead.getBankName() : "",
                    lead.getPrimaryBankAccount() != null ? lead.getPrimaryBankAccount() : "",
                    lead.getAccountStatementConsent() != null ? String.valueOf(lead.getAccountStatementConsent()) : "",
                    lead.getCibilLiabilityCheck() != null ? lead.getCibilLiabilityCheck() : "",
                    lead.getDebtToIncomeRatio() != null ? String.valueOf(lead.getDebtToIncomeRatio()) : "",
                    lead.getLoanToValueRatio() != null ? String.valueOf(lead.getLoanToValueRatio()) : "",
                    lead.getDebtServiceCoverageRatio() != null ? String.valueOf(lead.getDebtServiceCoverageRatio()) : "",
                    lead.getNetDisposableIncome() != null ? String.valueOf(lead.getNetDisposableIncome()) : "",
                    // 4. Referral Details
                    lead.getSourcingChannel() != null ? lead.getSourcingChannel() : "",
                    lead.getReferralDate() != null ? lead.getReferralDate() : "",
                    lead.getLspPartnerCode() != null ? lead.getLspPartnerCode() : "",
                    lead.getAgentPartnerName() != null ? lead.getAgentPartnerName() : "",
                    lead.getSourcingEmployeeId() != null ? lead.getSourcingEmployeeId() : "",
                    lead.getSourcingEmployeeName() != null ? lead.getSourcingEmployeeName() : "",
                    // System Fields
                    lead.getLeadStatus() != null ? lead.getLeadStatus() : "NEW",
                    lead.getAssignedEmployeeId() != null ? lead.getAssignedEmployeeId() : "",
                    lead.getAssignmentTimestamp() != null ? lead.getAssignmentTimestamp().toString() : "",
                    lead.getAssignedBy() != null ? lead.getAssignedBy() : ""
            );
            sb.append(CsvHelper.toCsvLine(row));
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns pre-filled sample CSV bytes for client download and testing.
     */
    public byte[] getSampleCsv() {
        return CsvHelper.generateSampleCsvContent().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Retrieves a Lead by its Lead ID from database.
     */
    public Optional<Lead> getLeadById(String leadId) {
        ensureTenantContext();
        return leadRepository.findById(leadId);
    }

    /**
     * Retrieves all existing Leads from database.
     */
    public List<Lead> getAllLeads() {
        ensureTenantContext();
        return leadRepository.findAll();
    }
}
