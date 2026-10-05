package com.example.demo.lead.service;

import com.example.demo.lead.exception.ResourceNotFoundException;
import com.example.demo.lead.exception.ValidationException;
import com.example.demo.lead.model.Employee;
import com.example.demo.lead.repository.EmployeeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service managing internal employee directory and role/status validations for Lead assignments.
 */
@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    /**
     * Seeds initial standard employees on startup if table is empty.
     */
    @PostConstruct
    @Transactional
    public void initDefaultEmployees() {
        if (employeeRepository.count() == 0) {
            employeeRepository.save(new Employee("EMP101", "Admin User", "ADMIN", "ACTIVE"));
            employeeRepository.save(new Employee("EMP102", "Rahul Verma", "MAKER", "ACTIVE"));
            employeeRepository.save(new Employee("EMP103", "Sita Sharma", "MAKER", "INACTIVE"));
            employeeRepository.save(new Employee("EMP104", "Vikram Malhotra", "CHECKER", "ACTIVE"));
            employeeRepository.save(new Employee("EMP105", "Pooja Mehta", "MAKER", "ACTIVE"));
        }
    }

    public Optional<Employee> findById(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findById(employeeId.trim());
    }

    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }

    public List<Employee> getActiveMakers() {
        return employeeRepository.findByRoleIgnoreCaseAndStatusIgnoreCase("MAKER", "ACTIVE");
    }

    /**
     * Validates that employee exists (404), is ACTIVE (400), and has MAKER role (400).
     *
     * @param employeeId Employee identifier (e.g. EMP102)
     * @return validated active Maker Employee
     */
    public Employee validateAndGetMakerEmployee(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            throw new ValidationException("Employee ID is required");
        }

        String trimmedId = employeeId.trim();
        Employee employee = employeeRepository.findById(trimmedId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with ID '" + trimmedId + "' not found"));

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new ValidationException("Employee '" + trimmedId + "' is inactive and cannot be assigned leads");
        }

        if (!"MAKER".equalsIgnoreCase(employee.getRole())) {
            throw new ValidationException("Employee '" + trimmedId + "' does not have MAKER role (Current role: " + employee.getRole() + ")");
        }

        return employee;
    }

    /**
     * Validates that assigning employee exists (404), is ACTIVE (400), and has ADMIN role (400).
     *
     * @param adminEmployeeId Admin Employee identifier (e.g. EMP101)
     * @return validated active Admin Employee
     */
    public Employee validateAndGetAdminEmployee(String adminEmployeeId) {
        if (adminEmployeeId == null || adminEmployeeId.trim().isEmpty()) {
            throw new ValidationException("Assigning Admin Employee ID is required");
        }

        String trimmedId = adminEmployeeId.trim();
        Employee employee = employeeRepository.findById(trimmedId)
                .orElseThrow(() -> new ResourceNotFoundException("Assigning Admin employee with ID '" + trimmedId + "' not found"));

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new ValidationException("Assigning Admin employee '" + trimmedId + "' is inactive");
        }

        if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
            throw new ValidationException("Only ADMIN users can assign leads to Makers (Employee '" + trimmedId + "' has role: " + employee.getRole() + ")");
        }

        return employee;
    }
}
