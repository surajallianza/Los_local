package com.bank.los.administration.organization.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for updating an existing Bank or NBFC Organization")
public class UpdateOrganizationRequest {

    @JsonProperty("bank_code")
    @JsonAlias({"bankCode", "code"})
    @Schema(description = "Bank code", example = "AXISBA01")
    private String bankCode;

    @Size(max = 150, message = "bank_name must not exceed 150 characters")
    @JsonProperty("bank_name")
    @JsonAlias({"bankName", "name"})
    @Schema(description = "Display name of the bank", example = "Axis Bank Limited")
    private String bankName;

    @Size(max = 200, message = "legal_name must not exceed 200 characters")
    @JsonProperty("legal_name")
    @JsonAlias({"legalName"})
    @Schema(description = "Registered legal entity name", example = "Axis Bank Ltd")
    private String legalName;

    @JsonProperty("bank_type")
    @JsonAlias({"bankType", "type"})
    @Schema(description = "Type of financial institution / bank", example = "COMMERCIAL_BANK")
    private String bankType;

    @JsonProperty("license_number")
    @JsonAlias({"licenseNumber", "license_no", "licenseNo", "registration_number", "registrationNumber"})
    @Schema(description = "Banking license / incorporation certificate number", example = "LIC-RBI-1993-AXIS-0207")
    private String licenseNumber;

    @JsonProperty("PAN")
    @JsonAlias({"pan", "Pan", "pan_number", "panNumber"})
    @Schema(description = "10-digit Permanent Account Number", example = "AAACA9876K")
    private String pan;

    @JsonProperty("gst_number")
    @JsonAlias({"gstNumber", "gst_no", "gstNo", "GST", "gst"})
    @Schema(description = "15-digit Goods and Services Tax Identification Number (GSTIN)", example = "27AAACA9876K1ZA")
    private String gstNumber;

    @JsonProperty("CIN")
    @JsonAlias({"cin", "Cin", "cin_number", "cinNumber"})
    @Schema(description = "Corporate Identification Number (CIN)", example = "L65110GJ1993PLC020769")
    private String cin;

    @JsonProperty("website")
    @Schema(description = "Official website URL", example = "https://www.axisbank.com")
    private String website;

    @JsonProperty("logo")
    @Schema(description = "Logo image URL or base64 asset identifier", example = "https://assets.bank.com/logos/axis.png")
    private String logo;

    @JsonProperty("regulatory_authority_id")
    @JsonAlias({"regulatoryAuthorityId"})
    @Schema(description = "UUID of the governing regulatory authority (e.g., RBI UUID)", example = "b5a76e2d-3c9f-4321-9e87-654321fedcba")
    private UUID regulatoryAuthorityId;

    @JsonProperty("regulatory_status")
    @JsonAlias({"regulatoryStatus"})
    @Schema(description = "Regulatory compliance status", example = "ACTIVE")
    private String regulatoryStatus;

    @JsonProperty("country")
    @Schema(description = "Country of jurisdiction / operation", example = "India")
    private String country;

    @JsonProperty("status")
    @Schema(description = "Operational status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE", "SUSPENDED"})
    private String status;

    @JsonProperty("contact_email")
    @JsonAlias({"contactEmail", "email"})
    @Schema(description = "Primary contact email address", example = "support@axisbank.com")
    private String contactEmail;

    @JsonProperty("contact_phone")
    @JsonAlias({"contactPhone", "phone"})
    @Schema(description = "Primary contact telephone number", example = "+912224252525")
    private String contactPhone;

    // Regulatory & Banking clearing details
    @JsonProperty("direct_clearing_number")
    @JsonAlias({"directClearingNumber", "clearing_number", "clearingNumber"})
    @Schema(description = "Direct clearing number or indicator code", example = "CLR-AXIS-01")
    private String directClearingNumber;

    @JsonProperty("direct_clearing_member")
    @JsonAlias({"directClearingMember"})
    @Schema(description = "Whether the bank is a direct clearing member", example = "true")
    private Boolean directClearingMember;

    @JsonProperty("direct_member_iftas")
    @JsonAlias({"directMemberIftas", "direct_member_of_iftas", "directMemberOfIftas"})
    @Schema(description = "Whether the bank is a direct member of IFTAS", example = "true")
    private Boolean directMemberIftas;

    @JsonProperty("micr_details")
    @JsonAlias({"micrDetails", "micr_code", "micrCode", "micr"})
    @Schema(description = "Full 9-digit MICR code or details", example = "400211002")
    private String micrCode;

    @JsonProperty("micr_city_code")
    @JsonAlias({"micrCityCode"})
    @Schema(description = "3-digit MICR city code", example = "400")
    private String micrCityCode;

    @JsonProperty("micr_bank_code")
    @JsonAlias({"micrBankCode"})
    @Schema(description = "3-digit MICR bank code", example = "211")
    private String micrBankCode;

    @JsonProperty("micr_branch_code")
    @JsonAlias({"micrBranchCode"})
    @Schema(description = "3-digit MICR branch code", example = "002")
    private String micrBranchCode;

    @JsonProperty("ifsc_code")
    @JsonAlias({"ifscCode", "ifsc", "IFSC"})
    @Schema(description = "11-character Indian Financial System Code (IFSC)", example = "UTIB0000004")
    private String ifscCode;

    @JsonProperty("number_of_branches")
    @JsonAlias({"numberOfBranches", "branches_count", "branchesCount", "totalBranches", "total_branches"})
    @Schema(description = "Total number of bank branches", example = "5377")
    private Integer numberOfBranches;

    @JsonProperty("sponsor_bank_of_clearing")
    @JsonAlias({"sponsorBankOfClearing", "sponsor_bank_for_clearing", "sponsorBankForClearing"})
    @Schema(description = "Sponsor bank for clearing operations", example = "Reserve Bank of India")
    private String sponsorBankForClearing;

    @JsonProperty("sponsor_bank_of_iftas")
    @JsonAlias({"sponsorBankOfIftas", "sponsor_bank_for_iftas", "sponsorBankForIftas"})
    @Schema(description = "Sponsor bank for IFTAS operations", example = "Axis Bank Limited")
    private String sponsorBankForIftas;

    // Address Details
    @JsonProperty("address_type")
    @JsonAlias({"addressType"})
    @Schema(description = "Type of address", example = "HEAD_OFFICE")
    private String addressType;

    @JsonProperty("unit_gala_name_and_number")
    @JsonAlias({"unitGalaNameAndNumber", "unit_gala_name_number", "unitGalaNameNumber", "building_name", "buildingName"})
    @Schema(description = "Unit / Gala / Building name and number", example = "Axis House, C-2, Wadia International Centre")
    private String unitGalaNameNumber;

    @JsonProperty("street_road")
    @JsonAlias({"streetRoad", "street_or_road", "streetOrRoad", "street", "road"})
    @Schema(description = "Street or road name", example = "Pandurang Budhkar Marg, Worli")
    private String streetRoad;

    @JsonProperty("landmark")
    @JsonAlias({"Landmark"})
    @Schema(description = "Nearby landmark", example = "Near Century Mills")
    private String landmark;

    @JsonProperty("city")
    @JsonAlias({"City"})
    @Schema(description = "City", example = "Mumbai")
    private String city;

    @JsonProperty("state")
    @JsonAlias({"State"})
    @Schema(description = "State", example = "Maharashtra")
    private String state;

    @JsonProperty("pincode")
    @JsonAlias({"pin_code", "pinCode", "postalCode", "zip"})
    @Schema(description = "6-digit PIN code", example = "400025")
    private String pincode;

    // Helper getters / setters for compatibility
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

    public String getName() {
        return bankName != null ? bankName : legalName;
    }

    public String getType() {
        return bankType;
    }

    public String getRegistrationNumber() {
        return licenseNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.licenseNumber = registrationNumber;
    }

    public String getGstNo() {
        return gstNumber;
    }

    public void setGstNo(String gstNo) {
        this.gstNumber = gstNo;
    }

    public String getPanNumber() {
        return pan;
    }

    public void setPanNumber(String panNumber) {
        this.pan = panNumber;
    }

    public String getCinNumber() {
        return cin;
    }

    public void setCinNumber(String cinNumber) {
        this.cin = cinNumber;
    }

    public String getMicrDetails() {
        return micrCode;
    }

    public void setMicrDetails(String micrDetails) {
        this.micrCode = micrDetails;
    }

    public String getUnitGalaNameAndNumber() {
        return unitGalaNameNumber;
    }

    public void setUnitGalaNameAndNumber(String unitGalaNameAndNumber) {
        this.unitGalaNameNumber = unitGalaNameAndNumber;
    }

    public String getStreetOrRoad() {
        return streetRoad;
    }

    public void setStreetOrRoad(String streetOrRoad) {
        this.streetRoad = streetOrRoad;
    }

    public String getSponsorBankOfClearing() {
        return sponsorBankForClearing;
    }

    public void setSponsorBankOfClearing(String sponsorBankOfClearing) {
        this.sponsorBankForClearing = sponsorBankOfClearing;
    }

    public String getSponsorBankOfIftas() {
        return sponsorBankForIftas;
    }

    public void setSponsorBankOfIftas(String sponsorBankOfIftas) {
        this.sponsorBankForIftas = sponsorBankOfIftas;
    }

    public static class UpdateOrganizationRequestBuilder {
        public UpdateOrganizationRequestBuilder registrationNumber(String registrationNumber) {
            this.licenseNumber = registrationNumber;
            return this;
        }

        public UpdateOrganizationRequestBuilder gstNo(String gstNo) {
            this.gstNumber = gstNo;
            return this;
        }

        public UpdateOrganizationRequestBuilder panNumber(String panNumber) {
            this.pan = panNumber;
            return this;
        }

        public UpdateOrganizationRequestBuilder cinNumber(String cinNumber) {
            this.cin = cinNumber;
            return this;
        }

        public UpdateOrganizationRequestBuilder micrDetails(String micrDetails) {
            this.micrCode = micrDetails;
            return this;
        }

        public UpdateOrganizationRequestBuilder unitGalaNameAndNumber(String unitGalaNameAndNumber) {
            this.unitGalaNameNumber = unitGalaNameAndNumber;
            return this;
        }

        public UpdateOrganizationRequestBuilder streetOrRoad(String streetOrRoad) {
            this.streetRoad = streetOrRoad;
            return this;
        }

        public UpdateOrganizationRequestBuilder sponsorBankOfClearing(String sponsorBankOfClearing) {
            this.sponsorBankForClearing = sponsorBankOfClearing;
            return this;
        }

        public UpdateOrganizationRequestBuilder sponsorBankOfIftas(String sponsorBankOfIftas) {
            this.sponsorBankForIftas = sponsorBankOfIftas;
            return this;
        }
    }
}
