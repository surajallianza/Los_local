package com.bank.los.bank.master.repository;

import com.bank.los.bank.master.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByCode(String code);
    Optional<Branch> findByName(String name);
    boolean existsByCode(String code);
}
