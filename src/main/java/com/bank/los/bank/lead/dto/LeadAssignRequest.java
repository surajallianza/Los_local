package com.bank.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Request payload for Lead Assignment API (PUT /api/leads/{leadId}/assign).
 * Supports assigning lead to a Maker employee/bank user by an Admin user.
 */
public class LeadAssignRequest {

    @JsonProperty("employeeId")
    @JsonAlias({"employee_id", "empId", "userId", "user_id", "username", "Employee ID"})
    private String employeeId;

    @JsonProperty("assignedBy")
    @JsonAlias({"assigned_by", "assignedByEmployeeId", "assignedByUserId", "adminId", "Assigned By"})
    private String assignedBy;

    public LeadAssignRequest() {
    }

    public LeadAssignRequest(String employeeId) {
        this.employeeId = employeeId;
    }

    public LeadAssignRequest(String employeeId, String assignedBy) {
        this.employeeId = employeeId;
        this.assignedBy = assignedBy;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
