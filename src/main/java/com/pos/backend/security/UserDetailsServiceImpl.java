package com.pos.backend.security;

import com.pos.backend.model.Employee;
import com.pos.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = employeeRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + username));

        if (!employee.isActive()) {
            throw new UsernameNotFoundException("Employee account is inactive: " + username);
        }

        return User.builder()
                .username(employee.getUsername())
                .password(employee.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())))
                .build();
    }
}
