package com.bank.los.administration.master.entity;

import com.bank.los.common.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Entity
@Table(name = "organizations", schema = "organization")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Organization extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true)
    @Builder.Default
    private UUID uuid = UUID.randomUUID();

    @Column(name = "bank_code", nullable = false, unique = true, length = 50)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 150)
    private String bankName;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "bank_type", nullable = false, length = 50)
    private String bankType;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(name = "pan", length = 20)
    private String pan;

    @Column(name = "gst_no", length = 50)
    private String gstNo;

    @Column(name = "cin", length = 50)
    private String cin;

    @Column(name = "direct_clearing_number", length = 50)
    private String directClearingNumber;

    @Column(name = "micr_code", length = 9)
    private String micrCode;

    @Column(name = "website", length = 255)
    private String website;

    @Column(name = "logo", columnDefinition = "TEXT")
    private String logo;

    @Column(name = "regulatory_authority_id")
    private UUID regulatoryAuthorityId;

    @Column(name = "regulatory_status", length = 50)
    @Builder.Default
    private String regulatoryStatus = "ACTIVE"; // 'ACTIVE', 'LICENSED', 'REGULATED', 'SUSPENDED'

    @Column(name = "country", length = 100)
    @Builder.Default
    private String country = "India";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // 'ACTIVE', 'INACTIVE', 'SUSPENDED'

    @Column(name = "contact_email", length = 150)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "db_name", nullable = false, unique = true, length = 100)
    private String dbName;

    @Column(name = "db_host", nullable = false, length = 150)
    @Builder.Default
    private String dbHost = "localhost";

    @Column(name = "db_port", nullable = false)
    @Builder.Default
    private Integer dbPort = 5432;

    @Column(name = "direct_clearing_member")
    private Boolean directClearingMember;

    @Column(name = "direct_member_iftas")
    private Boolean directMemberIftas;

    @Column(name = "micr_city_code", length = 3)
    private String micrCityCode;

    @Column(name = "micr_bank_code", length = 3)
    private String micrBankCode;

    @Column(name = "micr_branch_code", length = 3)
    private String micrBranchCode;

    @Column(name = "ifsc_code", length = 11)
    private String ifscCode;

    @Column(name = "number_of_branches")
    private Integer numberOfBranches;

    @Column(name = "sponsor_bank_for_clearing", length = 150)
    private String sponsorBankForClearing;

    @Column(name = "sponsor_bank_for_iftas", length = 150)
    private String sponsorBankForIftas;

    @Column(name = "address_type", length = 50)
    private String addressType;

    @Column(name = "unit_gala_name_number", length = 200)
    private String unitGalaNameNumber;

    @Column(name = "street_road", length = 200)
    private String streetRoad;

    @Column(name = "landmark", length = 150)
    private String landmark;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "pincode", length = 6)
    private String pincode;

    // Helper compatibility getters / setters
    public String getCode() {
        return bankCode != null ? bankCode : "";
    }

    public void setCode(String code) {
        this.bankCode = code;
    }

    public String getName() {
        return bankName != null ? bankName : (legalName != null ? legalName : "");
    }

    public void setName(String name) {
        this.bankName = name;
    }

    public String getType() {
        return bankType != null ? bankType : "BANK";
    }

    public void setType(String type) {
        this.bankType = type;
    }

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

    public String getRegistrationNumber() {
        return licenseNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.licenseNumber = registrationNumber;
    }

    public String getCin() {
        return cin != null ? cin : gstNo;
    }

    public void setCin(String cin) {
        this.cin = cin;
    }

    public String getGstNumber() {
        return gstNo;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNo = gstNumber;
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

    public static class OrganizationBuilder {
        public OrganizationBuilder bankCode(String bankCode) {
            this.bankCode = bankCode;
            return this;
        }

        public OrganizationBuilder bankName(String bankName) {
            this.bankName = bankName;
            return this;
        }

        public OrganizationBuilder bankType(String bankType) {
            this.bankType = bankType;
            return this;
        }

        public OrganizationBuilder code(String code) {
            this.bankCode = code;
            return this;
        }

        public OrganizationBuilder name(String name) {
            this.bankName = name;
            return this;
        }

        public OrganizationBuilder type(String type) {
            this.bankType = type;
            return this;
        }

        public OrganizationBuilder registrationNumber(String registrationNumber) {
            this.licenseNumber = registrationNumber;
            return this;
        }

        public OrganizationBuilder licenseNumber(String licenseNumber) {
            this.licenseNumber = licenseNumber;
            return this;
        }

        public OrganizationBuilder cin(String cin) {
            this.cin = cin;
            return this;
        }

        public OrganizationBuilder cinNumber(String cinNumber) {
            this.cin = cinNumber;
            return this;
        }

        public OrganizationBuilder gstNo(String gstNo) {
            this.gstNo = gstNo;
            return this;
        }

        public OrganizationBuilder gstNumber(String gstNumber) {
            this.gstNo = gstNumber;
            return this;
        }

        public OrganizationBuilder panNumber(String panNumber) {
            this.pan = panNumber;
            return this;
        }

        public OrganizationBuilder directClearingNumber(String directClearingNumber) {
            this.directClearingNumber = directClearingNumber;
            return this;
        }

        public OrganizationBuilder micrCode(String micrCode) {
            this.micrCode = micrCode;
            return this;
        }
    }
}