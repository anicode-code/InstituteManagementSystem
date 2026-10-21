package com.institute.ims.config;

import com.institute.ims.model.Department;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.model.Role;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeData(
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            Department cse = createDepartmentIfNotExists(departmentRepository, "Computer Science", "CSE");
            createDepartmentIfNotExists(departmentRepository, "Information Technology", "IT");
            createDepartmentIfNotExists(departmentRepository, "Electronics & Communication", "ECE");
            createDepartmentIfNotExists(departmentRepository, "Mechanical Engineering", "ME");
            createDepartmentIfNotExists(departmentRepository, "Civil Engineering", "CIVIL");

            // Seed admin/staff users as VERIFIED (they are system-created)
            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "System Admin",
                    "ADMIN001",
                    "admin123",
                    "admin@ims.com",
                    Role.ADMIN,
                    cse,
                    VerificationStatus.VERIFIED
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "CSE HOD",
                    "HOD001",
                    "cse_hod123",
                    "cse_hod@ims.com",
                    Role.HOD,
                    cse,
                    VerificationStatus.VERIFIED
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "Faculty 1",
                    "FAC001",
                    "faculty123",
                    "faculty@ims.com",
                    Role.FACULTY,
                    cse,
                    VerificationStatus.VERIFIED
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "Demo Student",
                    "2026CS001",
                    "demo_student123",
                    "demo_student@ims.com",
                    Role.STUDENT,
                    cse,
                    VerificationStatus.VERIFIED
            );
        };
    }

    private Department createDepartmentIfNotExists(
            DepartmentRepository repository,
            String name,
            String code) {

        return repository.findAll().stream()
                .filter(d -> code.equalsIgnoreCase(d.getCode()))
                .findFirst()
                .orElseGet(() -> repository.save(new Department(name, code)));
    }

    private void createUserIfNotExists(
            UserRepository repository,
            PasswordEncoder encoder,
            String name,
            String rollNumber,
            String password,
            String email,
            Role role,
            Department department,
            VerificationStatus status) {

        if (repository.existsByRollNumber(rollNumber)) {
            return;
        }

        User user = new User();

        user.setName(name);
        user.setRollNumber(rollNumber);
        user.setPassword(
                encoder.encode(password)
        );
        user.setEmail(email);
        user.setRole(role);
        user.setDepartment(department);
        user.setStatus(status);

        repository.save(user);
    }
}