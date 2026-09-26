package com.institute.ims.config;

import com.institute.ims.department.Department;
import com.institute.ims.department.DepartmentRepository;
import com.institute.ims.user.Role;
import com.institute.ims.user.User;
import com.institute.ims.user.UserRepository;
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

            Department cse =
                    departmentRepository
                            .findAll()
                            .stream()
                            .findFirst()
                            .orElseGet(() ->
                                    departmentRepository.save(
                                            new Department(
                                                    "Computer Science",
                                                    "CSE"
                                            )
                                    )
                            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "System Admin",
                    "admin",
                    "admin123",
                    "admin@ims.com",
                    Role.ADMIN,
                    cse
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "CSE HOD",
                    "cse_hod",
                    "cse_hod123",
                    "cse_hod@ims.com",
                    Role.HOD,
                    cse
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "Faculty 1",
                    "faculty1",
                    "faculty123",
                    "faculty@ims.com",
                    Role.FACULTY,
                    cse
            );

            createUserIfNotExists(
                    userRepository,
                    passwordEncoder,
                    "Demo student",
                    "demo_student",
                    "demo_student123",
                    "demo_student@ims.com",
                    Role.STUDENT,
                    cse
            );
        };
    }

    private void createUserIfNotExists(
            UserRepository repository,
            PasswordEncoder encoder,
            String name,
            String username,
            String password,
            String email,
            Role role,
            Department department) {

        if (repository.existsByUsername(username)) {
            return;
        }

        User user = new User();

        user.setName(name);
        user.setUsername(username);
        user.setPassword(
                encoder.encode(password)
        );
        user.setEmail(email);
        user.setRole(role);
        user.setDepartment(department);

        repository.save(user);
    }
}