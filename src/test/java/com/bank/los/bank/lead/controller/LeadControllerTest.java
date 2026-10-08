package com.bank.los.bank.lead.controller;

import com.bank.los.bank.lead.dto.LeadRequest;
import com.bank.los.bank.lead.entity.Lead;
import com.bank.los.bank.lead.model.CsvImportResult;
import com.bank.los.bank.lead.service.LeadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LeadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private LeadService leadService;

    @InjectMocks
    private LeadController leadController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(leadController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    @DisplayName("POST /api/v1/leads: Should create Lead with passportExpiryDate, interestRate, totalInterest")
    void testCreateLead() throws Exception {
        Lead lead = new Lead();
        lead.setLeadId("LD2026100001");
        lead.setFirstNameBusinessName("Anita Desai");
        lead.setPassportExpiryDate("2033-05-15");
        lead.setInterestRate(8.75);
        lead.setTotalInterest(450000.0);
        lead.setLeadStatus("NEW");

        when(leadService.createLead(any(LeadRequest.class))).thenReturn(lead);

        LeadRequest request = new LeadRequest();
        request.setCustomerName("Anita Desai");
        request.setPassportExpiryDate("2033-05-15");
        request.setInterestRate(8.75);
        request.setTotalInterest(450000.0);
        request.setSourcingChannel("Website");

        mockMvc.perform(post("/api/v1/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.leadId").value("LD2026100001"))
                .andExpect(jsonPath("$.customerName").value("Anita Desai"))
                .andExpect(jsonPath("$.passportExpiryDate").value("2033-05-15"))
                .andExpect(jsonPath("$.interestRate").value(8.75))
                .andExpect(jsonPath("$.totalInterest").value(450000.0))
                .andExpect(jsonPath("$.leadStatus").value("NEW"));
    }

    @Test
    @DisplayName("GET /api/v1/leads/{id}: Should return lead details")
    void testGetLeadById() throws Exception {
        Lead lead = new Lead();
        lead.setLeadId("LD2026100001");
        lead.setFirstNameBusinessName("Anita Desai");
        lead.setPassportExpiryDate("2033-05-15");
        lead.setInterestRate(8.75);
        lead.setTotalInterest(450000.0);

        when(leadService.getLeadById("LD2026100001")).thenReturn(Optional.of(lead));

        mockMvc.perform(get("/api/v1/leads/LD2026100001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadId").value("LD2026100001"))
                .andExpect(jsonPath("$.passportExpiryDate").value("2033-05-15"))
                .andExpect(jsonPath("$.interestRate").value(8.75))
                .andExpect(jsonPath("$.totalInterest").value(450000.0));
    }

    @Test
    @DisplayName("PUT /api/v1/leads/{id}: Should update lead details")
    void testUpdateLead() throws Exception {
        Lead lead = new Lead();
        lead.setLeadId("LD2026100001");
        lead.setFirstNameBusinessName("Anita Desai");
        lead.setPassportExpiryDate("2035-12-31");
        lead.setInterestRate(9.0);
        lead.setTotalInterest(500000.0);

        when(leadService.updateLead(eq("LD2026100001"), any(LeadRequest.class))).thenReturn(lead);

        LeadRequest updateRequest = new LeadRequest();
        updateRequest.setPassportExpiryDate("2035-12-31");
        updateRequest.setInterestRate(9.0);
        updateRequest.setTotalInterest(500000.0);

        mockMvc.perform(put("/api/v1/leads/LD2026100001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passportExpiryDate").value("2035-12-31"))
                .andExpect(jsonPath("$.interestRate").value(9.0))
                .andExpect(jsonPath("$.totalInterest").value(500000.0));
    }

    @Test
    @DisplayName("GET /api/v1/leads/export: Should export CSV bytes")
    void testExportLeads() throws Exception {
        byte[] csvBytes = "Lead ID,Name of Customer,Date of Birth,Age,Customer Type,PAN Card,PAN Validation Status,Aadhaar Card,Aadhaar Validation Status,Residential Status,Gender,Marital Status,Passport No.,Passport Expiry Date,De-Duplicate Status,Blacklist Status,Last Name,Email Address,Pin Code,No. of Dependents,Loan Product Type,Loan Amount,Purpose of Loan,Tenure,No. of Instalments,EMI,Interest Rate,Total Interest,Security Amount,Down Payment / Collateral,Employment Type,Annual Income,Designation,Employer Name,Location,State,Take Home Pay,Deductions / EMIs Payable,Bank Name,Account Number,Account Statement Consent,CIBIL Liability Check,Debt-to-Income (DTI),Loan-to-Value (LTV),Debt Service Coverage Ratio (DSCR),Net Disposable Income (NDI),Lead Acquisition Channel,Referral Date,Sourcing Agent / Partner ID,Agent / Partner Name,Employee ID,Employee Name,Lead Status,Assigned Employee,Assignment Timestamp,Assigned By\n".getBytes();
        when(leadService.exportLeadsToCsv()).thenReturn(csvBytes);

        mockMvc.perform(get("/api/v1/leads/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"leads.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    @DisplayName("POST /api/v1/leads/import: Should accept multipart CSV file")
    void testImportLeads() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "leads.csv", "text/csv", "sample,data".getBytes());
        CsvImportResult result = new CsvImportResult(1, 1, 0, List.of(), List.of(), "Processed 1 rows: 1 successfully imported, 0 failed.");

        when(leadService.importLeadsFromCsv(any(InputStream.class))).thenReturn(result);

        mockMvc.perform(multipart("/api/v1/leads/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));
    }
}
