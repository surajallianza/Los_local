package com.example.demo.lead.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Robust RFC-4180 compliant CSV parser and generator for Lead bulk import/export.
 * Supports all fields across the 4-step Lead Origination flow:
 * 1. Personal Details: Customer Name, DOB, Age, Customer Type, Mobile Number, OTP, PAN Card,
 *    PAN Validation Status, Aadhaar Card, Aadhaar Validation Status, Residential Status, Gender,
 *    Marital Status, Passport No., De-Duplicate Status, Blacklist Status, Last Name, Email, Pin Code, Dependents
 * 2. Loan Details: Loan Product Type, Loan Amount, Purpose of Loan, Tenure, No. of Instalments,
 *    EMI, Property Value, Security Amount, Down Payment / Collateral
 * 3. Income Profile: Employment Type, Annual Income, Designation, Employer Name, Location, State,
 *    Take Home Pay, Deductions / EMIs Payable, Bank Name, Account Number, Account Statement Consent,
 *    CIBIL Liability Check, Debt-to-Income (DTI), Loan-to-Value (LTV), Debt Service Coverage Ratio (DSCR),
 *    Net Disposable Income (NDI)
 * 4. Referral Details: Lead Acquisition Channel, Referral Date, Sourcing Agent / Partner ID,
 *    Agent / Partner Name, Employee ID, Employee Name
 * System Fields: Lead Status, Assigned Employee, Assignment Timestamp, Assigned By
 */
public class CsvHelper {

    public static final String[] CSV_HEADERS = {
        "Lead ID",
        // 1. Personal Details
        "Name of Customer",
        "Date of Birth",
        "Age",
        "Customer Type",
        "Mobile Number",
        "OTP",
        "PAN Card",
        "PAN Validation Status",
        "Aadhaar Card",
        "Aadhaar Validation Status",
        "Residential Status",
        "Gender",
        "Marital Status",
        "Passport No.",
        "De-Duplicate Status",
        "Blacklist Status",
        "Last Name",
        "Email Address",
        "Pin Code",
        "No. of Dependents",
        // 2. Loan Details
        "Loan Product Type",
        "Loan Amount",
        "Purpose of Loan",
        "Tenure",
        "No. of Instalments",
        "EMI",
        "Property Value",
        "Security Amount",
        "Down Payment / Collateral",
        // 3. Income Profile
        "Employment Type",
        "Annual Income",
        "Designation",
        "Employer Name",
        "Location",
        "State",
        "Take Home Pay",
        "Deductions / EMIs Payable",
        "Bank Name",
        "Account Number",
        "Account Statement Consent",
        "CIBIL Liability Check",
        "Debt-to-Income (DTI)",
        "Loan-to-Value (LTV)",
        "Debt Service Coverage Ratio (DSCR)",
        "Net Disposable Income (NDI)",
        // 4. Referral Details
        "Lead Acquisition Channel",
        "Referral Date",
        "Sourcing Agent / Partner ID",
        "Agent / Partner Name",
        "Employee ID",
        "Employee Name",
        // System Fields
        "Lead Status",
        "Assigned Employee",
        "Assignment Timestamp",
        "Assigned By"
    };

    /**
     * Parses CSV reader stream into a list of row records according to RFC-4180 rules.
     */
    public static List<List<String>> parseCsv(Reader reader) throws IOException {
        List<List<String>> records = new ArrayList<>();
        BufferedReader br = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);

        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        boolean firstChar = true;

        int ch;
        while ((ch = br.read()) != -1) {
            // Strip UTF-8 Byte Order Mark (\uFEFF) if present at start of stream
            if (firstChar) {
                firstChar = false;
                if (ch == '\uFEFF') {
                    continue;
                }
            }

            if (inQuotes) {
                if (ch == '"') {
                    br.mark(1);
                    int next = br.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        br.reset();
                    }
                } else {
                    currentField.append((char) ch);
                }
            } else {
                switch (ch) {
                    case '"' -> inQuotes = true;
                    case ',' -> {
                        currentRecord.add(currentField.toString().trim());
                        currentField.setLength(0);
                    }
                    case '\r' -> {
                        br.mark(1);
                        int next = br.read();
                        if (next != '\n') {
                            br.reset();
                        }
                        currentRecord.add(currentField.toString().trim());
                        currentField.setLength(0);
                        if (!isRecordEmpty(currentRecord)) {
                            records.add(currentRecord);
                        }
                        currentRecord = new ArrayList<>();
                    }
                    case '\n' -> {
                        currentRecord.add(currentField.toString().trim());
                        currentField.setLength(0);
                        if (!isRecordEmpty(currentRecord)) {
                            records.add(currentRecord);
                        }
                        currentRecord = new ArrayList<>();
                    }
                    default -> currentField.append((char) ch);
                }
            }
        }

        // Add trailing field and record if present
        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString().trim());
            if (!isRecordEmpty(currentRecord)) {
                records.add(currentRecord);
            }
        }

        return records;
    }

    private static boolean isRecordEmpty(List<String> record) {
        if (record == null || record.isEmpty()) {
            return true;
        }
        for (String field : record) {
            if (field != null && !field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Escapes CSV field value if it contains comma, quotes, or newline characters.
     */
    public static String escapeCsvField(String value) {
        if (value == null) {
            return "";
        }
        boolean containsSpecial = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        if (containsSpecial) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Formats a list of fields into an RFC-4180 CSV line terminated with CRLF.
     */
    public static String toCsvLine(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(escapeCsvField(fields.get(i)));
        }
        sb.append("\r\n");
        return sb.toString();
    }

    /**
     * Maps CSV column header positions to Lead entity fields dynamically.
     */
    public static class CsvColumnIndices {
        public int leadIdCol = -1;

        // 1. Personal Details
        public int firstNameBusinessNameCol = -1;
        public int dobCol = -1;
        public int ageCol = -1;
        public int userCategoryCol = -1; // customerType
        public int mobileNumberCol = -1;
        public int otpCol = -1;
        public int panNumberCol = -1;
        public int panValidationStatusCol = -1;
        public int aadhaarNumberCol = -1;
        public int aadhaarValidationStatusCol = -1;
        public int residentialStatusCol = -1;
        public int genderCol = -1;
        public int maritalStatusCol = -1;
        public int passportNumberCol = -1;
        public int dedupeStatusCol = -1;
        public int blacklistStatusCol = -1;
        public int lastNameCol = -1;
        public int emailAddressCol = -1;
        public int pinCodeCol = -1;
        public int numberOfDependentsCol = -1;

        // 2. Loan Details
        public int loanProductTypeCol = -1;
        public int loanAmountCol = -1;
        public int purposeOfLoanCol = -1;
        public int tenureCol = -1;
        public int numberOfInstalmentsCol = -1;
        public int emiCol = -1;
        public int propertyValueCol = -1;
        public int securityAmountCol = -1;
        public int downPaymentCollateralCol = -1;

        // 3. Income Profile
        public int employmentTypeCol = -1;
        public int annualIncomeCol = -1;
        public int designationCol = -1;
        public int employerBusinessNameCol = -1;
        public int locationCol = -1;
        public int stateCol = -1;
        public int takeHomePayCol = -1;
        public int deductionsOrEmisPayableCol = -1;
        public int bankNameCol = -1;
        public int primaryBankAccountCol = -1;
        public int accountStatementConsentCol = -1;
        public int cibilLiabilityCheckCol = -1;
        public int debtToIncomeRatioCol = -1;
        public int loanToValueRatioCol = -1;
        public int debtServiceCoverageRatioCol = -1;
        public int netDisposableIncomeCol = -1;

        // 4. Referral Details
        public int sourcingChannelCol = -1;
        public int referralDateCol = -1;
        public int lspPartnerCodeCol = -1;
        public int agentPartnerNameCol = -1;
        public int sourcingEmployeeIdCol = -1;
        public int sourcingEmployeeNameCol = -1;

        // System Fields
        public int leadStatusCol = -1;
        public int assignedEmployeeIdCol = -1;
        public int assignmentTimestampCol = -1;
        public int assignedByCol = -1;

        public boolean isMissingRequiredHeaders() {
            return (sourcingChannelCol == -1 && referralDateCol == -1 && lspPartnerCodeCol == -1)
                    || firstNameBusinessNameCol == -1
                    || mobileNumberCol == -1;
        }

        public List<String> getMissingRequiredHeaders() {
            List<String> missing = new ArrayList<>();
            if (sourcingChannelCol == -1) {
                missing.add("Lead Acquisition Channel / Sourcing Channel");
            }
            if (firstNameBusinessNameCol == -1) {
                missing.add("Name of Customer / First Name");
            }
            if (mobileNumberCol == -1) {
                missing.add("Mobile Number");
            }
            return missing;
        }
    }

    public static CsvColumnIndices resolveIndices(List<String> headers) {
        CsvColumnIndices indices = new CsvColumnIndices();
        for (int i = 0; i < headers.size(); i++) {
            String raw = headers.get(i);
            if (raw == null) {
                continue;
            }
            String norm = raw.toLowerCase().replaceAll("[^a-z0-9]", "");

            if (norm.equals("leadid") || norm.equals("id")) {
                indices.leadIdCol = i;
            } else if (norm.contains("channel") || norm.contains("sourcingchannel") || norm.contains("leadacquisitionchannel") || norm.equals("channel")) {
                indices.sourcingChannelCol = i;
            } else if (norm.equals("referraldate") || norm.equals("sourcingdate") || norm.equals("date")) {
                indices.referralDateCol = i;
            } else if (norm.contains("lsp") || norm.contains("partnercode") || norm.contains("sourcingagentpartnerid") || norm.equals("partnerid")) {
                indices.lspPartnerCodeCol = i;
            } else if (norm.contains("agentname") || norm.contains("partnername") || norm.contains("agentpartnername")) {
                indices.agentPartnerNameCol = i;
            } else if (norm.contains("sourcingemployeeid") || norm.contains("referralemployeeid")) {
                indices.sourcingEmployeeIdCol = i;
            } else if (norm.contains("sourcingemployeename") || norm.contains("referralemployeename")) {
                indices.sourcingEmployeeNameCol = i;
            } else if (norm.equals("customertype") || norm.equals("usercategory") || norm.equals("category")) {
                indices.userCategoryCol = i;
            } else if (norm.contains("employer") || norm.contains("company")) {
                indices.employerBusinessNameCol = i;
            } else if (norm.contains("nameofcustomer") || norm.contains("customername") || norm.contains("firstname") || norm.equals("name") || (norm.contains("businessname") && !norm.contains("employer"))) {
                indices.firstNameBusinessNameCol = i;
            } else if (norm.equals("lastname") || norm.equals("surname")) {
                indices.lastNameCol = i;
            } else if (norm.contains("mobile") || norm.contains("phone")) {
                indices.mobileNumberCol = i;
            } else if (norm.contains("email")) {
                indices.emailAddressCol = i;
            } else if (norm.contains("pin") || norm.contains("postal") || norm.contains("zip")) {
                indices.pinCodeCol = i;
            } else if (norm.equals("dob") || norm.contains("dateofbirth") || norm.contains("birthdate")) {
                indices.dobCol = i;
            } else if (norm.equals("age") || norm.contains("applicantage")) {
                indices.ageCol = i;
            } else if (norm.equals("otp") || norm.contains("otpcode") || norm.contains("otpnumber")) {
                indices.otpCol = i;
            } else if (norm.contains("panvalidate") || norm.contains("panvalidation")) {
                indices.panValidationStatusCol = i;
            } else if (norm.contains("pan") || norm.contains("pancard")) {
                indices.panNumberCol = i;
            } else if (norm.contains("aadhaarvalidate") || norm.contains("aadhaarvalidation") || norm.contains("aadharvalidate")) {
                indices.aadhaarValidationStatusCol = i;
            } else if (norm.contains("aadhaar") || norm.contains("aadhar") || norm.contains("aadhaarcard")) {
                indices.aadhaarNumberCol = i;
            } else if (norm.contains("residential") || norm.contains("residence")) {
                indices.residentialStatusCol = i;
            } else if (norm.equals("gender") || norm.equals("sex")) {
                indices.genderCol = i;
            } else if (norm.contains("marital") || norm.contains("marriage")) {
                indices.maritalStatusCol = i;
            } else if (norm.contains("passport")) {
                indices.passportNumberCol = i;
            } else if (norm.contains("dedupe") || norm.contains("deduplicate")) {
                indices.dedupeStatusCol = i;
            } else if (norm.contains("blacklist")) {
                indices.blacklistStatusCol = i;
            } else if (norm.contains("dependent") || norm.contains("dependents")) {
                indices.numberOfDependentsCol = i;
            } else if (norm.contains("employmenttype") || norm.equals("employment")) {
                indices.employmentTypeCol = i;
            } else if (norm.contains("annualincome") || norm.equals("income")) {
                indices.annualIncomeCol = i;
            } else if (norm.contains("designation") || norm.contains("title") || norm.contains("jobtitle")) {
                indices.designationCol = i;
            } else if (norm.equals("location") || norm.equals("city")) {
                indices.locationCol = i;
            } else if (norm.equals("state")) {
                indices.stateCol = i;
            } else if (norm.contains("takehome") || norm.contains("takehomepay")) {
                indices.takeHomePayCol = i;
            } else if (norm.contains("deduction") || norm.contains("deductions") || norm.contains("emispayable")) {
                indices.deductionsOrEmisPayableCol = i;
            } else if (norm.contains("bankname") || norm.equals("bank")) {
                indices.bankNameCol = i;
            } else if (norm.contains("bankaccount") || norm.contains("accountnumber") || norm.contains("accountno")) {
                indices.primaryBankAccountCol = i;
            } else if (norm.contains("accountstatement") || norm.contains("statementconsent")) {
                indices.accountStatementConsentCol = i;
            } else if (norm.contains("cibil")) {
                indices.cibilLiabilityCheckCol = i;
            } else if (norm.contains("debttoincome") || norm.equals("dti")) {
                indices.debtToIncomeRatioCol = i;
            } else if (norm.contains("loantovalue") || norm.equals("ltv")) {
                indices.loanToValueRatioCol = i;
            } else if (norm.contains("debtservice") || norm.equals("dscr")) {
                indices.debtServiceCoverageRatioCol = i;
            } else if (norm.contains("disposable") || norm.equals("ndi")) {
                indices.netDisposableIncomeCol = i;
            } else if (norm.contains("producttype") || norm.contains("loantype") || norm.contains("loanproduct")) {
                indices.loanProductTypeCol = i;
            } else if (norm.contains("loanamount") || norm.equals("amount")) {
                indices.loanAmountCol = i;
            } else if (norm.contains("purpose") || norm.contains("loanpurpose")) {
                indices.purposeOfLoanCol = i;
            } else if (norm.equals("tenure") || norm.contains("tenuremonths") || norm.contains("loantenure")) {
                indices.tenureCol = i;
            } else if (norm.contains("instalment") || norm.contains("installment")) {
                indices.numberOfInstalmentsCol = i;
            } else if (norm.equals("emi") || norm.contains("monthlyemi")) {
                indices.emiCol = i;
            } else if (norm.contains("propertyvalue")) {
                indices.propertyValueCol = i;
            } else if (norm.contains("securityamount")) {
                indices.securityAmountCol = i;
            } else if (norm.contains("downpayment") || norm.contains("collateral")) {
                indices.downPaymentCollateralCol = i;
            } else if (norm.equals("leadstatus") || norm.equals("status")) {
                indices.leadStatusCol = i;
            } else if (norm.contains("assignmenttimestamp") || norm.contains("assignmentdate") || (norm.contains("assignment") && norm.contains("time"))) {
                indices.assignmentTimestampCol = i;
            } else if (norm.contains("assignedemployee") || (norm.contains("employee") && norm.contains("assign"))) {
                indices.assignedEmployeeIdCol = i;
            } else if (norm.contains("assignedby")) {
                indices.assignedByCol = i;
            } else if (norm.equals("employeeid")) {
                // If assignedEmployeeIdCol is already set, this is sourcingEmployeeId, otherwise check
                if (indices.assignedEmployeeIdCol == -1) {
                    indices.assignedEmployeeIdCol = i;
                } else {
                    indices.sourcingEmployeeIdCol = i;
                }
            } else if (norm.equals("employeename")) {
                indices.sourcingEmployeeNameCol = i;
            }
        }
        return indices;
    }

    /**
     * Pre-built standard sample CSV string with diverse valid rows demonstrating all 4-section fields.
     */
    public static String generateSampleCsvContent() {
        return """
                Lead ID,Name of Customer,Date of Birth,Age,Customer Type,Mobile Number,OTP,PAN Card,PAN Validation Status,Aadhaar Card,Aadhaar Validation Status,Residential Status,Gender,Marital Status,Passport No.,De-Duplicate Status,Blacklist Status,Last Name,Email Address,Pin Code,No. of Dependents,Loan Product Type,Loan Amount,Purpose of Loan,Tenure,No. of Instalments,EMI,Property Value,Security Amount,Down Payment / Collateral,Employment Type,Annual Income,Designation,Employer Name,Location,State,Take Home Pay,Deductions / EMIs Payable,Bank Name,Account Number,Account Statement Consent,CIBIL Liability Check,Debt-to-Income (DTI),Loan-to-Value (LTV),Debt Service Coverage Ratio (DSCR),Net Disposable Income (NDI),Lead Acquisition Channel,Referral Date,Sourcing Agent / Partner ID,Agent / Partner Name,Employee ID,Employee Name,Lead Status,Assigned Employee,Assignment Timestamp,Assigned By\r
                ,Amit Sharma,1990-05-15,36,Individual,9876543210,654321,ABCDE1234F,VALIDATED,123456789012,VALIDATED,Resident Indian,Male,Married,Z1234567,PASSED,CLEARED,Sharma,amit.sharma@example.com,400001,2,Home Loan,5000000,Apartment Purchase,240,240,43391,7000000,500000,1000000 Down Payment,Salaried,1200000,Senior Architect,Tata Consultancy Services,Mumbai,Maharashtra,85000,12000,HDFC Bank,50100234567890,true,780 - Clear,35.5,71.4,2.8,42000,Branch Office,2026-10-01,LSP-1001,Karan Kapoor,EMP102,Rahul Verma,NEW,,,\r
                ,Priya Verma,1993-08-20,33,Individual,9812345678,123456,BCDEF2345G,VALIDATED,234567890123,VALIDATED,Resident Indian,Female,Single,,PASSED,CLEARED,Verma,priya.verma@example.com,400076,0,Personal Loan,800000,Home Renovation,36,36,26388,0,0,,Salaried,1500000,Software Lead,Infosys Technologies,Pune,Maharashtra,105000,0,ICICI Bank,001122334455,true,755 - Clear,25.1,0.0,3.9,78612,Website,2026-10-02,,,EMP105,Pooja Mehta,NEW,,,\r
                ,Apex Technologies,1985-03-10,41,Non-Individual,9823456789,789123,CDEFG3456H,VALIDATED,345678901234,VALIDATED,Resident Indian,Male,Married,,PASSED,CLEARED,,contact@apextech.com,560001,1,Business Loan,2500000,Working Capital,60,60,56885,4000000,200000,Commercial Property,Self-Employed Business,3500000,Managing Director,Apex Technologies,Bengaluru,Karnataka,220000,25000,State Bank of India,334455667788,true,810 - Clear,37.2,62.5,2.7,138115,Mobile App,2026-10-03,,,EMP102,Rahul Verma,NEW,,,\r
                LD2026099901,Rajesh Patel,1988-11-12,38,Individual,9845678901,789012,EFGHI5678J,VALIDATED,456789012345,VALIDATED,Resident Indian,Male,Married,M9876543,PASSED,CLEARED,Patel,rajesh.patel@example.com,411001,1,Education Loan,1200000,Child Higher Education,84,84,19688,0,0,,Salaried,2000000,VP Operations,Reliance Industries,Mumbai,Maharashtra,140000,15000,Kotak Mahindra Bank,123456789012,true,760 - Clear,24.7,0.0,4.0,105312,Branch Office,2026-09-30,LSP-3002,Pooja Sharma,EMP102,Rahul Verma,ASSIGNED,EMP102,2026-09-30T10:00:00,EMP101\r
                """;
    }
}
