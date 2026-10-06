package com.bank.los.administration.organization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response representing an onboarded Bank or NBFC Organization")
public class OrganizationResponse {

    @JsonProperty("id")
    @Schema(description = "Institution UUID", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @JsonProperty("pkid")
    @Schema(description = "Internal sequential ID", example = "1")
    private Long pkid;

    @JsonProperty("bank_code")
    @Schema(description = "Bank Code", example = "SBI01")
    private String bankCode;

    @JsonProperty("bank_name")
    @Schema(description = "Bank Name", example = "State Bank of India")
    private String bankName;

    @JsonProperty("legal_name")
    @Schema(description = "Legal Business Name", example = "State Bank of India Limited")
    private String legalName;

    @JsonProperty("bank_type")
    @Schema(description = "Bank Type", example = "COMMERCIAL_BANK")
    private String bankType;

    @JsonProperty("license_number")
    @Schema(description = "Banking License Number", example = "LIC-MH-2024-1001")
    private String licenseNumber;

    @JsonProperty("registration_number")
    @Schema(description = "Registration Number (alias of license_number)", example = "LIC-MH-2024-1001")
    private String registrationNumber;

    @JsonProperty("PAN")
    @Schema(description = "10-digit PAN", example = "AAACS1234F")
    private String pan;

    @JsonProperty("gst_number")
    @Schema(description = "15-digit GSTIN", example = "27AAACS1234F1Z5")
    private String gstNumber;

    @JsonProperty("gst_no")
    @Schema(description = "GST Number alias", example = "27AAACS1234F1Z5")
    private String gstNo;

    @JsonProperty("CIN")
    @Schema(description = "Corporate Identification Number (CIN)", example = "L65190MH2004GOI148838")
    private String cin;

    // Regulatory & Banking clearing details
    @JsonProperty("direct_clearing_number")
    @Schema(description = "Direct Clearing Number or Code", example = "CLR-SBI-01")
    private String directClearingNumber;

    @JsonProperty("direct_clearing_member")
    @Schema(description = "Direct Clearing Member Flag", example = "true")
    private Boolean directClearingMember;

    @JsonProperty("direct_member_iftas")
    @Schema(description = "Direct Member of IFTAS Flag", example = "true")
    private Boolean directMemberIftas;

    @JsonProperty("micr_details")
    @Schema(description = "MICR Code / Details", example = "400002001")
    private String micrCode;

    @JsonProperty("micr_city_code")
    @Schema(description = "MICR City Code", example = "400")
    private String micrCityCode;

    @JsonProperty("micr_bank_code")
    @Schema(description = "MICR Bank Code", example = "002")
    private String micrBankCode;

    @JsonProperty("micr_branch_code")
    @Schema(description = "MICR Branch Code", example = "001")
    private String micrBranchCode;

    @JsonProperty("ifsc_code")
    @Schema(description = "IFSC Code", example = "SBIN0000001")
    private String ifscCode;

    @JsonProperty("number_of_branches")
    @Schema(description = "Total Number of Branches", example = "22000")
    private Integer numberOfBranches;

    @JsonProperty("sponsor_bank_of_clearing")
    @Schema(description = "Sponsor Bank for Clearing", example = "Reserve Bank of India")
    private String sponsorBankForClearing;

    @JsonProperty("sponsor_bank_of_iftas")
    @Schema(description = "Sponsor Bank for IFTAS", example = "State Bank of India")
    private String sponsorBankForIftas;

    // Address Details
    @JsonProperty("address_type")
    @Schema(description = "Address Type", example = "HEAD_OFFICE")
    private String addressType;

    @JsonProperty("unit_gala_name_and_number")
    @Schema(description = "Unit / Gala / Building Name and Number", example = "State Bank Bhavan, 4th Floor")
    private String unitGalaNameNumber;

    @JsonProperty("street_road")
    @Schema(description = "Street or Road", example = "Madame Cama Road")
    private String streetRoad;

    @JsonProperty("landmark")
    @Schema(description = "Landmark", example = "Nariman Point")
    private String landmark;

    @JsonProperty("city")
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @JsonProperty("state")
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @JsonProperty("pincode")
    @Schema(description = "PIN Code", example = "400021")
    private String pincode;

    @JsonProperty("website")
    @Schema(description = "Website URL", example = "https://www.sbi.co.in")
    private String website;

    @JsonProperty("logo")
    @Schema(description = "Logo URL or base64 asset identifier", example = "https://assets.bank.com/logos/sbi.png")
    private String logo;

    @JsonProperty("regulatory_authority_id")
    @Schema(description = "Regulatory Authority UUID", example = "b5a76e2d-3c9f-4321-9e87-654321fedcba")
    private UUID regulatoryAuthorityId;

    @JsonProperty("regulatory_status")
    @Schema(description = "Regulatory Status", example = "ACTIVE")
    private String regulatoryStatus;

    @JsonProperty("country")
    @Schema(description = "Country", example = "India")
    private String country;

    @JsonProperty("status")
    @Schema(description = "Operational Status", example = "ACTIVE")
    private String status;

    @JsonProperty("contact_email")
    @Schema(description = "Contact Email", example = "compliance@sbi.co.in")
    private String contactEmail;

    @JsonProperty("contact_phone")
    @Schema(description = "Contact Phone", example = "+912222740000")
    private String contactPhone;

    @JsonProperty("db_name")
    @Schema(description = "Assigned Database Name", example = "los_sbi01_db")
    private String dbName;

    @JsonProperty("db_host")
    @Schema(description = "Database Host", example = "localhost")
    private String dbHost;

    @JsonProperty("db_port")
    @Schema(description = "Database Port", example = "5432")
    private Integer dbPort;

    @JsonProperty("created_at")
    @Schema(description = "Creation Timestamp")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @Schema(description = "Last Updated Timestamp")
    private LocalDateTime updatedAt;

    public String getBankCode() {
        return bankCode;
    }

    public void setBankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankType() {
        return bankType;
    }

    public void setBankType(String bankType) {
        this.bankType = bankType;
    }

    // Helper getters
    public String getName() {
        return bankName != null ? bankName : legalName;
    }

    public String getType() {
        return bankType;
    }

    public static class OrganizationResponseBuilder {
        public OrganizationResponseBuilder sponsorBankOfClearing(String s) {
            this.sponsorBankForClearing = s;
            return this;
        }

        public OrganizationResponseBuilder sponsorBankOfIftas(String s) {
            this.sponsorBankForIftas = s;
            return this;
        }

        public OrganizationResponseBuilder unitGalaNameAndNumber(String s) {
            this.unitGalaNameNumber = s;
            return this;
        }

        public OrganizationResponseBuilder micrDetails(String s) {
            this.micrCode = s;
            return this;
        }
    }
}
