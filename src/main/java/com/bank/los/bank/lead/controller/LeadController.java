package com.bank.los.bank.lead.controller;

import com.bank.los.bank.lead.dto.LeadAssignRequest;
import com.bank.los.bank.lead.dto.LeadAssignResponse;
import com.bank.los.bank.lead.dto.LeadRequest;
import com.bank.los.bank.lead.entity.Lead;
import com.bank.los.bank.lead.exception.ValidationException;
import com.bank.los.bank.lead.model.CsvImportResult;
import com.bank.los.bank.lead.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * REST Controller exposing the Lead management API.
 * Maps both /api/leads and /api/v1/leads to support seamless backward compatibility.
 */
@RestController
@RequestMapping({"/api/leads", "/api/v1/leads"})
@Tag(name = "Lead Management", description = "Endpoints for Lead Capture, Multi-Step Origination, Assignment, and Bulk Import/Export")
@SecurityRequirement(name = "BearerAuth")
public class LeadController {

    private final LeadService leadService;

    public LeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    /**
     * Single REST API endpoint to create a Lead.
     * Collects all fields from multi-step lead forms in one request DTO,
     * validates, duplicate-checks, and saves within a single transaction.
     * Automatically sets leadStatus = NEW.
     *
     * @param request unified LeadRequest payload containing customer, employment, loan, and sourcing fields
     * @return 201 Created with the saved Lead object containing generated leadId
     */
    @PostMapping
    @Operation(summary = "Create a new Lead across 4 sections (Personal, Loan, Income, Referral)")
    public ResponseEntity<Lead> createLead(@RequestBody LeadRequest request) {
        Lead savedLead = leadService.createLead(request);
        return new ResponseEntity<>(savedLead, HttpStatus.CREATED);
    }

    /**
     * Updates an existing lead's details (customer details, employment details,
     * loan details, sourcing channel, etc.). System-managed fields cannot be changed.
     *
     * @param leadId Lead identifier
     * @param request payload with updated fields
     * @return 200 OK with updated Lead
     */
    @PutMapping("/{leadId}")
    @Operation(summary = "Update an existing Lead (editable fields only)")
    public ResponseEntity<Lead> updateLead(@PathVariable String leadId, @RequestBody LeadRequest request) {
        Lead updatedLead = leadService.updateLead(leadId, request);
        return ResponseEntity.ok(updatedLead);
    }

    /**
     * Assigns a lead to an active bank user with MAKER role.
     * Updates assignedEmployeeId, sets leadStatus = ASSIGNED, stores timestamp and assignedBy.
     *
     * @param leadId Lead identifier
     * @param request JSON payload containing employeeId
     * @return 200 OK with LeadAssignResponse
     */
    @PutMapping("/{leadId}/assign")
    @Operation(summary = "Assign a Lead to an active Maker user by Admin")
    public ResponseEntity<LeadAssignResponse> assignLead(
            @PathVariable String leadId,
            @RequestBody(required = false) LeadAssignRequest request) {
        LeadAssignResponse response = leadService.assignLead(leadId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve a lead by Lead ID.
     *
     * @param leadId the Lead ID
     * @return 200 OK with Lead or 404 Not Found
     */
    @GetMapping("/{leadId}")
    @Operation(summary = "Get Lead details by ID")
    public ResponseEntity<Lead> getLeadById(@PathVariable String leadId) {
        return leadService.getLeadById(leadId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    /**
     * Retrieve all leads.
     *
     * @return 200 OK with list of leads
     */
    @GetMapping
    @Operation(summary = "Get all leads")
    public ResponseEntity<List<Lead>> getAllLeads() {
        return ResponseEntity.ok(leadService.getAllLeads());
    }

    /**
     * Preview the next auto-generated Lead ID.
     *
     * @return 200 OK with next Lead ID string
     */
    @GetMapping("/next-id")
    @Operation(summary = "Preview the next auto-generated Lead ID (LDYYYYMM####)")
    public ResponseEntity<String> getNextLeadId() {
        return ResponseEntity.ok(leadService.generateNextLeadId());
    }

    /**
     * Import multiple leads in bulk from a CSV file.
     * Accepts multipart/form-data ("file" parameter) or direct raw CSV stream.
     *
     * @param file optional multipart CSV file
     * @param request HTTP request for stream fallback
     * @return 200 OK with CsvImportResult summary
     */
    @PostMapping(value = "/import", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, "text/csv", MediaType.APPLICATION_OCTET_STREAM_VALUE})
    @Operation(summary = "Bulk import leads from CSV file (multipart or stream)")
    public ResponseEntity<CsvImportResult> importLeads(
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) throws IOException {
        InputStream inputStream;
        if (file != null) {
            if (file.isEmpty()) {
                throw new ValidationException("Uploaded CSV file is empty");
            }
            inputStream = file.getInputStream();
        } else {
            inputStream = request.getInputStream();
        }
        CsvImportResult result = leadService.importLeadsFromCsv(inputStream);
        return ResponseEntity.ok(result);
    }

    /**
     * Export all existing leads from database into an RFC-4180 CSV file.
     *
     * @return 200 OK with CSV file attachment
     */
    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export all leads to CSV attachment")
    public ResponseEntity<byte[]> exportLeads() {
        byte[] csvData = leadService.exportLeadsToCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"leads.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    /**
     * Download pre-configured sample CSV template demonstrating valid rows for bulk upload.
     *
     * @return 200 OK with sample CSV file attachment
     */
    @GetMapping(value = "/sample-csv", produces = "text/csv")
    @Operation(summary = "Download pre-configured sample CSV template for bulk import")
    public ResponseEntity<byte[]> getSampleCsv() {
        byte[] csvData = leadService.getSampleCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sample_leads.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
