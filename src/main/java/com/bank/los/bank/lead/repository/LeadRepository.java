package com.bank.los.bank.lead.repository;

import com.bank.los.bank.lead.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Lead entity, persisting into database table customer.leads.
 */
@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {

    @Override
    <S extends Lead> S save(S entity);

    @Override
    Optional<Lead> findById(String leadId);

    @Override
    List<Lead> findAll();

    List<Lead> findByLeadIdStartingWithOrderByLeadIdDesc(String prefix);

    boolean existsByPanNumber(String panNumber);

    boolean existsByAadhaarNumber(String aadhaarNumber);

    Optional<Lead> findByPanNumber(String panNumber);

    Optional<Lead> findByAadhaarNumber(String aadhaarNumber);
}
