package com.bank.los.bank.lead.service;

import com.bank.los.bank.lead.dto.LeadRequest;
import com.bank.los.bank.lead.entity.Lead;
import com.bank.los.bank.lead.exception.ValidationException;
import com.bank.los.bank.lead.model.CsvImportResult;
import com.bank.los.bank.lead.repository.LeadRepository;
import com.bank.los.config.OrganizationContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadUserService leadUserService;

    @Spy
    private LeadValidator leadValidator = new LeadValidator();

    @InjectMocks
    private LeadService leadService;

    @BeforeEach
    void setUp() {
        OrganizationContext.setCurrentOrganization("los_sbi01_db");
    }

    @AfterEach
    void tearDown() {
        OrganizationContext.clear();
    }

    @Test
    @DisplayName("Create Lead: should succeed with passportExpiryDate, interestRate, totalInterest without mobile/otp")
    void testCreateLeadSuccess() {
        LeadRequest request = new LeadRequest();
        request.setCustomerName("Sunil Verma");
        request.setCustomerType("Individual");
        request.setPanNumber("ABCDE1234F");
        request.setAadhaarNumber("123456789012");
        request.setPassportNumber("P1234567");
        request.setPassportExpiryDate("2032-12-31");
        request.setLoanProductType("Home Loan");
        request.setLoanAmount(5000000.0);
        request.setTenure(240);
        request.setNumberOfInstalments(240);
        request.setEmi(45000.0);
        request.setInterestRate(8.5);
        request.setTotalInterest(5800000.0);
        request.setSourcingChannel("Website");

        when(leadRepository.findByLeadIdStartingWithOrderByLeadIdDesc(anyString())).thenReturn(List.of());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lead created = leadService.createLead(request);

        assertNotNull(created);
        assertNotNull(created.getLeadId());
        assertEquals("Sunil Verma", created.getFirstNameBusinessName());
        assertEquals("P1234567", created.getPassportNumber());
        assertEquals("2032-12-31", created.getPassportExpiryDate());
        assertEquals(8.5, created.getInterestRate());
        assertEquals(5800000.0, created.getTotalInterest());
        assertEquals("NEW", created.getLeadStatus());
        verify(leadRepository).save(any(Lead.class));
    }

    @Test
    @DisplayName("Update Lead: should update passportExpiryDate, interestRate, totalInterest")
    void testUpdateLeadSuccess() {
        Lead existing = new Lead();
        existing.setLeadId("LD2026100001");
        existing.setFirstNameBusinessName("Sunil Verma");
        existing.setSourcingChannel("Website");
        existing.setCustomerType("Individual");
        existing.setLoanAmount(5000000.0);
        existing.setTenure(240);
        existing.setNumberOfInstalments(240);
        existing.setEmi(45000.0);
        existing.setInterestRate(8.5);
        existing.setTotalInterest(5800000.0);

        when(leadRepository.findById("LD2026100001")).thenReturn(Optional.of(existing));
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadRequest update = new LeadRequest();
        update.setPassportExpiryDate("2035-06-30");
        update.setInterestRate(9.25);
        update.setTotalInterest(6200000.0);

        Lead updated = leadService.updateLead("LD2026100001", update);

        assertNotNull(updated);
        assertEquals("2035-06-30", updated.getPassportExpiryDate());
        assertEquals(9.25, updated.getInterestRate());
        assertEquals(6200000.0, updated.getTotalInterest());
        verify(leadRepository).save(existing);
    }

    @Test
    @DisplayName("Export CSV: headers should include passportExpiryDate, interestRate, totalInterest and exclude mobile, otp, propertyValue")
    void testExportLeadsToCsv() {
        Lead lead = new Lead();
        lead.setLeadId("LD2026100001");
        lead.setFirstNameBusinessName("Sunil Verma");
        lead.setPassportExpiryDate("2032-12-31");
        lead.setInterestRate(8.5);
        lead.setTotalInterest(5800000.0);
        lead.setSourcingChannel("Website");
        lead.setCustomerType("Individual");

        when(leadRepository.findAll()).thenReturn(List.of(lead));

        byte[] csvBytes = leadService.exportLeadsToCsv();
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);

        assertTrue(csvContent.contains("Passport Expiry Date"));
        assertTrue(csvContent.contains("Interest Rate"));
        assertTrue(csvContent.contains("Total Interest"));
        assertFalse(csvContent.contains("Mobile Number"));
        assertFalse(csvContent.contains("Property Value"));
        assertTrue(csvContent.contains("2032-12-31"));
        assertTrue(csvContent.contains("8.5"));
        assertTrue(csvContent.contains("5800000.0"));
    }

    @Test
    @DisplayName("Import CSV: should import leads with passportExpiryDate, interestRate, and totalInterest")
    void testImportLeadsFromCsv() {
        String csv = """
                Lead ID,Name of Customer,Date of Birth,Age,Customer Type,PAN Card,PAN Validation Status,Aadhaar Card,Aadhaar Validation Status,Residential Status,Gender,Marital Status,Passport No.,Passport Expiry Date,De-Duplicate Status,Blacklist Status,Last Name,Email Address,Pin Code,No. of Dependents,Loan Product Type,Loan Amount,Purpose of Loan,Tenure,No. of Instalments,EMI,Interest Rate,Total Interest,Security Amount,Down Payment / Collateral,Employment Type,Annual Income,Designation,Employer Name,Location,State,Take Home Pay,Deductions / EMIs Payable,Bank Name,Account Number,Account Statement Consent,CIBIL Liability Check,Debt-to-Income (DTI),Loan-to-Value (LTV),Debt Service Coverage Ratio (DSCR),Net Disposable Income (NDI),Lead Acquisition Channel,Referral Date,Sourcing Agent / Partner ID,Agent / Partner Name,Employee ID,Employee Name,Lead Status,Assigned Employee,Assignment Timestamp,Assigned By
                ,Rahul Kumar,1992-04-10,34,Individual,ABCDE1234F,VALIDATED,123456789012,VALIDATED,Resident Indian,Male,Single,Z1234567,2034-08-15,PASSED,CLEARED,Kumar,rahul@example.com,400001,0,Home Loan,3000000,Purchase,180,180,31000,8.75,2580000,0,,Salaried,1000000,Manager,TCS,Mumbai,Maharashtra,75000,0,HDFC,1234567890,true,750,30.0,60.0,3.0,45000,Website,2026-10-01,,,EMP101,Admin,NEW,,,
                """;

        when(leadRepository.findByLeadIdStartingWithOrderByLeadIdDesc(anyString())).thenReturn(List.of());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ByteArrayInputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        CsvImportResult result = leadService.importLeadsFromCsv(in);

        assertEquals(1, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        Lead imported = result.getImportedLeads().get(0);
        assertEquals("Rahul Kumar", imported.getFirstNameBusinessName());
        assertEquals("1992-04-10", imported.getDob());
        assertEquals(34, imported.getAge());
        assertEquals("ABCDE1234F", imported.getPanNumber());
        assertEquals("123456789012", imported.getAadhaarNumber());
        assertEquals("rahul@example.com", imported.getEmailAddress());
        assertEquals("400001", imported.getPinCode());
        assertEquals("TCS", imported.getEmployerName());
        assertEquals("2034-08-15", imported.getPassportExpiryDate());
        assertEquals(8.75, imported.getInterestRate());
        assertEquals(2580000.0, imported.getTotalInterest());
    }

    @Test
    @DisplayName("Complete Flow: Create -> Get -> Update -> Export -> Import works end-to-end")
    void testCompleteLeadFlowCreateGetUpdateImportExport() {
        // 1. Create
        LeadRequest createReq = new LeadRequest();
        createReq.setCustomerName("Sunil Verma");
        createReq.setCustomerType("Individual");
        createReq.setPanNumber("ABCDE1234F");
        createReq.setAadhaarNumber("123456789012");
        createReq.setPassportNumber("P1234567");
        createReq.setPassportExpiryDate("2032-12-31");
        createReq.setLoanProductType("Home Loan");
        createReq.setLoanAmount(5000000.0);
        createReq.setTenure(240);
        createReq.setNumberOfInstalments(240);
        createReq.setEmi(45000.0);
        createReq.setInterestRate(8.5);
        createReq.setTotalInterest(5800000.0);
        createReq.setSourcingChannel("Website");

        when(leadRepository.findByLeadIdStartingWithOrderByLeadIdDesc(anyString())).thenReturn(List.of());
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Lead created = leadService.createLead(createReq);
        assertNotNull(created.getLeadId());
        assertEquals("2032-12-31", created.getPassportExpiryDate());
        assertEquals(8.5, created.getInterestRate());
        assertEquals(5800000.0, created.getTotalInterest());
        assertEquals("NEW", created.getLeadStatus());

        // 2. Get
        when(leadRepository.findById(created.getLeadId())).thenReturn(Optional.of(created));
        Optional<Lead> retrieved = leadService.getLeadById(created.getLeadId());
        assertTrue(retrieved.isPresent());
        assertEquals("2032-12-31", retrieved.get().getPassportExpiryDate());
        assertEquals(8.5, retrieved.get().getInterestRate());
        assertEquals(5800000.0, retrieved.get().getTotalInterest());

        // 3. Update
        LeadRequest updateReq = new LeadRequest();
        updateReq.setPassportExpiryDate("2035-06-30");
        updateReq.setInterestRate(9.25);
        updateReq.setTotalInterest(6200000.0);

        Lead updated = leadService.updateLead(created.getLeadId(), updateReq);
        assertEquals("2035-06-30", updated.getPassportExpiryDate());
        assertEquals(9.25, updated.getInterestRate());
        assertEquals(6200000.0, updated.getTotalInterest());

        // 4. Export
        when(leadRepository.findAll()).thenReturn(List.of(updated));
        byte[] csvBytes = leadService.exportLeadsToCsv();
        String exportedCsv = new String(csvBytes, StandardCharsets.UTF_8);
        assertTrue(exportedCsv.contains("Passport Expiry Date"));
        assertTrue(exportedCsv.contains("Interest Rate"));
        assertTrue(exportedCsv.contains("Total Interest"));
        assertFalse(exportedCsv.contains("Mobile Number"));
        assertFalse(exportedCsv.contains("Property Value"));
        assertTrue(exportedCsv.contains("2035-06-30"));
        assertTrue(exportedCsv.contains("9.25"));
        assertTrue(exportedCsv.contains("6200000.0"));

        // 5. Import
        ByteArrayInputStream importIn = new ByteArrayInputStream(csvBytes);
        CsvImportResult importResult = leadService.importLeadsFromCsv(importIn);
        assertEquals(1, importResult.getTotalRows());
        assertEquals(1, importResult.getSuccessCount());
        Lead reImported = importResult.getImportedLeads().get(0);
        assertEquals("2035-06-30", reImported.getPassportExpiryDate());
        assertEquals(9.25, reImported.getInterestRate());
        assertEquals(6200000.0, reImported.getTotalInterest());
    }

    @Test
    @DisplayName("Validation: should reject invalid passportExpiryDate, interestRate > 100, and negative totalInterest")
    void testValidationErrors() {
        Lead lead1 = new Lead();
        lead1.setFirstNameBusinessName("Test User");
        lead1.setCustomerType("Individual");
        lead1.setSourcingChannel("Website");
        lead1.setPassportExpiryDate("not-a-date");

        ValidationException ex1 = assertThrows(ValidationException.class, () -> leadValidator.validateAndNormalize(lead1));
        assertTrue(ex1.getErrors().stream().anyMatch(e -> e.contains("Passport Expiry Date")));

        Lead lead2 = new Lead();
        lead2.setFirstNameBusinessName("Test User");
        lead2.setCustomerType("Individual");
        lead2.setSourcingChannel("Website");
        lead2.setInterestRate(150.0);

        ValidationException ex2 = assertThrows(ValidationException.class, () -> leadValidator.validateAndNormalize(lead2));
        assertTrue(ex2.getErrors().stream().anyMatch(e -> e.contains("Interest Rate")));

        Lead lead3 = new Lead();
        lead3.setFirstNameBusinessName("Test User");
        lead3.setCustomerType("Individual");
        lead3.setSourcingChannel("Website");
        lead3.setTotalInterest(-500.0);

        ValidationException ex3 = assertThrows(ValidationException.class, () -> leadValidator.validateAndNormalize(lead3));
        assertTrue(ex3.getErrors().stream().anyMatch(e -> e.contains("Total Interest")));
    }
}
