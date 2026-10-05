package com.example.demo.lead.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Employee entity representing internal staff/users who can be assigned leads.
 * Table: employees
 */
@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "employee_id", nullable = false, unique = true)
    @JsonProperty("employeeId")
    private String employeeId;

    @Column(name = "name", nullable = false)
    @JsonProperty("name")
    private String name;

    @Column(name = "role", nullable = false)
    @JsonProperty("role")
    private String role; // "MAKER", "CHECKER", "ADMIN"

    @Column(name = "status", nullable = false)
    @JsonProperty("status")
    private String status; // "ACTIVE", "INACTIVE"

    public Employee() {
    }

    public Employee(String employeeId, String name, String role, String status) {
        this.employeeId = employeeId;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "employeeId='" + employeeId + '\'' +
                ", name='" + name + '\'' +
                ", role='" + role + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
