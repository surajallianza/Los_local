package com.example.demo.lead.repository;

import com.example.demo.lead.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for Employee entity, persisting into SQLite database table employees.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    List<Employee> findByRoleIgnoreCaseAndStatusIgnoreCase(String role, String status);
}
