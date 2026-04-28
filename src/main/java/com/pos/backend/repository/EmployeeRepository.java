package com.pos.backend.repository;

import com.pos.backend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<Employee> findByRole(Employee.Role role);

    List<Employee> findByActive(boolean active);
}
