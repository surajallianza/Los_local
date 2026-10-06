package com.bank.los.security;

import com.bank.los.administration.master.entity.Organization;
import com.bank.los.administration.master.repository.OrganizationRepository;
import com.bank.los.common.constant.ApplicationConstants;
import com.bank.los.common.exception.UnauthorizedException;
import com.bank.los.config.BankContext;
import com.bank.los.config.OrganizationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service responsible for resolving tenant/organization IDs (extracted from verified JWT claims)
 * to internal physical database names by looking up the Master DB.
 * 
 * Never trusts any client-provided header or parameter for database routing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantResolutionService {

    private final OrganizationRepository organizationRepository;

    // High-speed thread-safe in-memory resolution caches
    private final Map<Long, String> orgIdToDbCache = new ConcurrentHashMap<>();
    private final Map<String, String> orgCodeToDbCache = new ConcurrentHashMap<>();
    private final Map<UUID, String> orgUuidToDbCache = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void initCache() {
        try {
            BankContext.setCurrentBank(BankContext.MASTER_BANK_ID);
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);
            organizationRepository.findAll().forEach(this::cacheOrganization);
            log.info("TenantResolutionService initialized with {} cached organizations", orgIdToDbCache.size());
        } catch (Exception e) {
            log.debug("Tenant resolution cache will populate dynamically: {}", e.getMessage());
        }
    }

    /**
     * Resolves the physical database name for an organization securely from the Master DB.
     *
     * @param orgId    Primary key ID of the organization in master DB
     * @param orgCode  Institution / Organization Code
     * @param orgUuid  UUID of the organization
     * @param userType User Type (INTERNAL, STAFF, CUSTOMER)
     * @return Physical database name (e.g. "los_icicib01_db" or "los_master_db")
     */
    public String resolveTenantDb(Long orgId, String orgCode, UUID orgUuid, String userType) {
        // Platform internal users always route to Master DB
        if (ApplicationConstants.UserTypes.INTERNAL.equalsIgnoreCase(userType) ||
                "MASTER".equalsIgnoreCase(orgCode) ||
                (orgId != null && orgId == 0L)) {
            return OrganizationContext.MASTER_ORG_ID;
        }

        // 1. Fast cache lookup
        if (orgId != null && orgId > 0 && orgIdToDbCache.containsKey(orgId)) {
            return orgIdToDbCache.get(orgId);
        }
        if (orgCode != null && !orgCode.isBlank() && orgCodeToDbCache.containsKey(orgCode.toUpperCase())) {
            return orgCodeToDbCache.get(orgCode.toUpperCase());
        }
        if (orgUuid != null && orgUuidToDbCache.containsKey(orgUuid)) {
            return orgUuidToDbCache.get(orgUuid);
        }

        // 2. Safe Master DB lookup
        String prevBank = BankContext.getCurrentBank();
        try {
            BankContext.setCurrentBank(BankContext.MASTER_BANK_ID);
            OrganizationContext.setCurrentOrganization(OrganizationContext.MASTER_ORG_ID);

            Organization org = null;
            if (orgId != null && orgId > 0) {
                org = organizationRepository.findById(orgId).orElse(null);
            }
            if (org == null && orgUuid != null) {
                org = organizationRepository.findByUuid(orgUuid).orElse(null);
            }
            if (org == null && orgCode != null && !orgCode.isBlank()) {
                org = organizationRepository.findByBankCode(orgCode)
                        .or(() -> organizationRepository.findByCode(orgCode))
                        .orElse(null);
            }

            if (org != null) {
                validateOrganization(org);
                cacheOrganization(org);
                return org.getDbName();
            }
        } finally {
            if (prevBank != null) {
                BankContext.setCurrentBank(prevBank);
                OrganizationContext.setCurrentOrganization(prevBank);
            }
        }

        return OrganizationContext.MASTER_ORG_ID;
    }

    private void validateOrganization(Organization org) {
        if (ApplicationConstants.OrgStatus.SUSPENDED.equalsIgnoreCase(org.getStatus())) {
            throw new UnauthorizedException("Organization account is suspended");
        }
        if (ApplicationConstants.OrgStatus.INACTIVE.equalsIgnoreCase(org.getStatus())) {
            throw new UnauthorizedException("Organization account is inactive");
        }
    }

    public void cacheOrganization(Organization org) {
        if (org != null && org.getDbName() != null) {
            if (org.getId() != null) {
                orgIdToDbCache.put(org.getId(), org.getDbName());
            }
            if (org.getBankCode() != null) {
                orgCodeToDbCache.put(org.getBankCode().toUpperCase(), org.getDbName());
            }
            if (org.getUuid() != null) {
                orgUuidToDbCache.put(org.getUuid(), org.getDbName());
            }
        }
    }

    public void evictCache(Long orgId) {
        if (orgId != null) {
            orgIdToDbCache.remove(orgId);
        }
    }
}
