package com.bank.los.bank.lead.model;

import com.bank.los.bank.lead.entity.Lead;

import java.util.ArrayList;
import java.util.List;

/**
 * Result DTO summarizing bulk CSV import operations:
 * - totalRows: Total data rows found in the CSV
 * - successCount: Number of leads successfully created and stored in the database
 * - failureCount: Number of rows that failed business validation
 * - errors: Detailed error messages specifying the failed row numbers and causes
 * - importedLeads: List of newly persisted Lead entities
 * - message: Human-readable summary message
 */
public class CsvImportResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private List<Lead> importedLeads = new ArrayList<>();
    private List<String> errors = new ArrayList<>();
    private String message;

    public CsvImportResult() {
    }

    public CsvImportResult(int totalRows, int successCount, int failureCount,
                           List<Lead> importedLeads, List<String> errors, String message) {
        this.totalRows = totalRows;
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.importedLeads = importedLeads != null ? importedLeads : new ArrayList<>();
        this.errors = errors != null ? errors : new ArrayList<>();
        this.message = message;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public List<Lead> getImportedLeads() {
        return importedLeads;
    }

    public void setImportedLeads(List<Lead> importedLeads) {
        this.importedLeads = importedLeads;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
