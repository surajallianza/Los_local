package com.bank.los.bank.lead.service;

import com.bank.los.bank.lead.entity.Lead;
import com.bank.los.bank.lead.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validator component enforcing all field-level and conditional rules for Lead entity:
 * 1. Personal Details: Name of Customer, Date of Birth, Age, Customer Type, Mobile Number,
 *    OTP, PAN Card, PAN Validate, Aadhaar Card, Aadhaar Validate, Residential Status,
 *    Gender, Marital Status, Passport No., De-Duplicate Check, Blacklist Check.
 * 2. Loan Details: Loan Product Type, Loan Amount, Purpose of Loan, Tenure, No. of Instalments,
 *    EMI, Property Value, Security Amount.
 * 3. Income Profile: Employment Type, Annual Income, Designation, Employer Name, Location, State,
 *    Take Home Pay, Deductions / EMIs Payable, Bank Name, Account Number, Account Statement Consent,
 *    CIBIL Liability Check, Debt-to-Income (DTI), Loan-to-Value (LTV), Debt Service Coverage Ratio (DSCR),
 *    Net Disposable Income (NDI).
 * 4. Referral Details: Lead Acquisition Channel, Date, Sourcing Agent / Partner ID, Agent / Partner Name,
 *    Employee ID, Employee Name.
 */
@Component
public class LeadValidator {

    private static final Pattern LEAD_ID_PATTERN = Pattern.compile("^LD\\d{6}\\d{4,}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\d{10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PIN_CODE_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern PAN_PATTERN = Pattern.compile("^[A-Z]{5}[0-9]{4}[A-Z]{1}$");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("^\\d{12}$");
    private static final Pattern OTP_PATTERN = Pattern.compile("^\\d{4,6}$");
    private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("^[0-9A-Za-z]{9,18}$");

    public void validateAndNormalize(Lead lead) {
        if (lead == null) {
            throw new ValidationException("Lead payload cannot be null");
        }

        List<String> errors = new ArrayList<>();

        // 1. Lead ID (if provided, validate format; if not, generated in service)
        if (lead.getLeadId() != null && !lead.getLeadId().trim().isEmpty()) {
            String trimmedId = lead.getLeadId().trim();
            if (!LEAD_ID_PATTERN.matcher(trimmedId).matches()) {
                errors.add("Lead ID must follow format LDYYYYMM#### (e.g. LD2026090001)");
            } else {
                lead.setLeadId(trimmedId);
            }
        }

        // 2. Sourcing Channel / Lead Acquisition Channel (Mandatory)
        String channel = lead.getSourcingChannel();
        if (channel == null || channel.trim().isEmpty()) {
            errors.add("Sourcing Channel is mandatory and must be one of: Website, Mobile App, Branch Office, Partner Network, Sales Team, Customer Referral");
        } else {
            String trimmedChannel = channel.trim();
            if ("Website".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Website");
            } else if ("Mobile App".equalsIgnoreCase(trimmedChannel) || "MobileApp".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Mobile App");
            } else if ("Branch Office".equalsIgnoreCase(trimmedChannel) || "BranchOffice".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Branch Office");
            } else if ("Branch".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Branch");
            } else if ("Partner Network".equalsIgnoreCase(trimmedChannel) || "PartnerNetwork".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Partner Network");
            } else if ("Sales Team".equalsIgnoreCase(trimmedChannel) || "SalesTeam".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Sales Team");
            } else if ("Customer Referral".equalsIgnoreCase(trimmedChannel) || "CustomerReferral".equalsIgnoreCase(trimmedChannel)) {
                lead.setSourcingChannel("Customer Referral");
            } else {
                errors.add("Sourcing Channel must be one of: Website, Mobile App, Branch Office, Partner Network, Sales Team, Customer Referral");
            }
        }

        // 3. LSP / Partner Code (Conditional: Mandatory when Sourcing Channel is 'Branch' or 'Branch Office')
        String lsp = lead.getLspPartnerCode();
        if ("Branch".equalsIgnoreCase(lead.getSourcingChannel()) || "Branch Office".equalsIgnoreCase(lead.getSourcingChannel())) {
            if (lsp == null || lsp.trim().isEmpty()) {
                errors.add("LSP / Partner Code is mandatory when Sourcing Channel is '" + lead.getSourcingChannel() + "'");
            } else {
                lead.setLspPartnerCode(lsp.trim());
            }
        } else {
            if (lsp != null && !lsp.trim().isEmpty()) {
                lead.setLspPartnerCode(lsp.trim());
            } else {
                lead.setLspPartnerCode(null);
            }
        }

        // 4. Customer Type / User Category (Mandatory)
        String category = lead.getCustomerType();
        if (category == null || category.trim().isEmpty()) {
            lead.setCustomerType("Individual");
        } else {
            String trimmedCategory = category.trim();
            if ("Individual".equalsIgnoreCase(trimmedCategory)) {
                lead.setCustomerType("Individual");
            } else if ("Non-Individual".equalsIgnoreCase(trimmedCategory)
                    || "NonIndividual".equalsIgnoreCase(trimmedCategory)
                    || "Non Individual".equalsIgnoreCase(trimmedCategory)) {
                lead.setCustomerType("Non-Individual");
            } else if ("Sole Proprietorship".equalsIgnoreCase(trimmedCategory)
                    || "Partnership".equalsIgnoreCase(trimmedCategory)
                    || "Private Limited".equalsIgnoreCase(trimmedCategory)) {
                lead.setCustomerType(trimmedCategory);
            } else {
                errors.add("User Category must be either 'Individual' or 'Non-Individual'");
            }
        }

        // 5. First Name / Business Name / Customer Name (Mandatory: Free Text)
        String firstName = lead.getFirstNameBusinessName();
        if (firstName == null || firstName.trim().isEmpty()) {
            errors.add("First Name / Business Name is mandatory");
        } else {
            lead.setFirstNameBusinessName(firstName.trim());
        }

        // 6. Last Name (Conditional / Optional for all categories)
        String lastName = lead.getLastName();
        if (lastName != null && !lastName.trim().isEmpty()) {
            lead.setLastName(lastName.trim());
        } else {
            lead.setLastName(null);
        }

        // 7. Mobile Number (Mandatory: 10-digit number)
        String mobile = lead.getMobileNumber();
        if (mobile == null || mobile.trim().isEmpty()) {
            errors.add("Mobile Number is mandatory and must be a 10-digit number");
        } else {
            String cleanedMobile = mobile.trim().replaceAll("[\\s-]", "");
            if (!MOBILE_PATTERN.matcher(cleanedMobile).matches()) {
                errors.add("Mobile Number must be a valid 10-digit number");
            } else {
                lead.setMobileNumber(cleanedMobile);
            }
        }

        // 8. Email Address (Optional: Standard Email Format if provided)
        String email = lead.getEmailAddress();
        if (email != null && !email.trim().isEmpty()) {
            String trimmedEmail = email.trim();
            if (!EMAIL_PATTERN.matcher(trimmedEmail).matches()) {
                errors.add("Email Address must be a valid email format (e.g. user@example.com)");
            } else {
                lead.setEmailAddress(trimmedEmail);
            }
        } else {
            lead.setEmailAddress(null);
        }

        // 9. Pin Code (Optional: Exactly 6-digit number if provided)
        String pin = lead.getPinCode();
        if (pin != null && !pin.trim().isEmpty()) {
            String trimmedPin = pin.trim().replaceAll("\\s+", "");
            if (!PIN_CODE_PATTERN.matcher(trimmedPin).matches()) {
                errors.add("Pin Code must be a valid 6-digit number");
            } else {
                lead.setPinCode(trimmedPin);
            }
        } else {
            lead.setPinCode(null);
        }

        // 10. PAN Number (Optional / Conditional: 5 letters + 4 digits + 1 letter)
        String pan = lead.getPanNumber();
        if (pan != null && !pan.trim().isEmpty()) {
            String normalizedPan = pan.trim().toUpperCase();
            if (!PAN_PATTERN.matcher(normalizedPan).matches()) {
                errors.add("PAN must follow standard format (e.g. ABCDE1234F)");
            } else {
                lead.setPanNumber(normalizedPan);
            }
        } else {
            lead.setPanNumber(null);
        }

        // 11. Aadhaar Number (Optional / Conditional: 12-digit number)
        String aadhaar = lead.getAadhaarNumber();
        if (aadhaar != null && !aadhaar.trim().isEmpty()) {
            String cleanedAadhaar = aadhaar.trim().replaceAll("[\\s-]", "");
            if (!AADHAAR_PATTERN.matcher(cleanedAadhaar).matches()) {
                errors.add("Aadhaar Number must be a valid 12-digit number");
            } else {
                lead.setAadhaarNumber(cleanedAadhaar);
            }
        } else {
            lead.setAadhaarNumber(null);
        }

        // 12. Date of Birth (DOB) and Age calculation
        String dob = lead.getDob();
        if (dob != null && !dob.trim().isEmpty()) {
            String trimmedDob = dob.trim();
            try {
                LocalDate dobDate = LocalDate.parse(trimmedDob);
                LocalDate today = LocalDate.now();
                if (dobDate.isAfter(today)) {
                    errors.add("Date of Birth cannot be in the future");
                } else if (dobDate.isAfter(today.minusYears(18))) {
                    errors.add("Applicant must be at least 18 years of age");
                } else {
                    lead.setDob(trimmedDob);
                    int calculatedAge = Period.between(dobDate, today).getYears();
                    if (lead.getAge() == null) {
                        lead.setAge(calculatedAge);
                    }
                }
            } catch (DateTimeParseException ex) {
                errors.add("Date of Birth (DOB) must follow format YYYY-MM-DD");
            }
        } else {
            lead.setDob(null);
        }

        // Age validation if provided directly
        if (lead.getAge() != null && lead.getAge() < 0) {
            errors.add("Age must be a positive number");
        }

        // 13. OTP (Optional / Conditional: 4 to 6 digits)
        String otp = lead.getOtp();
        if (otp != null && !otp.trim().isEmpty()) {
            String trimmedOtp = otp.trim();
            if (!OTP_PATTERN.matcher(trimmedOtp).matches()) {
                errors.add("OTP must be a 4 to 6 digit verification code");
            } else {
                lead.setOtp(trimmedOtp);
            }
        } else {
            lead.setOtp(null);
        }

        // 14. Financial Values & Loan Details
        Double annualIncome = lead.getAnnualIncome();
        if (annualIncome != null && annualIncome < 0) {
            errors.add("Annual Income must be a non-negative amount");
        }

        Double loanAmount = lead.getLoanAmount();
        if (loanAmount != null && loanAmount <= 0) {
            errors.add("Loan Amount must be greater than zero");
        }

        Integer tenure = lead.getTenure();
        if (tenure != null && tenure <= 0) {
            errors.add("Tenure must be greater than zero");
        }

        Integer instalments = lead.getNumberOfInstalments();
        if (instalments != null && instalments <= 0) {
            errors.add("Number of instalments must be greater than zero");
        }

        Double emi = lead.getEmi();
        if (emi != null && emi < 0) {
            errors.add("EMI must be a non-negative amount");
        }

        Double propertyValue = lead.getPropertyValue();
        if (propertyValue != null && propertyValue < 0) {
            errors.add("Property Value must be a non-negative amount");
        }

        Double securityAmount = lead.getSecurityAmount();
        if (securityAmount != null && securityAmount < 0) {
            errors.add("Security Amount must be a non-negative amount");
        }

        Double takeHomePay = lead.getTakeHomePay();
        if (takeHomePay != null && takeHomePay < 0) {
            errors.add("Take Home Pay must be a non-negative amount");
        }

        Double deductions = lead.getDeductionsOrEmisPayable();
        if (deductions != null && deductions < 0) {
            errors.add("Deductions / EMIs Payable must be a non-negative amount");
        }

        Double dti = lead.getDebtToIncomeRatio();
        if (dti != null && dti < 0) {
            errors.add("Debt-to-Income (DTI) ratio must be non-negative");
        }

        Double ltv = lead.getLoanToValueRatio();
        if (ltv != null && ltv < 0) {
            errors.add("Loan-to-Value (LTV) ratio must be non-negative");
        }

        Double dscr = lead.getDebtServiceCoverageRatio();
        if (dscr != null && dscr < 0) {
            errors.add("Debt Service Coverage Ratio (DSCR) must be non-negative");
        }

        Integer dependents = lead.getNumberOfDependents();
        if (dependents != null && dependents < 0) {
            errors.add("Number of Dependents must be a non-negative number");
        }

        // Primary Bank Account
        String bankAccount = lead.getPrimaryBankAccount();
        if (bankAccount != null && !bankAccount.trim().isEmpty()) {
            String cleanedAccount = bankAccount.trim().replaceAll("\\s+", "");
            if (!ACCOUNT_NUMBER_PATTERN.matcher(cleanedAccount).matches()) {
                errors.add("Primary Bank Account must be between 9 and 18 alphanumeric characters");
            } else {
                lead.setPrimaryBankAccount(cleanedAccount);
            }
        } else {
            lead.setPrimaryBankAccount(null);
        }

        // Free text fields normalization
        if (lead.getResidentialStatus() != null) lead.setResidentialStatus(lead.getResidentialStatus().trim());
        if (lead.getGender() != null) lead.setGender(lead.getGender().trim());
        if (lead.getMaritalStatus() != null) lead.setMaritalStatus(lead.getMaritalStatus().trim());
        if (lead.getPassportNumber() != null) lead.setPassportNumber(lead.getPassportNumber().trim().toUpperCase());
        if (lead.getPanValidationStatus() != null) lead.setPanValidationStatus(lead.getPanValidationStatus().trim());
        if (lead.getAadhaarValidationStatus() != null) lead.setAadhaarValidationStatus(lead.getAadhaarValidationStatus().trim());
        if (lead.getDedupeStatus() != null) lead.setDedupeStatus(lead.getDedupeStatus().trim());
        if (lead.getBlacklistStatus() != null) lead.setBlacklistStatus(lead.getBlacklistStatus().trim());

        if (lead.getLoanProductType() != null) lead.setLoanProductType(lead.getLoanProductType().trim());
        if (lead.getPurposeOfLoan() != null) lead.setPurposeOfLoan(lead.getPurposeOfLoan().trim());
        if (lead.getDownPaymentCollateral() != null) lead.setDownPaymentCollateral(lead.getDownPaymentCollateral().trim());

        if (lead.getEmploymentType() != null) lead.setEmploymentType(lead.getEmploymentType().trim());
        if (lead.getDesignation() != null) lead.setDesignation(lead.getDesignation().trim());
        if (lead.getEmployerBusinessName() != null) lead.setEmployerBusinessName(lead.getEmployerBusinessName().trim());
        if (lead.getLocation() != null) lead.setLocation(lead.getLocation().trim());
        if (lead.getState() != null) lead.setState(lead.getState().trim());
        if (lead.getBankName() != null) lead.setBankName(lead.getBankName().trim());
        if (lead.getCibilLiabilityCheck() != null) lead.setCibilLiabilityCheck(lead.getCibilLiabilityCheck().trim());

        if (lead.getReferralDate() != null) lead.setReferralDate(lead.getReferralDate().trim());
        if (lead.getAgentPartnerName() != null) lead.setAgentPartnerName(lead.getAgentPartnerName().trim());
        if (lead.getSourcingEmployeeId() != null) lead.setSourcingEmployeeId(lead.getSourcingEmployeeId().trim());
        if (lead.getSourcingEmployeeName() != null) lead.setSourcingEmployeeName(lead.getSourcingEmployeeName().trim());

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
