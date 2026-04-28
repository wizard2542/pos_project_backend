package com.pos.backend.controller;

import com.pos.backend.exception.DuplicateResourceException;
import com.pos.backend.exception.ResourceNotFoundException;
import com.pos.backend.model.Employee;
import com.pos.backend.repository.EmployeeRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<Employee> getEmployeeByUsername(@PathVariable String username) {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "username", username));
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<Employee>> getEmployeesByRole(@PathVariable Employee.Role role) {
        return ResponseEntity.ok(employeeRepository.findByRole(role));
    }

    @PostMapping
    public ResponseEntity<Employee> createEmployee(@Valid @RequestBody Employee employee) {
        if (employeeRepository.existsByUsername(employee.getUsername())) {
            throw new DuplicateResourceException("Employee", "username", employee.getUsername());
        }
        if (employee.getEmail() != null && employeeRepository.existsByEmail(employee.getEmail())) {
            throw new DuplicateResourceException("Employee", "email", employee.getEmail());
        }
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        Employee saved = employeeRepository.save(employee);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> updateEmployee(@PathVariable Long id,
                                                   @Valid @RequestBody Employee employeeDetails) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));

        if (!employee.getUsername().equals(employeeDetails.getUsername())
                && employeeRepository.existsByUsername(employeeDetails.getUsername())) {
            throw new DuplicateResourceException("Employee", "username", employeeDetails.getUsername());
        }

        employee.setName(employeeDetails.getName());
        employee.setUsername(employeeDetails.getUsername());
        employee.setPhone(employeeDetails.getPhone());
        employee.setEmail(employeeDetails.getEmail());
        employee.setRole(employeeDetails.getRole());
        employee.setActive(employeeDetails.isActive());

        if (employeeDetails.getPassword() != null && !employeeDetails.getPassword().isBlank()) {
            employee.setPassword(passwordEncoder.encode(employeeDetails.getPassword()));
        }

        return ResponseEntity.ok(employeeRepository.save(employee));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", "id", id));
        employeeRepository.delete(employee);
        return ResponseEntity.noContent().build();
    }
}
