package com.institute.ims.service;

import com.institute.ims.dto.LoginRequest;
import com.institute.ims.dto.LoginResponse;
import com.institute.ims.dto.RegisterRequest;
import com.institute.ims.dto.RegisterResponse;
import com.institute.ims.model.*;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import com.institute.ims.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public RegisterResponse register(
            RegisterRequest request) {

        // ─── Role restriction: only STUDENT and FACULTY can self-register ───
        if (request.getRole() != Role.STUDENT && request.getRole() != Role.FACULTY) {
            throw new RuntimeException(
                    "Only Student and Faculty roles can be registered.");
        }

        if (userRepository.existsByRollNumber(
                request.getRollNumber())) {

            throw new RuntimeException(
                    "A user with this roll number already exists.");
        }

        String email = request.getEmail() == null
                ? null
                : request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (email != null && !email.isBlank() && userRepository.existsByEmailIgnoreCase(email)) {

            throw new RuntimeException(
                    "Email already exists");
        }

        User user = new User();

        user.setName(request.getName());
        user.setRollNumber(request.getRollNumber());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setEmail(email);
        user.setRole(request.getRole());
        // New self-registered users start as PENDING
        user.setStatus(VerificationStatus.PENDING);

        // ─── STUDENT registration ───
        if (request.getRole() == Role.STUDENT) {

            // Department is mandatory (exactly one)
            if (request.getDepartmentId() == null) {
                throw new RuntimeException(
                        "Department is required for students.");
            }

            Department department =
                    departmentRepository.findById(
                            request.getDepartmentId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Department not found"));

            user.setDepartment(department);

        }

        // ─── FACULTY registration ───
        if (request.getRole() == Role.FACULTY) {

            // Resolve department IDs for multi-department selection
            List<Long> deptIds = request.getDepartmentIds();

            // Fallback: if departmentIds is empty, try single departmentId
            if ((deptIds == null || deptIds.isEmpty()) && request.getDepartmentId() != null) {
                deptIds = List.of(request.getDepartmentId());
            }

            if (deptIds == null || deptIds.isEmpty()) {
                throw new RuntimeException(
                        "At least one department is required for faculty.");
            }

            Set<Department> departments = new HashSet<>();
            Department primaryDept = null;

            for (Long deptId : deptIds) {
                Department dept = departmentRepository.findById(deptId)
                        .orElseThrow(() -> new RuntimeException(
                                "Department not found: ID " + deptId));
                departments.add(dept);
                if (primaryDept == null) {
                    primaryDept = dept;
                }
            }

            user.setDepartments(departments);
            // Set the first department as the primary department for backward compatibility
            user.setDepartment(primaryDept);
        }

        User savedUser =
                userRepository.save(user);

        Long departmentId =
                savedUser.getDepartment() != null
                        ? savedUser.getDepartment().getId()
                        : null;

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getRollNumber(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                savedUser.getStatus().name(),
                departmentId
        );
    }

    public LoginResponse login(
            LoginRequest request) {

        User user =
                userRepository.findByRollNumber(
                        request.getRollNumber()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "Invalid roll number or password"));

        if (user.getStatus() != VerificationStatus.VERIFIED) {
            throw new RuntimeException(
                    "Your account has not been verified by an admin yet. Please wait for verification before logging in.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getRollNumber(),
                        request.getPassword()
                )
        );

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.getRollNumber()
                );

        String token =
                jwtService.generateToken(userDetails);

        Department dept = user.getDepartment();
        if (dept == null && user.getRole() == Role.HOD) {
            List<Department> hodDepts = departmentRepository.findByHodId(user.getId());
            if (!hodDepts.isEmpty()) {
                dept = hodDepts.get(0);
            }
        }

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getRollNumber(),
                user.getRole().name(),
                user.getStatus().name(),
                dept
        );
    }
}
