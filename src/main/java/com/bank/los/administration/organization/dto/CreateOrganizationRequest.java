package com.bank.los.administration.organization.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body for onboarding a new Bank or NBFC Organization")
public class CreateOrganizationRequest {

    @Schema(description = "Optional custom UUID (auto-generated if omitted)", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @JsonProperty("bank_code")
    @JsonAlias({"bankCode", "code"})
    @Schema(description = "Optional bank code (auto-generated if omitted)", example = "SBI01")
    private String bankCode;

    @NotBlank(message = "bank_name is required")
    @Size(max = 150, message = "bank_name must not exceed 150 characters")
    @JsonProperty("bank_name")
    @JsonAlias({"bankName", "name"})
    @Schema(description = "Display name of the bank", example = "State Bank of India")
    private String bankName;

    @NotBlank(message = "legal_name is required")
    @Size(max = 200, message = "legal_name must not exceed 200 characters")
    @JsonProperty("legal_name")
    @JsonAlias({"legalName"})
    @Schema(description = "Registered legal entity name", example = "State Bank of India Limited")
    private String legalName;

    @NotBlank(message = "bank_type is required")
    @JsonProperty("bank_type")
    @JsonAlias({ "bankType", "type" })
    @Schema(description = "Type of financial institution / bank", example = "COMMERCIAL_BANK", allowableValues = {
            "BANK", "NBFC", "COMMERCIAL_BANK", "SMALL_FINANCE_BANK", "PAYMENT_BANK", "COOPERATIVE_BANK",
            "HOUSING_FINANCE" })
    private String bankType;

    @NotBlank(message = "license_number is required")
    @JsonProperty("license_number")
    @JsonAlias({"licenseNumber", "license_no", "licenseNo", "registration_number", "registrationNumber"})
    @Schema(description = "Banking license / incorporation certificate number", example = "LIC-MH-2024-1001")
    private String licenseNumber;

    @NotBlank(message = "PAN is required")
    @Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "PAN must be a valid 10-character Indian PAN format (e.g. ABCDE1234F)")
    @JsonProperty("PAN")
    @JsonAlias({"pan", "Pan", "pan_number", "panNumber"})
    @Schema(description = "10-digit Permanent Account Number", example = "AAACS1234F")
    private String pan;

    @JsonProperty("gst_number")
    @JsonAlias({"gstNumber", "gst_no", "gstNo", "GST", "gst"})
    @Schema(description = "15-digit Goods and Services Tax Identification Number (GSTIN)", example = "27AAACS1234F1Z5")
    private String gstNumber;

    // @NotBlank(message = "CIN is required")
    @JsonProperty("CIN")
    @JsonAlias({"cin", "Cin", "cin_number", "cinNumber"})
    @Schema(description = "Corporate Identification Number (CIN)", example = "L65190MH2004GOI148838")
    private String cin;

    @JsonProperty("website")
    @Schema(description = "Official website URL", example = "https://www.sbi.co.in")
    private String website;

    @JsonProperty("logo")
    @Schema(description = "Logo image URL or base64 asset identifier", example = "https://assets.bank.com/logos/sbi.png")
    private String logo;

    // @NotNull(message = "regulatory_authority_id is required")
    @JsonProperty("regulatory_authority_id")
    @JsonAlias({"regulatoryAuthorityId"})
    @Schema(description = "UUID of the governing regulatory authority (e.g., RBI UUID)", example = "b5a76e2d-3c9f-4321-9e87-654321fedcba")
    private UUID regulatoryAuthorityId;

    @NotBlank(message = "regulatory_status is required")
    @JsonProperty("regulatory_status")
    @JsonAlias({ "regulatoryStatus" })
    @Schema(description = "Regulatory compliance status", example = "ACTIVE", allowableValues = { "ACTIVE", "LICENSED",
            "REGULATED", "PENDING_APPROVAL", "SUSPENDED" })
    private String regulatoryStatus;

    @NotBlank(message = "country is required")
    @JsonProperty("country")
    @Schema(description = "Country of jurisdiction / operation", example = "India")
    private String country;

    @JsonProperty("contact_email")
    @JsonAlias({"contactEmail", "email"})
    @Schema(description = "Primary contact email address", example = "compliance@sbi.co.in")
    private String contactEmail;

    @JsonProperty("contact_phone")
    @JsonAlias({"contactPhone", "phone"})
    @Schema(description = "Primary contact telephone number", example = "+912222740000")
    private String contactPhone;

    @JsonProperty("db_name")
    @JsonAlias({"dbName"})
    @Size(max = 100, message = "Database name must not exceed 100 characters")
    @Schema(description = "Dedicated PostgreSQL database name allocated for this organization (optional, auto-generated if omitted)", example = "los_sbi01_db")
    private String dbName;

    @JsonProperty("db_host")
    @JsonAlias({"dbHost"})
    @Schema(description = "Database host", example = "localhost", defaultValue = "localhost")
    @Builder.Default
    private String dbHost = "localhost";

    @JsonProperty("db_port")
    @JsonAlias({"dbPort"})
    @Schema(description = "Database port", example = "5432", defaultValue = "5432")
    @Builder.Default
    private Integer dbPort = 5432;

    // Regulatory & Banking clearing details
    @JsonProperty("direct_clearing_number")
    @JsonAlias({"directClearingNumber", "clearing_number", "clearingNumber"})
    @Schema(description = "Direct clearing number or indicator code", example = "CLR-SBI-01")
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
    @Schema(description = "Full 9-digit MICR code or details", example = "400002001")
    private String micrCode;

    @JsonProperty("micr_city_code")
    @JsonAlias({"micrCityCode"})
    @Schema(description = "3-digit MICR city code", example = "400")
    private String micrCityCode;

    @JsonProperty("micr_bank_code")
    @JsonAlias({"micrBankCode"})
    @Schema(description = "3-digit MICR bank code", example = "002")
    private String micrBankCode;

    @JsonProperty("micr_branch_code")
    @JsonAlias({"micrBranchCode"})
    @Schema(description = "3-digit MICR branch code", example = "001")
    private String micrBranchCode;

    @JsonProperty("ifsc_code")
    @JsonAlias({"ifscCode", "ifsc", "IFSC"})
    @Schema(description = "11-character Indian Financial System Code (IFSC)", example = "SBIN0000001")
    private String ifscCode;

    @JsonProperty("number_of_branches")
    @JsonAlias({"numberOfBranches", "branches_count", "branchesCount", "totalBranches", "total_branches"})
    @Schema(description = "Total number of bank branches", example = "22000")
    private Integer numberOfBranches;

    @JsonProperty("sponsor_bank_of_clearing")
    @JsonAlias({"sponsorBankOfClearing", "sponsor_bank_for_clearing", "sponsorBankForClearing"})
    @Schema(description = "Sponsor bank for clearing operations", example = "Reserve Bank of India")
    private String sponsorBankForClearing;

    @JsonProperty("sponsor_bank_of_iftas")
    @JsonAlias({"sponsorBankOfIftas", "sponsor_bank_for_iftas", "sponsorBankForIftas"})
    @Schema(description = "Sponsor bank for IFTAS operations", example = "State Bank of India")
    private String sponsorBankForIftas;

    // Address Details
    @JsonProperty("address_type")
    @JsonAlias({ "addressType" })
    @Schema(description = "Type of address", example = "HEAD_OFFICE", allowableValues = { "HEAD_OFFICE",
            "REGISTERED_OFFICE", "CORPORATE_OFFICE", "BRANCH" })
    private String addressType;

    @JsonProperty("unit_gala_name_and_number")
    @JsonAlias({ "unitGalaNameAndNumber", "unit_gala_name_number", "unitGalaNameNumber", "building_name",
            "buildingName" })
    @Schema(description = "Unit / Gala / Building name and number", example = "State Bank Bhavan, 4th Floor")
    private String unitGalaNameNumber;

    @JsonProperty("street_road")
    @JsonAlias({"streetRoad", "street_or_road", "streetOrRoad", "street", "road"})
    @Schema(description = "Street or road name", example = "Madame Cama Road")
    private String streetRoad;

    @JsonProperty("landmark")
    @JsonAlias({"Landmark"})
    @Schema(description = "Nearby landmark", example = "Nariman Point")
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
    @Schema(description = "6-digit PIN code", example = "400021")
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

    public static class CreateOrganizationRequestBuilder {
        public CreateOrganizationRequestBuilder registrationNumber(String registrationNumber) {
            this.licenseNumber = registrationNumber;
            return this;
        }

        public CreateOrganizationRequestBuilder gstNo(String gstNo) {
            this.gstNumber = gstNo;
            return this;
        }

        public CreateOrganizationRequestBuilder panNumber(String panNumber) {
            this.pan = panNumber;
            return this;
        }

        public CreateOrganizationRequestBuilder cinNumber(String cinNumber) {
            this.cin = cinNumber;
            return this;
        }

        public CreateOrganizationRequestBuilder micrDetails(String micrDetails) {
            this.micrCode = micrDetails;
            return this;
        }

        public CreateOrganizationRequestBuilder unitGalaNameAndNumber(String unitGalaNameAndNumber) {
            this.unitGalaNameNumber = unitGalaNameAndNumber;
            return this;
        }

        public CreateOrganizationRequestBuilder streetOrRoad(String streetOrRoad) {
            this.streetRoad = streetOrRoad;
            return this;
        }

        public CreateOrganizationRequestBuilder sponsorBankOfClearing(String sponsorBankOfClearing) {
            this.sponsorBankForClearing = sponsorBankOfClearing;
            return this;
        }

        public CreateOrganizationRequestBuilder sponsorBankOfIftas(String sponsorBankOfIftas) {
            this.sponsorBankForIftas = sponsorBankOfIftas;
            return this;
        }
    }
}
