package com.bank.los.administration.organization.service;

import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.administration.organization.dto.CreateOrganizationRequest;
import com.bank.los.administration.organization.dto.OrganizationResponse;
import com.bank.los.administration.organization.dto.UpdateOrganizationRequest;
import com.bank.los.bank.branch.dto.BranchResponse;
import com.bank.los.bank.master.entity.Branch;
import com.bank.los.bank.master.entity.OrganizationRole;
import com.bank.los.bank.master.repository.BranchRepository;
import com.bank.los.bank.master.repository.OrganizationRoleRepository;
import com.bank.los.bank.user.dto.RoleResponse;
import com.bank.los.common.exception.BusinessException;
import com.bank.los.common.exception.ResourceNotFoundException;
import com.bank.los.config.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationProvisioningService organizationProvisioningService;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final BranchRepository branchRepository;

    public List<OrganizationResponse> getAllOrganizations() {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        return organizationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public OrganizationResponse getOrganizationById(Long id) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", id));
        return mapToResponse(org);
    }

    public OrganizationResponse getOrganizationByUuid(UUID uuid) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = organizationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "uuid", uuid));
        return mapToResponse(org);
    }

    public Organization findOrganizationByIdOrIdentifier(String identifier) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        try {
            Long numericId = Long.parseLong(identifier);
            Optional<Organization> byId = organizationRepository.findById(numericId);
            if (byId.isPresent()) return byId.get();
        } catch (NumberFormatException ignored) {}

        try {
            UUID uuid = UUID.fromString(identifier);
            Optional<Organization> byUuid = organizationRepository.findByUuid(uuid);
            if (byUuid.isPresent()) return byUuid.get();
        } catch (IllegalArgumentException ignored) {}

        return organizationRepository.findByBankCode(identifier.toUpperCase())
                .or(() -> organizationRepository.findByCode(identifier.toUpperCase()))
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "identifier", identifier));
    }

    @Transactional
    public OrganizationResponse updateOrganization(String identifier, UpdateOrganizationRequest request) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = findOrganizationByIdOrIdentifier(identifier);

        if (request.getBankName() != null && !request.getBankName().isBlank()) {
            org.setBankName(request.getBankName().trim());
        }
        if (request.getLegalName() != null && !request.getLegalName().isBlank()) {
            org.setLegalName(request.getLegalName().trim());
        }
        if (request.getBankType() != null && !request.getBankType().isBlank()) {
            org.setBankType(request.getBankType().trim().toUpperCase());
        }
        if (request.getLicenseNumber() != null && !request.getLicenseNumber().isBlank()) {
            org.setLicenseNumber(request.getLicenseNumber().trim());
        } else if (request.getRegistrationNumber() != null && !request.getRegistrationNumber().isBlank()) {
            org.setLicenseNumber(request.getRegistrationNumber().trim());
        }
        if (request.getPan() != null && !request.getPan().isBlank()) {
            org.setPan(request.getPan().trim().toUpperCase());
        }
        if (request.getGstNumber() != null && !request.getGstNumber().isBlank()) {
            org.setGstNo(request.getGstNumber().trim().toUpperCase());
        } else if (request.getGstNo() != null && !request.getGstNo().isBlank()) {
            org.setGstNo(request.getGstNo().trim().toUpperCase());
        }
        if (request.getCin() != null && !request.getCin().isBlank()) {
            org.setCin(request.getCin().trim().toUpperCase());
        }
        if (request.getDirectClearingNumber() != null) {
            org.setDirectClearingNumber(request.getDirectClearingNumber().trim());
        }
        if (request.getDirectClearingMember() != null) {
            org.setDirectClearingMember(request.getDirectClearingMember());
        } else if (request.getDirectClearingNumber() != null) {
            org.setDirectClearingMember(!request.getDirectClearingNumber().trim().equalsIgnoreCase("false"));
        }
        if (request.getDirectMemberIftas() != null) {
            org.setDirectMemberIftas(request.getDirectMemberIftas());
        }

        // MICR details
        String micrCode = request.getMicrCode();
        String micrCityCode = request.getMicrCityCode();
        String micrBankCode = request.getMicrBankCode();
        String micrBranchCode = request.getMicrBranchCode();
        if (micrCode != null) {
            String cleanMicr = micrCode.replaceAll("[^0-9]", "");
            if (cleanMicr.length() == 9) {
                if (micrCityCode == null) micrCityCode = cleanMicr.substring(0, 3);
                if (micrBankCode == null) micrBankCode = cleanMicr.substring(3, 6);
                if (micrBranchCode == null) micrBranchCode = cleanMicr.substring(6, 9);
            }
            org.setMicrCode(micrCode);
        }
        if (micrCityCode != null) org.setMicrCityCode(micrCityCode);
        if (micrBankCode != null) org.setMicrBankCode(micrBankCode);
        if (micrBranchCode != null) org.setMicrBranchCode(micrBranchCode);

        if (request.getIfscCode() != null) {
            org.setIfscCode(request.getIfscCode().trim().toUpperCase());
        }
        if (request.getNumberOfBranches() != null) {
            org.setNumberOfBranches(request.getNumberOfBranches());
        }
        if (request.getSponsorBankForClearing() != null) {
            org.setSponsorBankForClearing(request.getSponsorBankForClearing().trim());
        }
        if (request.getSponsorBankForIftas() != null) {
            org.setSponsorBankForIftas(request.getSponsorBankForIftas().trim());
        }

        // Address details
        if (request.getAddressType() != null) org.setAddressType(request.getAddressType().trim());
        if (request.getUnitGalaNameNumber() != null) org.setUnitGalaNameNumber(request.getUnitGalaNameNumber().trim());
        if (request.getStreetRoad() != null) org.setStreetRoad(request.getStreetRoad().trim());
        if (request.getLandmark() != null) org.setLandmark(request.getLandmark().trim());
        if (request.getCity() != null) org.setCity(request.getCity().trim());
        if (request.getState() != null) org.setState(request.getState().trim());
        if (request.getPincode() != null) org.setPincode(request.getPincode().trim());

        if (request.getWebsite() != null) org.setWebsite(request.getWebsite().trim());
        if (request.getLogo() != null) org.setLogo(request.getLogo().trim());
        if (request.getRegulatoryAuthorityId() != null) org.setRegulatoryAuthorityId(request.getRegulatoryAuthorityId());
        if (request.getRegulatoryStatus() != null) org.setRegulatoryStatus(request.getRegulatoryStatus().trim().toUpperCase());
        if (request.getCountry() != null) org.setCountry(request.getCountry().trim());
        if (request.getStatus() != null) org.setStatus(request.getStatus().trim().toUpperCase());
        if (request.getContactEmail() != null) org.setContactEmail(request.getContactEmail().trim());
        if (request.getContactPhone() != null) org.setContactPhone(request.getContactPhone().trim());

        Organization updated = organizationRepository.save(org);
        log.info("Bank/Organization updated successfully: id={}, bankCode={}", updated.getId(), updated.getBankCode());
        return mapToResponse(updated);
    }

    @Transactional
    public void deleteOrganization(String identifier, boolean hardDelete) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = findOrganizationByIdOrIdentifier(identifier);

        if (hardDelete) {
            organizationRepository.delete(org);
            log.warn("Bank/Organization HARD DELETED: id={}, bankCode={}", org.getId(), org.getBankCode());
        } else {
            org.setStatus("DELETED");
            organizationRepository.save(org);
            log.info("Bank/Organization SOFT DELETED (status=DELETED): id={}, bankCode={}", org.getId(), org.getBankCode());
        }
    }

    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

        String code = request.getBankCode();
        if (code == null || code.isBlank()) {
            String baseName = request.getBankName() != null ? request.getBankName() : 
                    (request.getLegalName() != null ? request.getLegalName() : "BANK");
            String clean = baseName.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            if (clean.length() > 6) {
                clean = clean.substring(0, 6);
            }
            if (clean.isBlank()) {
                clean = "BANK";
            }
            int seq = 1;
            code = clean + String.format("%02d", seq);
            while (organizationRepository.existsByCode(code) || organizationRepository.existsByBankCode(code)) {
                seq++;
                code = clean + String.format("%02d", seq);
            }
        } else {
            code = code.trim().toUpperCase();
            if (organizationRepository.existsByCode(code) || organizationRepository.existsByBankCode(code)) {
                throw new BusinessException("BANK_CODE_IN_USE", "Bank code " + code + " is already assigned");
            }
        }

        String name = request.getBankName() != null ? request.getBankName() : request.getLegalName();
        String shortName = name != null ? name.split("\\s+")[0] : code;

        String dbName = request.getDbName();
        if (dbName == null || dbName.isBlank()) {
            dbName = "los_" + code.toLowerCase().replaceAll("[^a-z0-9]", "") + "_db";
        }

        if (organizationRepository.existsByDbName(dbName)) {
            throw new BusinessException("DATABASE_NAME_IN_USE", "Database name " + dbName + " is already assigned");
        }

        UUID orgUuid = request.getId() != null ? request.getId() : UUID.randomUUID();

        // Parse MICR codes if full MICR code or details provided
        String micrCode = request.getMicrCode();
        String micrCityCode = request.getMicrCityCode();
        String micrBankCode = request.getMicrBankCode();
        String micrBranchCode = request.getMicrBranchCode();
        if (micrCode != null) {
            String cleanMicr = micrCode.replaceAll("[^0-9]", "");
            if (cleanMicr.length() == 9) {
                if (micrCityCode == null) micrCityCode = cleanMicr.substring(0, 3);
                if (micrBankCode == null) micrBankCode = cleanMicr.substring(3, 6);
                if (micrBranchCode == null) micrBranchCode = cleanMicr.substring(6, 9);
            }
        }

        Boolean directClearingMember = request.getDirectClearingMember();
        if (directClearingMember == null && request.getDirectClearingNumber() != null) {
            directClearingMember = !request.getDirectClearingNumber().trim().equalsIgnoreCase("false");
        }

        Organization org = Organization.builder()
                .uuid(orgUuid)
                .bankCode(code)
                .bankName(request.getBankName())
                .legalName(request.getLegalName())
                .shortName(shortName)
                .bankType(request.getBankType() != null ? request.getBankType().toUpperCase() : "BANK")
                .licenseNumber(request.getLicenseNumber() != null ? request.getLicenseNumber() : request.getRegistrationNumber())
                .pan(request.getPan() != null ? request.getPan().toUpperCase() : null)
                .gstNo(request.getGstNumber() != null ? request.getGstNumber().toUpperCase() : (request.getGstNo() != null ? request.getGstNo().toUpperCase() : null))
                .cin(request.getCin() != null ? request.getCin().toUpperCase() : null)
                .website(request.getWebsite())
                .logo(request.getLogo())
                .regulatoryAuthorityId(request.getRegulatoryAuthorityId())
                .regulatoryStatus(request.getRegulatoryStatus() != null ? request.getRegulatoryStatus().toUpperCase() : "ACTIVE")
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .status("ACTIVE")
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .dbName(dbName)
                .dbHost(request.getDbHost() != null ? request.getDbHost() : "localhost")
                .dbPort(request.getDbPort() != null ? request.getDbPort() : 5432)
                .directClearingNumber(request.getDirectClearingNumber())
                .directClearingMember(directClearingMember)
                .directMemberIftas(request.getDirectMemberIftas())
                .micrCode(micrCode)
                .micrCityCode(micrCityCode)
                .micrBankCode(micrBankCode)
                .micrBranchCode(micrBranchCode)
                .ifscCode(request.getIfscCode())
                .numberOfBranches(request.getNumberOfBranches())
                .sponsorBankForClearing(request.getSponsorBankForClearing())
                .sponsorBankForIftas(request.getSponsorBankForIftas())
                .addressType(request.getAddressType())
                .unitGalaNameNumber(request.getUnitGalaNameNumber())
                .streetRoad(request.getStreetRoad())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .build();

        Organization saved = organizationRepository.save(org);

        // Auto-provision organization DB schema, default roles (ADMIN, MAKER, etc.), permissions, and primary branch
        organizationProvisioningService.provisionOrganization(
                saved.getDbName(),
                saved.getCode(),
                saved.getName(),
                saved.getDbHost(),
                saved.getDbPort()
        );

        return mapToResponse(saved);
    }

    public List<RoleResponse> getOrganizationRoles(Long orgId) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        OrganizationContext.setCurrentOrganization(org.getDbName());
        OrganizationContext.setCurrentOrgCode(org.getCode());
        try {
            return organizationRoleRepository.findAll().stream()
                    .map(this::mapRoleToResponse)
                    .collect(Collectors.toList());
        } finally {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        }
    }

    public List<BranchResponse> getOrganizationBranches(Long orgId) {
        OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        Organization org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", orgId));

        OrganizationContext.setCurrentOrganization(org.getDbName());
        OrganizationContext.setCurrentOrgCode(org.getCode());
        try {
            return branchRepository.findAll().stream()
                    .map(this::mapBranchToResponse)
                    .collect(Collectors.toList());
        } finally {
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
        }
    }

    public OrganizationResponse mapToResponse(Organization org) {
        String micr = org.getMicrCode();
        if (micr == null && org.getMicrCityCode() != null && org.getMicrBankCode() != null && org.getMicrBranchCode() != null) {
            micr = org.getMicrCityCode() + org.getMicrBankCode() + org.getMicrBranchCode();
        }

        return OrganizationResponse.builder()
                .id(org.getUuid() != null ? org.getUuid() : UUID.nameUUIDFromBytes(String.valueOf(org.getId()).getBytes()))
                .pkid(org.getId())
                .bankCode(org.getBankCode())
                .bankName(org.getBankName() != null ? org.getBankName() : org.getName())
                .legalName(org.getLegalName())
                .bankType(org.getBankType() != null ? org.getBankType() : org.getType())
                .licenseNumber(org.getLicenseNumber())
                .registrationNumber(org.getLicenseNumber())
                .pan(org.getPan())
                .gstNumber(org.getGstNumber())
                .gstNo(org.getGstNumber())
                .cin(org.getCin())
                .directClearingNumber(org.getDirectClearingNumber())
                .directClearingMember(org.getDirectClearingMember())
                .directMemberIftas(org.getDirectMemberIftas())
                .micrCode(micr)
                .micrCityCode(org.getMicrCityCode())
                .micrBankCode(org.getMicrBankCode())
                .micrBranchCode(org.getMicrBranchCode())
                .ifscCode(org.getIfscCode())
                .numberOfBranches(org.getNumberOfBranches())
                .sponsorBankForClearing(org.getSponsorBankForClearing())
                .sponsorBankForIftas(org.getSponsorBankForIftas())
                .addressType(org.getAddressType())
                .unitGalaNameNumber(org.getUnitGalaNameNumber())
                .streetRoad(org.getStreetRoad())
                .landmark(org.getLandmark())
                .city(org.getCity())
                .state(org.getState())
                .pincode(org.getPincode())
                .website(org.getWebsite())
                .logo(org.getLogo())
                .regulatoryAuthorityId(org.getRegulatoryAuthorityId())
                .regulatoryStatus(org.getRegulatoryStatus())
                .country(org.getCountry())
                .status(org.getStatus())
                .contactEmail(org.getContactEmail())
                .contactPhone(org.getContactPhone())
                .dbName(org.getDbName())
                .dbHost(org.getDbHost())
                .dbPort(org.getDbPort())
                .createdAt(org.getCreatedAt())
                .updatedAt(org.getUpdatedAt())
                .build();
    }

    private RoleResponse mapRoleToResponse(OrganizationRole role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .panel(role.getPanel())
                .description(role.getDescription())
                .build();
    }

    private BranchResponse mapBranchToResponse(Branch branch) {
        return BranchResponse.builder()
                .id(branch.getId())
                .name(branch.getName())
                .code(branch.getCode())
                .address(branch.getAddress())
                .city(branch.getCity())
                .state(branch.getState())
                .pincode(branch.getPincode())
                .status(branch.getStatus())
                .build();
    }
}

