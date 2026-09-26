package com.institute.ims.auth;

import com.institute.ims.auth.dto.LoginRequest;
import com.institute.ims.auth.dto.LoginResponse;
import com.institute.ims.auth.dto.RegisterRequest;
import com.institute.ims.auth.dto.RegisterResponse;
import com.institute.ims.department.Department;
import com.institute.ims.department.DepartmentRepository;
import com.institute.ims.security.JwtService;
import com.institute.ims.user.User;
import com.institute.ims.user.UserRepository;
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

        if (userRepository.existsByUsername(
                request.getUsername())) {

            throw new RuntimeException(
                    "Username already exists");
        }

        if (userRepository.existsByEmail(
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
        user.setUsername(request.getUsername());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setEmail(request.getEmail());
        user.setRole(request.getRole());
        user.setDepartment(department);

        User savedUser =
                userRepository.save(user);

        Long departmentId =
                savedUser.getDepartment() != null
                        ? savedUser.getDepartment().getId()
                        : null;

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name(),
                departmentId
        );
    }

    public LoginResponse login(
            LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        User user =
                userRepository.findByUsername(
                        request.getUsername()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "User not found"));

        UserDetails userDetails =
                userDetailsService.loadUserByUsername(
                        request.getUsername()
                );

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponse(
                token,
                user.getId(),
                user.getName(),
                user.getUsername(),
                user.getRole().name()
        );
    }
}