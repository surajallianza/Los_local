package com.bank.los.bank.lead.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Success response payload for Lead Assignment API.
 * Matching:
 * {
 *   "message": "Lead assigned successfully",
 *   "leadId": "LD2026090001",
 *   "assignedEmployeeId": "EMP102",
 *   "leadStatus": "ASSIGNED",
 *   "assignedBy": "EMP101"
 * }
 */
public class LeadAssignResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("leadId")
    private String leadId;

    @JsonProperty("assignedEmployeeId")
    @JsonAlias({"assignedUserId", "assignedToUserId"})
    private String assignedEmployeeId;

    @JsonProperty("leadStatus")
    private String leadStatus;

    @JsonProperty("assignedBy")
    private String assignedBy;

    public LeadAssignResponse() {
    }

    public LeadAssignResponse(String message, String leadId, String assignedEmployeeId, String leadStatus) {
        this.message = message;
        this.leadId = leadId;
        this.assignedEmployeeId = assignedEmployeeId;
        this.leadStatus = leadStatus;
    }

    public LeadAssignResponse(String message, String leadId, String assignedEmployeeId, String leadStatus, String assignedBy) {
        this.message = message;
        this.leadId = leadId;
        this.assignedEmployeeId = assignedEmployeeId;
        this.leadStatus = leadStatus;
        this.assignedBy = assignedBy;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getLeadId() {
        return leadId;
    }

    public void setLeadId(String leadId) {
        this.leadId = leadId;
    }

    public String getAssignedEmployeeId() {
        return assignedEmployeeId;
    }

    public void setAssignedEmployeeId(String assignedEmployeeId) {
        this.assignedEmployeeId = assignedEmployeeId;
    }

    public String getLeadStatus() {
        return leadStatus;
    }

    public void setLeadStatus(String leadStatus) {
        this.leadStatus = leadStatus;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }
}
