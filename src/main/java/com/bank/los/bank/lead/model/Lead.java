package com.example.demo.lead.model;

import com.example.demo.lead.dto.LeadRequest;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Lead entity model stored in SQLite database (table: leads).
 * Extends LeadRequest to inherit all 4-section multi-step form fields, annotations,
 * and getters/setters without duplicating field declarations:
 * 1. Personal Details
 * 2. Loan Details
 * 3. Income Profile
 * 4. Referral Details
 */
@Entity
@Table(name = "leads")
public class Lead extends LeadRequest {

    public Lead() {
        super();
    }

    public Lead(String leadId, String sourcingChannel, String lspPartnerCode, String userCategory,
                String firstNameBusinessName, String lastName, String mobileNumber, String emailAddress) {
        super();
        setLeadId(leadId);
        setSourcingChannel(sourcingChannel);
        setLspPartnerCode(lspPartnerCode);
        setCustomerType(userCategory);
        setFirstNameBusinessName(firstNameBusinessName);
        setLastName(lastName);
        setMobileNumber(mobileNumber);
        setEmailAddress(emailAddress);
    }

    public Lead(LeadRequest request) {
        super();
        if (request != null) {
            // System fields
            setLeadId(request.getLeadId());
            setLeadStatus(request.getLeadStatus());
            setAssignedEmployeeId(request.getAssignedEmployeeId());
            setAssignmentTimestamp(request.getAssignmentTimestamp());
            setAssignedBy(request.getAssignedBy());

            // 1. Personal Details
            setFirstNameBusinessName(request.getFirstNameBusinessName());
            setLastName(request.getLastName());
            setDob(request.getDob());
            setAge(request.getAge());
            setCustomerType(request.getCustomerType());
            setMobileNumber(request.getMobileNumber());
            setOtp(request.getOtp());
            setPanNumber(request.getPanNumber());
            setPanValidationStatus(request.getPanValidationStatus());
            setAadhaarNumber(request.getAadhaarNumber());
            setAadhaarValidationStatus(request.getAadhaarValidationStatus());
            setResidentialStatus(request.getResidentialStatus());
            setGender(request.getGender());
            setMaritalStatus(request.getMaritalStatus());
            setPassportNumber(request.getPassportNumber());
            setDedupeStatus(request.getDedupeStatus());
            setBlacklistStatus(request.getBlacklistStatus());
            setNumberOfDependents(request.getNumberOfDependents());
            setEmailAddress(request.getEmailAddress());
            setPinCode(request.getPinCode());

            // 2. Loan Details
            setLoanProductType(request.getLoanProductType());
            setLoanAmount(request.getLoanAmount());
            setPurposeOfLoan(request.getPurposeOfLoan());
            setTenure(request.getTenure());
            setNumberOfInstalments(request.getNumberOfInstalments());
            setEmi(request.getEmi());
            setPropertyValue(request.getPropertyValue());
            setSecurityAmount(request.getSecurityAmount());
            setDownPaymentCollateral(request.getDownPaymentCollateral());

            // 3. Income Profile
            setEmploymentType(request.getEmploymentType());
            setAnnualIncome(request.getAnnualIncome());
            setDesignation(request.getDesignation());
            setEmployerBusinessName(request.getEmployerBusinessName());
            setLocation(request.getLocation());
            setState(request.getState());
            setTakeHomePay(request.getTakeHomePay());
            setDeductionsOrEmisPayable(request.getDeductionsOrEmisPayable());
            setBankName(request.getBankName());
            setPrimaryBankAccount(request.getPrimaryBankAccount());
            setAccountStatementConsent(request.getAccountStatementConsent());
            setCibilLiabilityCheck(request.getCibilLiabilityCheck());
            setDebtToIncomeRatio(request.getDebtToIncomeRatio());
            setLoanToValueRatio(request.getLoanToValueRatio());
            setDebtServiceCoverageRatio(request.getDebtServiceCoverageRatio());
            setNetDisposableIncome(request.getNetDisposableIncome());

            // 4. Referral Details
            setSourcingChannel(request.getSourcingChannel());
            setReferralDate(request.getReferralDate());
            setLspPartnerCode(request.getLspPartnerCode());
            setAgentPartnerName(request.getAgentPartnerName());
            setSourcingEmployeeId(request.getSourcingEmployeeId());
            setSourcingEmployeeName(request.getSourcingEmployeeName());
        }
    }

    @Override
    public String toString() {
        return "Lead{" +
                "leadId='" + getLeadId() + '\'' +
                ", customerName='" + getFirstNameBusinessName() + '\'' +
                ", mobileNumber='" + getMobileNumber() + '\'' +
                ", panNumber='" + getPanNumber() + '\'' +
                ", loanProductType='" + getLoanProductType() + '\'' +
                ", loanAmount=" + getLoanAmount() +
                ", sourcingChannel='" + getSourcingChannel() + '\'' +
                ", leadStatus='" + getLeadStatus() + '\'' +
                ", assignedEmployeeId='" + getAssignedEmployeeId() + '\'' +
                '}';
    }
}
