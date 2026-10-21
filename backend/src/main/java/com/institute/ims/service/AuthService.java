package com.institute.ims.service;

import com.institute.ims.dto.LoginRequest;
import com.institute.ims.dto.LoginResponse;
import com.institute.ims.dto.RegisterRequest;
import com.institute.ims.dto.RegisterResponse;
import com.institute.ims.model.Department;
import com.institute.ims.model.User;
import com.institute.ims.model.VerificationStatus;
import com.institute.ims.repository.DepartmentRepository;
import com.institute.ims.repository.UserRepository;
import com.institute.ims.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
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

        if (userRepository.existsByRollNumber(
                request.getRollNumber())) {

            throw new RuntimeException(
                    "A user with this roll number already exists.");
        }

        if (request.getEmail() != null && !request.getEmail().isBlank() &&
                userRepository.existsByEmail(
                        request.getEmail())) {

            throw new RuntimeException(
                    "Email already exists");
        }

        Department department = null;

        if (request.getDepartmentId() != null) {

            department =
                    departmentRepository.findById(
                            request.getDepartmentId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Department not found"));
        }

        User user = new User();

        user.setName(request.getName());
        user.setRollNumber(request.getRollNumber());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setDepartment(department);
        // New self-registered users start as PENDING
        user.setStatus(VerificationStatus.PENDING);

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

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getRollNumber(),
                user.getRole().name(),
                user.getStatus().name()
        );
    }
}
